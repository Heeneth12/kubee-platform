package com.kubee.inventory.employee.dto;

import com.kubee.inventory.contacts.dto.AddressDto;
import com.kubee.inventory.employee.entity.EmployeeRole;
import com.kubee.inventory.employee.entity.Gender;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDto {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private Gender gender;
    private EmployeeRole role;
    private String officialEmail;
    private String personalEmail;
    private String contactNumber;
    private Boolean active;
    private AddressDto address;
}
