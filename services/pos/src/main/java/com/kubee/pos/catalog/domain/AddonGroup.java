package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.AggregateRoot;
import com.kubee.pos.common.domain.Guard;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A set of extras offered with items, e.g. "Toppings": Extra Cheese, Extra Butter.
 * minSelect / maxSelect say how many the customer must / may pick.
 */
@Getter
@Entity
@Table(name = "addon_groups")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AddonGroup extends AggregateRoot {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "min_select", nullable = false)
    private int minSelect;

    /** null = no upper limit. */
    @Column(name = "max_select")
    private Integer maxSelect;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "addonGroup", cascade = CascadeType.ALL)
    @SQLRestriction("is_deleted = false")
    @OrderBy("sortOrder ASC, id ASC")
    private List<Addon> addons = new ArrayList<>();

    public static AddonGroup create(String name, int minSelect, Integer maxSelect, List<AddonSpec> addons) {
        AddonGroup group = new AddonGroup();
        group.active = true;
        group.apply(name, minSelect, maxSelect, addons);
        return group;
    }

    /** Full replace, including the add-on list. */
    public void update(String name, int minSelect, Integer maxSelect, boolean active, List<AddonSpec> addons) {
        this.active = active;
        apply(name, minSelect, maxSelect, addons);
    }

    public void delete() {
        addons.forEach(Addon::remove);
        markDeleted();
    }

    public List<Addon> getAddons() {
        return addons.stream().filter(a -> !a.isDeleted()).toList();
    }

    private void apply(String name, int minSelect, Integer maxSelect, List<AddonSpec> specs) {
        this.name = Guard.requireText(name, "Add-on group name", 100);
        applyAddons(specs == null ? List.of() : specs);

        Guard.isTrue(minSelect >= 0, "Minimum selection cannot be negative");
        Guard.isTrue(maxSelect == null || maxSelect >= 1, "Maximum selection must be at least 1");
        Guard.isTrue(maxSelect == null || maxSelect >= minSelect, "Maximum selection cannot be less than minimum");
        long activeCount = getAddons().stream().filter(Addon::isActive).count();
        Guard.isTrue(minSelect <= activeCount,
                "Minimum selection (" + minSelect + ") is more than the active add-ons (" + activeCount + ")");
        this.minSelect = minSelect;
        this.maxSelect = maxSelect;
    }

    private void applyAddons(List<AddonSpec> specs) {
        Set<String> names = new HashSet<>();
        Set<String> seenUuids = new HashSet<>();
        for (AddonSpec spec : specs) {
            String addonName = Guard.requireText(spec.name(), "Add-on name", 100);
            Guard.isTrue(names.add(addonName.toLowerCase(Locale.ROOT)), "Duplicate add-on name: " + addonName);
            Guard.isTrue(spec.uuid() == null || seenUuids.add(spec.uuid()), "Add-on listed twice: " + spec.uuid());
        }

        Map<String, Addon> existing = new HashMap<>();
        getAddons().forEach(a -> existing.put(a.getUuid(), a));

        for (AddonSpec spec : specs) {
            if (spec.uuid() == null) {
                addons.add(new Addon(this, spec));
            } else {
                Addon addon = existing.remove(spec.uuid());
                Guard.isTrue(addon != null, "Add-on does not belong to this group: " + spec.uuid());
                addon.update(spec);
            }
        }
        existing.values().forEach(Addon::remove);
    }
}
