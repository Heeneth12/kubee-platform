package com.kubee.auth.subscription.controller;

import com.kubee.auth.subscription.dto.PlanRequestDto;
import com.kubee.auth.subscription.dto.SubscriptionDto;
import com.kubee.auth.subscription.dto.SubscriptionPlanDto;
import com.kubee.auth.subscription.dto.SubscriptionPlanSummaryDto;
import com.kubee.auth.subscription.service.SubscriptionService;
import org.springframework.data.domain.Page;
import com.kubee.security.UserContextUtil;
import com.kubee.common.CommonResponse;
import com.kubee.common.ResponseResource;
import com.kubee.common.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping(value = "/tenant/{tenantId}/plan/{planId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> subscribeTenant(
            @PathVariable Long tenantId,
            @PathVariable Long planId) throws CommonException {
        log.info("Entered subscribe tenant {} to plan {}", tenantId, planId);
        UserContextUtil.requireTenantAccess(tenantId);
        CommonResponse response = subscriptionService.subscribeTenant(tenantId, planId);
        return ResponseResource.success(HttpStatus.CREATED, response, "Tenant subscribed successfully");
    }

    @GetMapping(value = "/tenant/{tenantId}/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<SubscriptionDto> getCurrentSubscription(@PathVariable Long tenantId) throws CommonException {
        log.info("Entered get current subscription for tenant: {}", tenantId);
        UserContextUtil.requireTenantAccess(tenantId);
        SubscriptionDto response = subscriptionService.getTenantSubscription(tenantId);
        return ResponseResource.success(HttpStatus.OK, response, "Current subscription fetched successfully");
    }

    @GetMapping(value = "/requests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<PlanRequestDto>> getOpenPlanRequests() throws CommonException {
        log.info("Entered get open plan requests");
        List<PlanRequestDto> response = subscriptionService.getOpenPlanRequests();
        return ResponseResource.success(HttpStatus.OK, response, "Plan requests fetched successfully");
    }

    @GetMapping(value = "/tenant/{tenantId}/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<SubscriptionDto> getPendingRequest(@PathVariable Long tenantId) throws CommonException {
        UserContextUtil.requireTenantAccess(tenantId);
        SubscriptionDto response = subscriptionService.getPendingRequest(tenantId);
        return ResponseResource.success(HttpStatus.OK, response, "Pending plan request fetched successfully");
    }

    @PutMapping(value = "/{subscriptionId}/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> cancelSubscription(@PathVariable Long subscriptionId) throws CommonException {
        log.info("Entered cancel subscription for id: {}", subscriptionId);
        CommonResponse response = subscriptionService.cancelSubscription(subscriptionId);
        return ResponseResource.success(HttpStatus.OK, response, "Subscription cancelled successfully");
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createPlan(@RequestBody SubscriptionPlanDto planDto) throws CommonException {
        log.info("Entered create subscription plan: {}", planDto);
        CommonResponse response = subscriptionService.createPlan(planDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Subscription plan created successfully");
    }

    @PutMapping(value = "/plan/{planId}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> editPlan(@PathVariable Long planId, @RequestBody SubscriptionPlanDto planDto) throws CommonException {
        log.info("Entered edit subscription plan for id: {}", planId);
        CommonResponse response = subscriptionService.editPlan(planId, planDto);
        return ResponseResource.success(HttpStatus.OK, response, "Subscription plan updated successfully");
    }

    @DeleteMapping(value = "/plan/{planId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> deletePlan(@PathVariable Long planId) throws CommonException {
        log.info("Entered delete subscription plan for id: {}", planId);
        CommonResponse response = subscriptionService.deletePlan(planId);
        return ResponseResource.success(HttpStatus.OK, response, "Subscription plan deleted successfully");
    }

    @PatchMapping(value = "/plan/{planId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> updatePlanStatus(
            @PathVariable Long planId,
            @RequestParam Boolean st) throws CommonException {
        log.info("Entered update subscription plan status for id: {} to active: {}", planId, st);
        CommonResponse response = subscriptionService.updatePlanStatus(planId, st);
        String message = st ? "Subscription plan activated successfully" : "Subscription plan disabled successfully";
        return ResponseResource.success(HttpStatus.OK, response, message);
    }

    @GetMapping(value = "/plan/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<SubscriptionPlanSummaryDto>> getAllPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isActive) {
        log.info("Entered get all subscription plans - page: {}, size: {}, isActive: {}", page, size, isActive);
        Page<SubscriptionPlanSummaryDto> response = subscriptionService.getPlansPage(page, size, isActive);
        return ResponseResource.success(HttpStatus.OK, response, "Subscription plans fetched successfully");
    }

    @GetMapping(value = "/active", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<SubscriptionPlanDto>> getActivePlans() throws CommonException {
        log.info("Entered get all active subscription plans");
        List<SubscriptionPlanDto> response = subscriptionService.getActivePlans();
        return ResponseResource.success(HttpStatus.OK, response, "Active subscription plans fetched successfully");
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<SubscriptionPlanDto> getPlanById(@PathVariable Long id) throws CommonException {
        log.info("Entered get subscription plan by id: {}", id);
        SubscriptionPlanDto response = subscriptionService.getPlanById(id);
        return ResponseResource.success(HttpStatus.OK, response, "Subscription plan fetched successfully");
    }
}