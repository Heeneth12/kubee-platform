package com.kubee.auth.support.service;


import com.kubee.auth.support.dto.CreateAppRequestDto;
import com.kubee.auth.support.dto.CreateMarketingRequestDto;
import com.kubee.auth.support.dto.UpdateRequestDto;
import com.kubee.auth.support.dto.UserRequestDto;
import com.kubee.auth.support.entity.SupportCategory;
import com.kubee.auth.support.entity.SupportPriority;
import com.kubee.auth.support.entity.SupportStatus;
import com.kubee.auth.support.entity.UserRequest;
import com.kubee.auth.support.repository.UserRequestRepository;
import com.kubee.auth.tenant.entity.Tenant;
import com.kubee.auth.tenant.repository.TenantRepository;
import com.kubee.auth.user.entity.User;
import com.kubee.auth.user.repository.UserRepository;
import com.kubee.auth.utils.EmailService;
import com.kubee.security.UserContextUtil;
import com.kubee.common.CommonResponse;
import com.kubee.common.Status;
import com.kubee.common.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRequestService {

    private final UserRequestRepository userRequestRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;


    @Transactional
    public CommonResponse createMarketingRequest(CreateMarketingRequestDto requestDto) throws CommonException {
        log.info("Creating new marketing request for email: {}", requestDto.getContactEmail());

        UserRequest userRequest = UserRequest.builder()
                .tenantUuid(null)
                .userUuid(null)
                .assignedUuid(null)
                .contactEmail(requestDto.getContactEmail())
                .contactName(requestDto.getContactName())
                .subject(requestDto.getSubject())
                .description(requestDto.getDescription())
                .sourceUrl(requestDto.getSourceUrl())
                .sourceName(requestDto.getSourceName())
                .category(SupportCategory.GENERAL_INQUIRY)
                .status(SupportStatus.NEW)
                .priority(SupportPriority.MEDIUM)
                .metadata(requestDto.getMetadata())
                .build();

        userRequest = userRequestRepository.save(userRequest);

        // Send Acknowledgement mail asynchronously
        emailService.sendSupportAcknowledgmentEmail(
                userRequest.getContactEmail(),
                userRequest.getContactName(),
                userRequest.getUserReqUuid(),
                userRequest.getSubject()
        );

        return CommonResponse.builder()
                .id(userRequest.getUserReqUuid())
                .status(Status.SUCCESS)
                .message("Marketing request created successfully")
                .build();
    }

    @Transactional
    public CommonResponse createRequest(CreateAppRequestDto requestDto) throws CommonException {

        log.info("Creating new user request from source: {}", requestDto.getSourceUrl());

        Long tenantId = UserContextUtil.getTenantIdOrThrow();
        Long userId = UserContextUtil.getUserIdOrThrow();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new CommonException("Tenant not found with id: " + tenantId, HttpStatus.BAD_REQUEST));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommonException("User not found with id: " + userId, HttpStatus.BAD_REQUEST));

        UserRequest userRequest = UserRequest.builder()
                .tenantUuid(tenant.getTenantUuid())
                .userUuid(user.getUserUuid())
                .contactEmail(user.getEmail())
                .contactName(user.getFullName())
                .subject(requestDto.getSubject())
                .description(requestDto.getDescription())
                .sourceUrl(requestDto.getSourceUrl())
                .sourceName(requestDto.getSourceName())
                .category(requestDto.getCategory())
                .status(SupportStatus.NEW)
                .priority(requestDto.getPriority())
                .metadata(requestDto.getMetadata())
                .build();

        userRequest = userRequestRepository.save(userRequest);

        return CommonResponse.builder()
                .id(userRequest.getUserReqUuid())
                .status(Status.SUCCESS)
                .message("User request created successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public UserRequestDto getRequestById(String userReqUuid) throws CommonException {
        UserRequest request = userRequestRepository.findByUserReqUuid(userReqUuid)
                .orElseThrow(() -> new CommonException("Request not found with UUID: " + userReqUuid, HttpStatus.NOT_FOUND));
        requireRequestAccess(request);
        return mapToDto(request);
    }

    @Transactional
    public CommonResponse updateRequest(String userReqUuid, UpdateRequestDto updateDto) throws CommonException {
        log.info("Updating user request: {}", userReqUuid);

        UserRequest existingRequest = userRequestRepository.findByUserReqUuid(userReqUuid)
                .orElseThrow(() -> new CommonException("Request not found with UUID: " + userReqUuid, HttpStatus.NOT_FOUND));
        requireRequestAccess(existingRequest);

        // 1. Update Status & Resolution Timestamp
        if (updateDto.getStatus() != null) {
            existingRequest.setStatus(updateDto.getStatus());

            // Using Enum equality is much safer than String "equalsIgnoreCase"
            if (updateDto.getStatus() == SupportStatus.RESOLVED || updateDto.getStatus() == SupportStatus.CLOSED) {
                existingRequest.setResolvedAt(LocalDateTime.now());
            } else {
                // If the ticket is moved back to IN_PROGRESS or NEW, clear the resolved timestamp
                existingRequest.setResolvedAt(null);
            }
        }

        // 2. Update Priority
        if (updateDto.getPriority() != null) {
            existingRequest.setPriority(updateDto.getPriority());
        }

        // 3. Update Category
        if (updateDto.getCategory() != null) {
            existingRequest.setCategory(updateDto.getCategory());
        }

        // 4. Update Assignee
        if (updateDto.getAssignedUuid() != null) {
            existingRequest.setAssignedUuid(updateDto.getAssignedUuid());
        }

        userRequestRepository.save(existingRequest);

        return CommonResponse.builder()
                .id(existingRequest.getUserReqUuid())
                .status(Status.SUCCESS)
                .message("User request updated successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public Page<UserRequestDto> getRequestsWithPagination(String tenantUuid, Pageable pageable) {
        if (!UserContextUtil.isPlatformUser()) {
            tenantUuid = UserContextUtil.getTenantUuid();
        }
        log.info("Fetching paginated requests for tenant id: {}", tenantUuid);
        Page<UserRequest> requestsPage;

        if (tenantUuid != null) {
            requestsPage = userRequestRepository.findByTenantUuid(tenantUuid, pageable);
        } else {
            requestsPage = userRequestRepository.findAll(pageable);
        }

        return requestsPage.map(this::mapToDto);
    }

    private UserRequestDto mapToDto(UserRequest entity) {
        return UserRequestDto.builder()
                .userReqUuid(entity.getUserReqUuid())
                .userUuid(entity.getUserUuid())
                .assignedUuid(entity.getAssignedUuid())
                .contactEmail(entity.getContactEmail())
                .contactName(entity.getContactName())
                .subject(entity.getSubject())
                .description(entity.getDescription())
                .sourceUrl(entity.getSourceUrl())
                .sourceName(entity.getSourceName())
                .category(entity.getCategory())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .resolvedAt(entity.getResolvedAt())
                .build();
    }

    private void requireRequestAccess(UserRequest request) throws CommonException {
        if (!UserContextUtil.isPlatformUser()
                && !Objects.equals(request.getTenantUuid(), UserContextUtil.getTenantUuid())) {
            throw new CommonException("Request not found", HttpStatus.NOT_FOUND);
        }
    }
}
