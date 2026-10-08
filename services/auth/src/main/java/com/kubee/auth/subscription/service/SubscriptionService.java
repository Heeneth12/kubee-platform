package com.kubee.auth.subscription.service;

import com.kubee.auth.subscription.dto.PlanRequestDto;
import com.kubee.auth.subscription.dto.SubscriptionDto;
import com.kubee.auth.subscription.dto.SubscriptionPlanDto;
import com.kubee.auth.subscription.dto.SubscriptionPlanSummaryDto;
import com.kubee.auth.subscription.entity.Subscription;
import com.kubee.auth.subscription.entity.SubscriptionPlan;
import com.kubee.auth.subscription.entity.SubscriptionStatus;
import com.kubee.auth.subscription.repository.SubscriptionPlanRepository;
import com.kubee.auth.subscription.repository.SubscriptionRepository;
import com.kubee.auth.tenant.entity.Tenant;
import com.kubee.auth.tenant.repository.TenantRepository;
import com.kubee.auth.common.service.AccessService;
import com.kubee.security.UserContextUtil;
import com.kubee.common.CommonResponse;
import com.kubee.common.Status;
import com.kubee.common.CommonException;
import com.kubee.auth.common.entity.Application;
import com.kubee.auth.common.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final TenantRepository tenantRepository;
    private final ApplicationRepository applicationRepository;
    private final AccessService accessService;

    @Transactional
    public CommonResponse subscribeTenant(Long tenantId, Long planId) throws CommonException {
        log.info("Assigning plan {} to tenant {}", planId, tenantId);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new CommonException("Tenant not found with id: " + tenantId, HttpStatus.BAD_REQUEST));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new CommonException("Plan not found with id: " + planId, HttpStatus.BAD_REQUEST));

        // Tenants can only request a plan change; it goes live when the platform activates it (after payment).
        // Without this a tenant could switch itself to any paid plan, or keep restarting the free trial.
        if (!UserContextUtil.isPlatformUser()) {
            return requestPlanChange(tenant, plan);
        }

        // Activating replaces any open request
        subscriptionRepository.findByTenantIdAndStatus(tenantId, SubscriptionStatus.PENDING_PAYMENT)
                .ifPresent(pending -> {
                    pending.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(pending);
                });

        subscriptionRepository.findPrimaryActiveSubscription(tenantId, SubscriptionStatus.ACTIVE)
                .ifPresent(oldSub -> {
                    oldSub.setIsPrimary(false);
                    oldSub.setStatus(SubscriptionStatus.EXPIRED);
                    // Flush now: the new ACTIVE row below would otherwise be inserted first
                    // and break the one-active-subscription-per-tenant index
                    subscriptionRepository.saveAndFlush(oldSub);
                });

        // Create new subscription
        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .tenant(tenant)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .isPrimary(true)
                .startDate(now)
                .endDate(now.plusDays(plan.getDurationDays()))
                .autoRenew(true)
                .build();

        subscriptionRepository.save(subscription);

        return CommonResponse.builder()
                .id(tenantId.toString())
                .status(Status.SUCCESS)
                .message("Tenant subscribed successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getTenantSubscription(Long tenantId) throws CommonException {
        if (!tenantRepository.existsById(tenantId)) {
            throw new CommonException("Tenant not found with id: " + tenantId, HttpStatus.BAD_REQUEST);
        }

        Subscription subscription = subscriptionRepository
                .findPrimaryActiveSubscription(tenantId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new CommonException("No active subscription found for tenant.", HttpStatus.BAD_REQUEST));

        return mapToDto(subscription);
    }

    private CommonResponse requestPlanChange(Tenant tenant, SubscriptionPlan plan) throws CommonException {
        accessService.requireSettingsEditor();
        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new CommonException("This plan is not available", HttpStatus.BAD_REQUEST);
        }
        if (!tenantRepository.existsTenantApplication(tenant.getId(), plan.getApplication().getId())) {
            throw new CommonException("This plan is for an application your business doesn't use", HttpStatus.BAD_REQUEST);
        }
        if (plan.getPrice() == null || plan.getPrice().signum() == 0) {
            throw new CommonException("Free plans can't be restarted. Choose a paid plan.", HttpStatus.BAD_REQUEST);
        }

        // One open request at a time: a new one replaces the previous
        subscriptionRepository.findByTenantIdAndStatus(tenant.getId(), SubscriptionStatus.PENDING_PAYMENT)
                .ifPresent(previous -> {
                    previous.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(previous);
                });

        LocalDateTime now = LocalDateTime.now();
        Subscription request = subscriptionRepository.save(Subscription.builder()
                .tenant(tenant)
                .plan(plan)
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .isPrimary(false)
                .startDate(now)
                .endDate(now.plusDays(plan.getDurationDays()))
                .autoRenew(true)
                .build());

        return CommonResponse.builder()
                .id(request.getId().toString())
                .status(Status.SUCCESS)
                .message("Plan change requested. It will be activated once payment is confirmed.")
                .build();
    }

    /** Every tenant's open plan-change request, oldest first. Platform only. */
    @Transactional(readOnly = true)
    public List<PlanRequestDto> getOpenPlanRequests() throws CommonException {
        UserContextUtil.requirePlatformAccess();
        return subscriptionRepository.findByStatusOrderByCreatedAtAsc(SubscriptionStatus.PENDING_PAYMENT).stream()
                .map(request -> {
                    Tenant tenant = request.getTenant();
                    Subscription current = subscriptionRepository
                            .findPrimaryActiveSubscription(tenant.getId(), SubscriptionStatus.ACTIVE)
                            .orElse(null);
                    return PlanRequestDto.builder()
                            .subscriptionId(request.getId())
                            .tenantId(tenant.getId())
                            .tenantName(tenant.getTenantName())
                            .tenantCode(tenant.getTenantCode())
                            .requestedPlan(mapToDto(request.getPlan()))
                            .currentPlan(current != null ? mapToDto(current.getPlan()) : null)
                            .currentPlanEndsAt(current != null ? current.getEndDate() : null)
                            .requestedAt(request.getCreatedAt())
                            .build();
                })
                .toList();
    }

    /** The tenant's open plan-change request, if any. */
    @Transactional(readOnly = true)
    public SubscriptionDto getPendingRequest(Long tenantId) {
        return subscriptionRepository.findByTenantIdAndStatus(tenantId, SubscriptionStatus.PENDING_PAYMENT)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Transactional
    public CommonResponse cancelSubscription(Long subscriptionId) throws CommonException {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CommonException("Subscription not found", HttpStatus.BAD_REQUEST));
        UserContextUtil.requireTenantAccess(subscription.getTenant().getId());

        // Tenants withdraw a pending request, or stop renewal of the active plan (it runs until its end date).
        // Ending an active plan immediately would lock everyone in the business out at once.
        if (!UserContextUtil.isPlatformUser()) {
            accessService.requireSettingsEditor();
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                subscription.setAutoRenew(false);
                subscriptionRepository.save(subscription);
                return CommonResponse.builder()
                        .id(subscription.getId().toString())
                        .status(Status.SUCCESS)
                        .message("Auto-renew turned off. Your plan stays active until " + subscription.getEndDate().toLocalDate())
                        .build();
            }
            if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
                throw new CommonException("This subscription can't be cancelled", HttpStatus.BAD_REQUEST);
            }
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        subscription = subscriptionRepository.save(subscription);

        return CommonResponse.builder()
                .id(subscription.getId().toString())
                .status(Status.SUCCESS)
                .message("Subscription cancelled successfully")
                .build();
    }


    @Transactional
    public CommonResponse createPlan(SubscriptionPlanDto dto) throws CommonException {
        UserContextUtil.requirePlatformAccess();

        log.info("Creating new subscription plan: {}", dto.getName());

        Application application = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new CommonException("Application not found with id: " + dto.getApplicationId(), HttpStatus.BAD_REQUEST));

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .application(application)
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .price(dto.getPrice())
                .durationDays(dto.getDurationDays())
                .maxUsers(dto.getMaxUsers())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        plan = planRepository.save(plan);
        return CommonResponse
                .builder()
                .id(plan.getId().toString())
                .status(Status.SUCCESS)
                .message("Subscription plan created successfully")
                .build();
    }

    @Transactional
    public CommonResponse editPlan(Long planId, SubscriptionPlanDto dto) throws CommonException {
        UserContextUtil.requirePlatformAccess();
        log.info("Editing subscription plan: {}", planId);

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new CommonException("Subscription plan not found with id: " + planId, HttpStatus.BAD_REQUEST));

        Application application = applicationRepository.findById(dto.getApplicationId())
                .orElseThrow(() -> new CommonException("Application not found with id: " + dto.getApplicationId(), HttpStatus.BAD_REQUEST));

        plan.setApplication(application);
        plan.setName(dto.getName());
        plan.setDescription(dto.getDescription());
        plan.setType(dto.getType());
        plan.setPrice(dto.getPrice());
        plan.setDurationDays(dto.getDurationDays());
        plan.setMaxUsers(dto.getMaxUsers());
        
        if (dto.getIsActive() != null) {
            plan.setIsActive(dto.getIsActive());
        }

        planRepository.save(plan);

        return CommonResponse.builder()
                .id(plan.getId().toString())
                .status(Status.SUCCESS)
                .message("Subscription plan updated successfully")
                .build();
    }

    @Transactional
    public CommonResponse deletePlan(Long planId) throws CommonException {
        UserContextUtil.requirePlatformAccess();
        log.info("Deleting subscription plan: {}", planId);

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new CommonException("Subscription plan not found with id: " + planId, HttpStatus.BAD_REQUEST));

        planRepository.delete(plan);

        return CommonResponse.builder()
                .id(planId.toString())
                .status(Status.SUCCESS)
                .message("Subscription plan deleted successfully")
                .build();
    }

    @Transactional
    public CommonResponse disablePlan(Long planId) throws CommonException {
        UserContextUtil.requirePlatformAccess();
        log.info("Disabling subscription plan: {}", planId);

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new CommonException("Subscription plan not found with id: " + planId, HttpStatus.BAD_REQUEST));

        plan.setIsActive(false);
        planRepository.save(plan);

        return CommonResponse.builder()
                .id(planId.toString())
                .status(Status.SUCCESS)
                .message("Subscription plan disabled successfully")
                .build();
    }

    @Transactional
    public CommonResponse updatePlanStatus(Long planId, Boolean st) throws CommonException {
        UserContextUtil.requirePlatformAccess();
        log.info("Updating subscription plan status for id: {} to active: {}", planId, st);

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new CommonException("Subscription plan not found with id: " + planId, HttpStatus.BAD_REQUEST));

        // Update the entity's field with the request parameter value
        plan.setIsActive(st);
        planRepository.save(plan);

        String actionMessage = st ? "activated" : "disabled";

        return CommonResponse.builder()
                .id(planId.toString())
                .status(Status.SUCCESS)
                .message("Subscription plan " + actionMessage + " successfully")
                .build();
    }

    public List<SubscriptionPlanDto> getActivePlans() {
        return planRepository.findByIsActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionPlanSummaryDto> getPlansPage(int page, int size, Boolean isActive) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SubscriptionPlan> plans = isActive != null
                ? planRepository.findByIsActive(isActive, pageable)
                : planRepository.findAll(pageable);
        return plans.map(this::mapToSummaryDto);
    }

    public SubscriptionPlanDto getPlanById(Long id) throws CommonException {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new CommonException("Subscription plan not found with id: " + id, HttpStatus.BAD_REQUEST));
        return mapToDto(plan);
    }

    /**
     * Checks if the given tenant has an active and unexpired subscription.
     */
    public Boolean hasValidSubscription(Long tenantId) {
        return subscriptionRepository.findValidSubscription(tenantId, SubscriptionStatus.ACTIVE, LocalDateTime.now()).isPresent();
    }

    private SubscriptionPlanSummaryDto mapToSummaryDto(SubscriptionPlan plan) {
        SubscriptionPlanSummaryDto.MiniApplicationDto appDto = SubscriptionPlanSummaryDto.MiniApplicationDto.builder()
                .id(plan.getApplication().getId())
                .appName(plan.getApplication().getAppName())
                .appKey(plan.getApplication().getAppKey())
                .build();
        return SubscriptionPlanSummaryDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .type(plan.getType())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .maxUsers(plan.getMaxUsers())
                .isActive(plan.getIsActive())
                .application(appDto)
                .build();
    }

    // Helper mapper
    private SubscriptionPlanDto mapToDto(SubscriptionPlan plan) {
        return SubscriptionPlanDto.builder()
                .id(plan.getId())
                .applicationId(plan.getApplication().getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .type(plan.getType())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .maxUsers(plan.getMaxUsers())
                .isActive(plan.getIsActive())
                .build();
    }

    // Helper mapper
    private SubscriptionDto mapToDto(Subscription subscription) {
        long daysRemaining = 0;
        if (subscription.getEndDate() != null && LocalDateTime.now().isBefore(subscription.getEndDate())) {
            daysRemaining = Duration.between(LocalDateTime.now(), subscription.getEndDate()).toDays();
        }

        SubscriptionPlan plan = subscription.getPlan();
        SubscriptionPlanDto planDto = SubscriptionPlanDto.builder()
                .id(plan.getId())
                .applicationId(plan.getApplication().getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .type(plan.getType())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .maxUsers(plan.getMaxUsers())
                .build();

        return SubscriptionDto.builder()
                .id(subscription.getId())
                .plan(planDto)
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .autoRenew(subscription.getAutoRenew())
                .createdAt(subscription.getCreatedAt())
                .isValid(subscription.isValid())
                .daysRemaining(daysRemaining)
                .build();
    }
}