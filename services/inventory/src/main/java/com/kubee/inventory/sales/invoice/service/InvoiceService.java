package com.kubee.inventory.sales.invoice.service;

import com.kubee.inventory.sales.invoice.dto.InvoiceDto;
import com.kubee.inventory.sales.invoice.dto.InvoiceFilter;
import com.kubee.inventory.sales.invoice.dto.InvoiceStats;
import com.kubee.inventory.sales.invoice.entity.InvoiceStatus;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InvoiceService {

    CommonResponse<?> createInvoice(InvoiceDto dto) throws CommonException;

    CommonResponse<?> updateInvoice(Long id, InvoiceDto dto) throws CommonException;

    InvoiceDto getInvoiceById(Long invoiceId) throws CommonException;

    Page<InvoiceDto> getAllInvoices(InvoiceFilter filter, Integer page, Integer size) throws CommonException;

    List<InvoiceDto> searchInvoices(InvoiceFilter filter) throws CommonException;

    CommonResponse<?> updateInvoiceStatus(Long invoiceId, InvoiceStatus status) throws CommonException;

    byte[] downloadInvoicesExcel(InvoiceFilter filter) throws CommonException;

    InvoiceStats getStats(InvoiceFilter filter) throws CommonException;
}
