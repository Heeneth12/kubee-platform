package com.kubee.auth.subscription.repository;

import com.kubee.auth.subscription.entity.SubscriptionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByName(String name);

    /** The app's free plan, used as the trial a new tenant starts on. */
    Optional<SubscriptionPlan> findFirstByApplication_IdAndPriceAndIsActiveTrueOrderByIdAsc(Long applicationId, BigDecimal price);
    List<SubscriptionPlan> findByIsActiveTrue();
    Page<SubscriptionPlan> findByIsActive(Boolean isActive, Pageable pageable);
}
