package com.kubee.inventory.contacts.repository;

import com.kubee.inventory.contacts.entiry.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
}
