package com.kubee.pos.catalog.api.dto;

import com.kubee.pos.catalog.domain.VariantSpec;
import com.kubee.pos.common.domain.Money;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** {@code uuid} null = new variant; existing uuid = update; variants left out of the list are removed. */
public record VariantRequest(
        String uuid,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 50) String itemCode,
        @Size(max = 100) String barcode,
        @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal sellingPrice,
        @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal mrp,
        Boolean isDefault,
        Integer sortOrder,
        Boolean active
) {

    public VariantSpec toSpec() {
        return new VariantSpec(uuid, name, itemCode, barcode, Money.ofNullable(sellingPrice), Money.ofNullable(mrp),
                Boolean.TRUE.equals(isDefault), sortOrder == null ? 0 : sortOrder, active == null || active);
    }
}
