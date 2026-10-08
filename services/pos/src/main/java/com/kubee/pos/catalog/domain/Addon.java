package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.BaseEntity;
import com.kubee.pos.common.domain.Guard;
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

/** One extra, e.g. "Extra Cheese +₹20". Only changed through its {@link AddonGroup}. */
@Getter
@Entity
@Table(name = "addons")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Addon extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addon_group_id", nullable = false)
    private AddonGroup addonGroup;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private Money price;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", length = 10)
    private FoodType foodType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    Addon(AddonGroup addonGroup, AddonSpec spec) {
        this.addonGroup = addonGroup;
        update(spec);
    }

    void update(AddonSpec spec) {
        Guard.isTrue(spec.price() == null || !spec.price().isNegative(), "Add-on price cannot be negative");
        this.name = Guard.requireText(spec.name(), "Add-on name", 100);
        this.price = spec.price() == null ? Money.ZERO : spec.price();
        this.foodType = spec.foodType();
        this.sortOrder = spec.sortOrder();
        this.active = spec.active();
    }

    void remove() {
        markDeleted();
    }
}
