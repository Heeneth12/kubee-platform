package com.kubee.auth.support.repository;


import com.kubee.auth.support.entity.UserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRequestRepository extends JpaRepository<UserRequest, Long> {
    Optional<UserRequest> findByUserReqUuid(String userReqUuid);

    Page<UserRequest> findByTenantUuid(String tenantUuid, Pageable pageable);
}
