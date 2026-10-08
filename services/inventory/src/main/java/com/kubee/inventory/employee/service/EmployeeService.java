package com.kubee.inventory.employee.service;

import com.kubee.inventory.employee.dto.EmployeeDto;
import com.kubee.inventory.employee.dto.EmployeeFilter;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

public interface EmployeeService {

    CommonResponse createEmployee(EmployeeDto employeeDto) throws CommonException;
    CommonResponse updateEmployee(Long id, EmployeeDto employeeDto) throws CommonException;
    EmployeeDto getEmployee(Long id) throws CommonException;
    Page<EmployeeDto> getAllEmployees(EmployeeFilter filter, Integer page, Integer size) throws CommonException;
    CommonResponse toggleStatus(Long id, Boolean active) throws CommonException;

}
