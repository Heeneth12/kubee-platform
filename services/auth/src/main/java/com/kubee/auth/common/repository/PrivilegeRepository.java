package com.kubee.auth.common.repository;

import com.kubee.auth.common.entity.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {
    Optional<Privilege> findByModule_IdAndId(Long moduleId, Long privilegeId);

    List<Privilege> findByModuleId(Long moduleId);

    Optional<Privilege> findByPrivilegeKeyAndModuleId(String privilegeKey, Long moduleId);
}
