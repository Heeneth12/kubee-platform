package com.kubee.pos.catalog.domain;

import com.kubee.pos.catalog.domain.event.ItemCreated;
import com.kubee.pos.catalog.domain.event.ItemDeleted;
import com.kubee.pos.catalog.domain.event.ItemUpdated;
import com.kubee.pos.common.domain.AggregateRoot;
import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Something the shop sells, e.g. "Veg Maggi". Owns its variants (Half / Full) and
 * the links to the add-on groups offered with it. No stock is tracked here.
 */
@Getter
@Entity
@Table(name = "items")
@SQLRestriction("is_deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends AggregateRoot {

    private static final Pattern HSN_SAC = Pattern.compile("\\d{4,8}");
    private static final String DEFAULT_UNIT = "PCS";

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(name = "item_code", length = 50)
    private String itemCode;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private ItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", length = 10)
    private FoodType foodType;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "selling_price", nullable = false)
    private Money sellingPrice;

    @Column(name = "mrp")
    private Money mrp;

    @Column(name = "price_includes_tax", nullable = false)
    private boolean priceIncludesTax;

    @Column(name = "tax_group_id")
    private Long taxGroupId;

    @Column(name = "hsn_sac_code", length = 20)
    private String hsnSacCode;

    @Column(name = "has_variants", nullable = false)
    private boolean hasVariants;

    @Column(name = "is_open_price", nullable = false)
    private boolean openPrice;

    @Column(name = "is_favourite", nullable = false)
    private boolean favourite;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    @SQLRestriction("is_deleted = false")
    @OrderBy("sortOrder ASC, id ASC")
    private List<ItemVariant> variants = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    @SQLRestriction("is_deleted = false")
    @OrderBy("sortOrder ASC, id ASC")
    private List<ItemAddonGroupLink> addonGroupLinks = new ArrayList<>();

    public static Item create(ItemDetails details, ItemPricing pricing, List<VariantSpec> variants, List<Long> addonGroupIds) {
        Item item = new Item();
        item.active = true;
        item.apply(details, pricing, variants, addonGroupIds);
        item.registerEvent(new ItemCreated(item.getUuid()));
        return item;
    }

    /** Full replace: details, price, variants and add-on groups all take the given state. */
    public void update(ItemDetails details, ItemPricing pricing, List<VariantSpec> variants, List<Long> addonGroupIds) {
        apply(details, pricing, variants, addonGroupIds);
        registerEvent(new ItemUpdated(getUuid()));
    }

    /** Inactive items stay in the catalog but cannot be billed (e.g. "out of stock today"). */
    public void changeActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            registerEvent(new ItemUpdated(getUuid()));
        }
    }

    public void changeFavourite(boolean favourite) {
        if (this.favourite != favourite) {
            this.favourite = favourite;
            registerEvent(new ItemUpdated(getUuid()));
        }
    }

    public void delete() {
        variants.forEach(ItemVariant::remove);
        addonGroupLinks.forEach(ItemAddonGroupLink::remove);
        markDeleted();
        registerEvent(new ItemDeleted(getUuid()));
    }

    public List<ItemVariant> getVariants() {
        return variants.stream().filter(v -> !v.isDeleted()).toList();
    }

    public List<Long> getAddonGroupIds() {
        return liveLinks().map(ItemAddonGroupLink::getAddonGroupId).toList();
    }

    /** Item code plus all variant codes; all must be unique across the shop's catalog. */
    public Set<String> allItemCodes() {
        return collect(itemCode, ItemVariant::getItemCode);
    }

    public Set<String> allBarcodes() {
        return collect(barcode, ItemVariant::getBarcode);
    }

    // ------------------------------------------------------------------

    private void apply(ItemDetails details, ItemPricing pricing, List<VariantSpec> variantSpecs, List<Long> addonGroupIds) {
        applyDetails(details);
        applyVariants(variantSpecs == null ? List.of() : variantSpecs);
        applyPricing(pricing);
        applyAddonGroups(addonGroupIds == null ? List.of() : addonGroupIds);
        checkCodesUniqueWithinItem();
    }

    private void applyDetails(ItemDetails d) {
        this.name = Guard.requireText(d.name(), "Item name", 255);
        this.shortName = Guard.optionalText(d.shortName(), "Short name", 50);
        this.itemCode = Guard.optionalText(d.itemCode(), "Item code", 50);
        this.barcode = Guard.optionalText(d.barcode(), "Barcode", 100);
        this.categoryId = d.categoryId();
        this.itemType = d.itemType() == null ? ItemType.GOODS : d.itemType();
        this.foodType = d.foodType();
        String unit = Guard.optionalText(d.unitOfMeasure(), "Unit of measure", 20);
        this.unitOfMeasure = unit == null ? DEFAULT_UNIT : unit.toUpperCase(Locale.ROOT);
        this.hsnSacCode = Guard.optionalPattern(d.hsnSacCode(), "HSN/SAC code", HSN_SAC, "must be 4 to 8 digits");
        this.imageUrl = Guard.optionalText(d.imageUrl(), "Image URL", 500);
        this.description = Guard.trimToNull(d.description());
        this.sortOrder = d.sortOrder();
    }

    private void applyVariants(List<VariantSpec> specs) {
        Set<String> names = new HashSet<>();
        Set<String> seenUuids = new HashSet<>();
        for (VariantSpec spec : specs) {
            String variantName = Guard.requireText(spec.name(), "Variant name", 100);
            Guard.isTrue(names.add(variantName.toLowerCase(Locale.ROOT)), "Duplicate variant name: " + variantName);
            Guard.isTrue(spec.uuid() == null || seenUuids.add(spec.uuid()), "Variant listed twice: " + spec.uuid());
        }

        Map<String, ItemVariant> existing = new HashMap<>();
        getVariants().forEach(v -> existing.put(v.getUuid(), v));

        for (VariantSpec spec : specs) {
            if (spec.uuid() == null) {
                variants.add(new ItemVariant(this, spec));
            } else {
                ItemVariant variant = existing.remove(spec.uuid());
                Guard.isTrue(variant != null, "Variant does not belong to this item: " + spec.uuid());
                variant.update(spec);
            }
        }
        existing.values().forEach(ItemVariant::remove);

        this.hasVariants = !specs.isEmpty();
        if (hasVariants) {
            resolveDefaultVariant();
        }
    }

    private void resolveDefaultVariant() {
        List<ItemVariant> live = getVariants().stream()
                .sorted(Comparator.comparingInt(ItemVariant::getSortOrder))
                .toList();
        List<ItemVariant> defaults = live.stream().filter(ItemVariant::isDefaultVariant).toList();
        Guard.isTrue(defaults.size() <= 1, "Only one variant can be the default");
        if (defaults.isEmpty()) {
            ItemVariant firstActive = live.stream().filter(ItemVariant::isActive).findFirst()
                    .orElseThrow(() -> new DomainException("At least one variant must be active"));
            firstActive.markDefault(true);
        } else {
            Guard.isTrue(defaults.getFirst().isActive(), "The default variant must be active");
        }
    }

    private void applyPricing(ItemPricing p) {
        this.priceIncludesTax = p.priceIncludesTax();
        this.taxGroupId = p.taxGroupId();
        this.openPrice = p.openPrice();
        this.mrp = p.mrp();
        Guard.isTrue(mrp == null || !mrp.isNegative(), "MRP cannot be negative");

        if (hasVariants) {
            Guard.isTrue(!openPrice, "Open-price items cannot have variants");
            this.sellingPrice = getVariants().stream()
                    .filter(ItemVariant::isDefaultVariant)
                    .findFirst()
                    .orElseThrow()
                    .getSellingPrice();
        } else {
            Money price = p.sellingPrice() == null && openPrice ? Money.ZERO : p.sellingPrice();
            Guard.isTrue(price != null, "Selling price is required");
            checkPrice(price, mrp);
            this.sellingPrice = price;
        }
    }

    private void applyAddonGroups(List<Long> addonGroupIds) {
        List<Long> wanted = List.copyOf(new LinkedHashSet<>(addonGroupIds));
        Map<Long, ItemAddonGroupLink> existing = new HashMap<>();
        liveLinks().forEach(link -> existing.put(link.getAddonGroupId(), link));

        for (int i = 0; i < wanted.size(); i++) {
            Long groupId = Objects.requireNonNull(wanted.get(i), "addon group id");
            ItemAddonGroupLink link = existing.remove(groupId);
            if (link == null) {
                addonGroupLinks.add(new ItemAddonGroupLink(this, groupId, i));
            } else {
                link.reorder(i);
            }
        }
        existing.values().forEach(ItemAddonGroupLink::remove);
    }

    private void checkCodesUniqueWithinItem() {
        checkNoDuplicates(Stream.concat(Stream.of(itemCode), getVariants().stream().map(ItemVariant::getItemCode)), "Item code");
        checkNoDuplicates(Stream.concat(Stream.of(barcode), getVariants().stream().map(ItemVariant::getBarcode)), "Barcode");
    }

    private static void checkNoDuplicates(Stream<String> values, String label) {
        Set<String> seen = new HashSet<>();
        values.filter(Objects::nonNull)
                .forEach(v -> Guard.isTrue(seen.add(v), label + " " + v + " is used more than once in this item"));
    }

    private Set<String> collect(String own, Function<ItemVariant, String> fromVariant) {
        Set<String> values = new LinkedHashSet<>();
        if (own != null) values.add(own);
        getVariants().stream().map(fromVariant).filter(Objects::nonNull).forEach(values::add);
        return values;
    }

    private Stream<ItemAddonGroupLink> liveLinks() {
        return addonGroupLinks.stream().filter(link -> !link.isDeleted());
    }

    static void checkPrice(Money price, Money mrp) {
        Guard.isTrue(!price.isNegative(), "Selling price cannot be negative");
        Guard.isTrue(mrp == null || !price.isGreaterThan(mrp), "Selling price cannot be more than MRP");
    }
}
