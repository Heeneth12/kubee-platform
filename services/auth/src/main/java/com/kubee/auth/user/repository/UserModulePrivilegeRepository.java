package com.kubee.auth.user.repository;

import com.kubee.auth.user.entity.UserModulePrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserModulePrivilegeRepository extends JpaRepository<UserModulePrivilege, Long> {

}
