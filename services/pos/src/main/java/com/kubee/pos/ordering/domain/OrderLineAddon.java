package com.kubee.pos.ordering.domain;

import com.kubee.pos.common.domain.BaseEntity;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.Objects;

/** "+ Extra Cheese" on a line. Snapshot of the add-on's name and price. Only changed through its {@link Order}. */
@Getter
@Entity
@Table(name = "order_item_addons")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderLineAddon extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderLine line;

    @Column(name = "addon_id")
    private Long addonId;

    @Column(name = "addon_name", nullable = false, length = 100)
    private String addonName;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private Money unitPrice;

    OrderLineAddon(OrderLine line, LineAddonSpec spec) {
        Guard.isTrue(spec.quantity() != null && spec.quantity().signum() > 0, "Add-on quantity must be more than 0");
        Guard.isTrue(spec.unitPrice() != null && !spec.unitPrice().isNegative(), "Add-on price cannot be negative");
        this.line = line;
        this.addonId = spec.addonId();
        this.addonName = Guard.requireText(spec.name(), "Add-on name", 100);
        this.quantity = spec.quantity();
        this.unitPrice = spec.unitPrice();
    }

    /** Price of this add-on for one unit of the parent line. */
    Money perUnitPrice() {
        return unitPrice.times(quantity);
    }

    boolean sameAs(LineAddonSpec spec) {
        return Objects.equals(addonId, spec.addonId()) && quantity.compareTo(spec.quantity()) == 0;
    }

    void remove() {
        markDeleted();
    }
}
