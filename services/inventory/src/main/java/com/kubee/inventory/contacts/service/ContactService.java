package com.kubee.inventory.contacts.service;

import com.kubee.inventory.contacts.dto.ContactDto;
import com.kubee.inventory.contacts.dto.ContactFilter;
import com.kubee.inventory.contacts.dto.NetworkRequestDto;
import com.kubee.inventory.utils.common.dto.TenantDto;
import com.kubee.inventory.contacts.entiry.NetworkRequest;
import com.kubee.inventory.contacts.entiry.NetworkStatus;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface  ContactService {
    CommonResponse createContact(ContactDto contact);
    CommonResponse updateContact(Long id, ContactDto contactDto);
    ContactDto getContact(Long id);
    Page<ContactDto> getAllContacts(ContactFilter contactFilter, Integer page, Integer size);
    CommonResponse toggleStatus(Long id, Boolean active);
    List<ContactDto> searchContact(ContactFilter contactFilter) throws CommonException;
    CommonResponse<?> updateNetworkStatus(Long id, NetworkStatus status) throws CommonException;
    List<TenantDto> getMyNetwork() throws CommonException;
    CommonResponse<?> sendNetworkRequest(NetworkRequestDto dto);
    List<NetworkRequest> getIncomingRequests() throws CommonException;
}
