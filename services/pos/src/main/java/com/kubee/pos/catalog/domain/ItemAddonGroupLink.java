package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.BaseEntity;
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

/** "This item offers that add-on group." References the AddonGroup aggregate by id only. */
@Getter
@Entity
@Table(name = "item_addon_groups")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemAddonGroupLink extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "addon_group_id", nullable = false)
    private Long addonGroupId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    ItemAddonGroupLink(Item item, Long addonGroupId, int sortOrder) {
        this.item = item;
        this.addonGroupId = addonGroupId;
        this.sortOrder = sortOrder;
    }

    void reorder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    void remove() {
        markDeleted();
    }
}
