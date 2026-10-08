package com.kubee.pos.billing.domain;

import com.kubee.pos.common.domain.BaseEntity;
import com.kubee.pos.common.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/** "CGST 2.5% on 100.00 = 2.50" for one bill line. Drives the GST summary report. */
@Getter
@Entity
@Table(name = "bill_item_taxes")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillLineTax extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_item_id", nullable = false)
    private BillLine line;

    @Column(name = "tax_component_id")
    private Long taxComponentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private TaxType taxType;

    @Column(name = "tax_name", nullable = false, length = 50)
    private String taxName;

    @Column(name = "rate", nullable = false, precision = 6, scale = 3)
    private BigDecimal rate;

    @Column(name = "taxable_amount", nullable = false)
    private Money taxableAmount;

    @Column(name = "tax_amount", nullable = false)
    private Money taxAmount;

    BillLineTax(BillLine line, TaxComponentSpec component, Money taxableAmount, Money taxAmount) {
        this.line = line;
        this.taxComponentId = component.id();
        this.taxType = component.type();
        this.taxName = component.name();
        this.rate = component.rate();
        this.taxableAmount = taxableAmount;
        this.taxAmount = taxAmount;
    }
}
