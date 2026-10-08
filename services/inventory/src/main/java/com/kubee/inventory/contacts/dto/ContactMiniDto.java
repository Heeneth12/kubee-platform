package com.kubee.inventory.contacts.dto;

import com.kubee.inventory.contacts.entiry.Contact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactMiniDto {
    private Long id;
    private String contactCode;
    private String name;

    public ContactMiniDto(Contact contact){
        this.id = contact.getId();
        this.contactCode = contact.getContactCode();
        this.name = contact.getName();
    }
}
