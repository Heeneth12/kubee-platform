package com.kubee.inventory.payment.service;

import com.kubee.inventory.payment.dto.AdvanceDto;
import com.kubee.inventory.payment.dto.CreditNoteDto;
import com.kubee.inventory.payment.dto.CreditNoteRefundRequestDto;
import com.kubee.inventory.payment.dto.CreditNoteUtilizeDto;
import com.kubee.inventory.utils.common.CommonFilter;
import com.kubee.common.CommonResponse;
import com.kubee.common.CommonException;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface CreditNoteService {

    /**
     * Auto-called by SalesReturn service when a return is confirmed
     */
    CommonResponse<?> createCreditNote(Long customerId, BigDecimal amount, Long sourceReturnId) throws CommonException;


    Page<CreditNoteDto> getAllCreditNote(Integer page, Integer size, CommonFilter commonFilter) throws CommonException;

    /**
     * Apply a specific credit note (or part of it) to an invoice
     */
    CommonResponse<?> utilizeCreditNote(CreditNoteUtilizeDto dto) throws CommonException;

    /**
     * Initiate a cash refund of a credit note balance — creates refund in PENDING state
     */
    CommonResponse<?> refundCreditNote(CreditNoteRefundRequestDto dto) throws CommonException;

    /**
     * Mark a PENDING CN refund as CLEARED
     */
    CommonResponse<?> confirmCreditNoteRefund(Long refundId) throws CommonException;

    /**
     * Single credit note with full utilization and refund history
     */
    CreditNoteDto getCreditNote(Long creditNoteId) throws CommonException;

    /**
     * All credit notes for a customer
     */
    List<CreditNoteDto> getCreditNotesByCustomer(Long customerId) throws CommonException;
}
