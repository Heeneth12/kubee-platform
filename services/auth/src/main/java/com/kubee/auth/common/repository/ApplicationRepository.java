package com.kubee.auth.common.repository;

import com.kubee.auth.common.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ApplicationRepository  extends JpaRepository<Application, Long> {

    Optional<Application> findByAppKey(String appKey);
}
