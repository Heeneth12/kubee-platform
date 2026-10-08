package com.kubee.auth.user.dto;

import com.kubee.auth.common.dto.AddressDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserMiniDto {
    private Long id;
    private String userType;
    private String UserUuid;
    private String name;
    private String email;
    private String phone;
    private List<AddressDto> userAddresses;
}
