package com.kubee.pos.billing.application.command;

import com.kubee.pos.common.cqrs.Command;

/**
 * Issue the GST invoice for a completed order. Customer name/phone left out are taken from the order.
 * If the order already has an issued bill, that bill is returned instead (safe to retry).
 *
 * @param series        counter/device series, default "A"
 * @param customerGstin only for B2B bills
 * @param placeOfSupply 2-digit state code; defaults to the GSTIN's state, else the shop's state
 */
public record IssueBillCommand(
        String orderUuid,
        String series,
        String customerName,
        String customerPhone,
        String customerGstin,
        String placeOfSupply
) implements Command<String> {
}
