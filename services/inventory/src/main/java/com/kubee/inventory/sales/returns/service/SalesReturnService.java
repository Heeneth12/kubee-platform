package com.kubee.inventory.sales.returns.service;

import com.kubee.inventory.sales.returns.dto.SalesReturnDto;
import com.kubee.inventory.sales.returns.dto.SalesReturnFilter;
import com.kubee.inventory.sales.returns.dto.SalesReturnRequestDto;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

public interface SalesReturnService {

    CommonResponse<?> createSalesReturn(SalesReturnRequestDto request)  throws CommonException;

    Page<SalesReturnDto> getSalesReturns(SalesReturnFilter filter, Integer page, Integer size) throws CommonException;

    SalesReturnDto getSalesReturnById(Long id) throws CommonException;

}
