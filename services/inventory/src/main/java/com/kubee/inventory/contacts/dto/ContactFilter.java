package com.kubee.inventory.contacts.dto;

import com.kubee.inventory.contacts.entiry.ContactType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactFilter {
    private String searchQuery;
    private String name;
    private String email;
    private String phone;
    private String gstNumber;
    private ContactType type;
    private Boolean Active;

}