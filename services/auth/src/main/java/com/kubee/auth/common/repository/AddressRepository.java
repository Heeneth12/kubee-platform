package com.kubee.auth.common.repository;

import com.kubee.auth.common.entity.Address;
import com.kubee.auth.common.entity.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);
}
