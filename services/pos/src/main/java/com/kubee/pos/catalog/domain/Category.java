package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.AggregateRoot;
import com.kubee.pos.common.domain.Guard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Menu / shelf grouping such as "Maggi" or "Beverages".
 * At most two levels deep (Category > Sub-category) to keep the billing screen simple.
 */
@Getter
@Entity
@Table(name = "categories")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends AggregateRoot {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public static Category create(String name, Category parent, String imageUrl, int sortOrder) {
        Category category = new Category();
        category.active = true;
        category.apply(name, parent, false, imageUrl, sortOrder);
        return category;
    }

    /**
     * @param hasChildren whether other categories sit under this one; such a category cannot itself become a child.
     */
    public void update(String name, Category parent, boolean hasChildren, String imageUrl, int sortOrder, boolean active) {
        apply(name, parent, hasChildren, imageUrl, sortOrder);
        this.active = active;
    }

    public void delete() {
        markDeleted();
    }

    private void apply(String name, Category parent, boolean hasChildren, String imageUrl, int sortOrder) {
        this.name = Guard.requireText(name, "Category name", 255);
        this.imageUrl = Guard.optionalText(imageUrl, "Image URL", 500);
        this.sortOrder = sortOrder;
        if (parent == null) {
            this.parentId = null;
            return;
        }
        Guard.isTrue(!parent.equals(this), "A category cannot be its own parent");
        Guard.isTrue(parent.getParentId() == null, "Parent must be a top-level category (max 2 levels)");
        Guard.isTrue(!hasChildren, "A category that has sub-categories cannot be moved under another category");
        this.parentId = parent.getId();
    }
}
