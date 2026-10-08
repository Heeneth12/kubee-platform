package com.kubee.pos.billing.api.dto;

import com.kubee.pos.billing.application.command.IssueBillCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Only {@code orderUuid} is required. Customer name/phone default to the order's.
 * For a B2B bill send {@code customerGstin}; the place of supply then defaults to its state.
 */
public record IssueBillRequest(
        @NotBlank String orderUuid,
        @Size(max = 4) String series,
        @Size(max = 255) String customerName,
        @Size(max = 20) String customerPhone,
        @Size(max = 15) String customerGstin,
        @Pattern(regexp = "\\d{2}", message = "must be a 2-digit state code") String placeOfSupply
) {

    public IssueBillCommand toCommand() {
        return new IssueBillCommand(orderUuid, series, customerName, customerPhone, customerGstin, placeOfSupply);
    }
}
