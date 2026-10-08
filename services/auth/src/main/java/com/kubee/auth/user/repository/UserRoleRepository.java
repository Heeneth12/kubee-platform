package com.kubee.auth.user.repository;

import com.kubee.auth.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    boolean existsByRole_Id(Long roleId);
}
