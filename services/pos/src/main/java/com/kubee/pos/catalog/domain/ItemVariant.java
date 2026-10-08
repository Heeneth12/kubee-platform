package com.kubee.pos.catalog.domain;

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

/** Half / Full, 250g / 500g ... Only changed through its {@link Item}. */
@Getter
@Entity
@Table(name = "item_variants")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemVariant extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "item_code", length = 50)
    private String itemCode;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "selling_price", nullable = false)
    private Money sellingPrice;

    @Column(name = "mrp")
    private Money mrp;

    @Column(name = "is_default", nullable = false)
    private boolean defaultVariant;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    ItemVariant(Item item, VariantSpec spec) {
        this.item = item;
        update(spec);
    }

    void update(VariantSpec spec) {
        Guard.isTrue(spec.sellingPrice() != null, "Variant price is required");
        Guard.isTrue(spec.mrp() == null || !spec.mrp().isNegative(), "Variant MRP cannot be negative");
        Item.checkPrice(spec.sellingPrice(), spec.mrp());
        this.name = Guard.requireText(spec.name(), "Variant name", 100);
        this.itemCode = Guard.optionalText(spec.itemCode(), "Variant item code", 50);
        this.barcode = Guard.optionalText(spec.barcode(), "Variant barcode", 100);
        this.sellingPrice = spec.sellingPrice();
        this.mrp = spec.mrp();
        this.defaultVariant = spec.isDefault();
        this.sortOrder = spec.sortOrder();
        this.active = spec.active();
    }

    void markDefault(boolean isDefault) {
        this.defaultVariant = isDefault;
    }

    void remove() {
        markDeleted();
    }
}
