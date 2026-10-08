package com.kubee.auth.common.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PrivilegeDto {
    private Long id;
    private String privilegeName;
    private String privilegeKey;
    private String description;
}
