package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemTest {

    private static ItemDetails details(String name, String code) {
        return new ItemDetails(name, null, code, null, null, null, FoodType.VEG, "plate", null, null, null, 0);
    }

    private static ItemPricing price(String selling, String mrp) {
        return new ItemPricing(selling == null ? null : Money.of(selling), mrp == null ? null : Money.of(mrp),
                true, null, false);
    }

    private static VariantSpec variant(String uuid, String name, String price, boolean isDefault) {
        return new VariantSpec(uuid, name, null, null, Money.of(price), null, isDefault, 0, true);
    }

    @Test
    void createsSimpleItemWithDefaults() {
        Item item = Item.create(details(" Veg Maggi ", "101"), price("60", null), List.of(), List.of());

        assertThat(item.getName()).isEqualTo("Veg Maggi");
        assertThat(item.getUnitOfMeasure()).isEqualTo("PLATE");
        assertThat(item.getItemType()).isEqualTo(ItemType.GOODS);
        assertThat(item.getSellingPrice()).isEqualTo(Money.of("60"));
        assertThat(item.isActive()).isTrue();
        assertThat(item.isHasVariants()).isFalse();
        assertThat(item.getUuid()).hasSize(36);
    }

    @Test
    void rejectsPriceAboveMrp() {
        assertThatThrownBy(() -> Item.create(details("Chips", null), price("25", "20"), List.of(), List.of()))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("MRP");
    }

    @Test
    void requiresPriceUnlessOpenPrice() {
        assertThatThrownBy(() -> Item.create(details("Chips", null), price(null, null), List.of(), List.of()))
                .isInstanceOf(DomainException.class);

        var openPrice = new ItemPricing(null, null, true, null, true);
        Item item = Item.create(details("Misc", null), openPrice, List.of(), List.of());
        assertThat(item.getSellingPrice()).isEqualTo(Money.ZERO);
    }

    @Test
    void rejectsBadHsnCode() {
        var bad = new ItemDetails("Tea", null, null, null, null, null, null, null, "12ab", null, null, 0);
        assertThatThrownBy(() -> Item.create(bad, price("10", null), List.of(), List.of()))
                .hasMessageContaining("HSN");
    }

    @Test
    void firstActiveVariantBecomesDefaultAndDrivesItemPrice() {
        Item item = Item.create(details("Veg Maggi", null), price(null, null),
                List.of(variant(null, "Half", "40", false), variant(null, "Full", "70", false)), List.of());

        assertThat(item.isHasVariants()).isTrue();
        assertThat(item.getVariants()).hasSize(2);
        assertThat(item.getVariants().getFirst().isDefaultVariant()).isTrue();
        assertThat(item.getSellingPrice()).isEqualTo(Money.of("40"));
    }

    @Test
    void explicitDefaultVariantWins() {
        Item item = Item.create(details("Veg Maggi", null), price(null, null),
                List.of(variant(null, "Half", "40", false), variant(null, "Full", "70", true)), List.of());

        assertThat(item.getSellingPrice()).isEqualTo(Money.of("70"));
    }

    @Test
    void rejectsTwoDefaultsAndDuplicateNames() {
        assertThatThrownBy(() -> Item.create(details("X", null), price(null, null),
                List.of(variant(null, "Half", "40", true), variant(null, "Full", "70", true)), List.of()))
                .hasMessageContaining("Only one variant");
        assertThatThrownBy(() -> Item.create(details("X", null), price(null, null),
                List.of(variant(null, "Half", "40", false), variant(null, "half", "70", false)), List.of()))
                .hasMessageContaining("Duplicate variant name");
    }

    @Test
    void updateUpsertsAndRemovesVariants() {
        Item item = Item.create(details("Veg Maggi", null), price(null, null),
                List.of(variant(null, "Half", "40", false), variant(null, "Full", "70", false)), List.of());
        String halfUuid = item.getVariants().get(0).getUuid();

        // keep Half (new price), drop Full, add Jumbo
        item.update(details("Veg Maggi", null), price(null, null),
                List.of(variant(halfUuid, "Half", "45", true), variant(null, "Jumbo", "90", false)), List.of());

        assertThat(item.getVariants()).extracting(ItemVariant::getName).containsExactly("Half", "Jumbo");
        assertThat(item.getVariants().getFirst().getUuid()).isEqualTo(halfUuid);
        assertThat(item.getSellingPrice()).isEqualTo(Money.of("45"));
    }

    @Test
    void removingAllVariantsNeedsItemPriceAgain() {
        Item item = Item.create(details("Veg Maggi", null), price(null, null),
                List.of(variant(null, "Half", "40", false)), List.of());

        item.update(details("Veg Maggi", null), price("55", null), List.of(), List.of());

        assertThat(item.isHasVariants()).isFalse();
        assertThat(item.getVariants()).isEmpty();
        assertThat(item.getSellingPrice()).isEqualTo(Money.of("55"));
    }

    @Test
    void rejectsVariantFromAnotherItem() {
        Item item = Item.create(details("Veg Maggi", null), price("60", null), List.of(), List.of());
        assertThatThrownBy(() -> item.update(details("Veg Maggi", null), price(null, null),
                List.of(variant("not-mine", "Half", "40", false)), List.of()))
                .hasMessageContaining("does not belong");
    }

    @Test
    void openPriceItemsCannotHaveVariants() {
        var openPrice = new ItemPricing(null, null, true, null, true);
        assertThatThrownBy(() -> Item.create(details("Misc", null), openPrice,
                List.of(variant(null, "Half", "40", false)), List.of()))
                .hasMessageContaining("Open-price");
    }

    @Test
    void codesMustBeUniqueWithinTheItem() {
        var v = new VariantSpec(null, "Half", "101", null, Money.of("40"), null, false, 0, true);
        assertThatThrownBy(() -> Item.create(details("Veg Maggi", "101"), price(null, null), List.of(v), List.of()))
                .hasMessageContaining("more than once");
    }

    @Test
    void allItemCodesIncludesVariantCodes() {
        var v = new VariantSpec(null, "Half", "101H", "890100", Money.of("40"), null, false, 0, true);
        Item item = Item.create(details("Veg Maggi", "101"), price(null, null), List.of(v), List.of());

        assertThat(item.allItemCodes()).containsExactly("101", "101H");
        assertThat(item.allBarcodes()).containsExactly("890100");
    }

    @Test
    void addonGroupLinksAreSyncedAndDeduplicated() {
        Item item = Item.create(details("Veg Maggi", null), price("60", null), List.of(), List.of(1L, 2L, 1L));
        assertThat(item.getAddonGroupIds()).containsExactly(1L, 2L);

        item.update(details("Veg Maggi", null), price("60", null), List.of(), List.of(2L, 3L));
        assertThat(item.getAddonGroupIds()).containsExactly(2L, 3L);
    }

    @Test
    void deleteRemovesChildrenAndRaisesEvent() {
        Item item = Item.create(details("Veg Maggi", null), price(null, null),
                List.of(variant(null, "Half", "40", false)), List.of(1L));

        item.delete();

        assertThat(item.isDeleted()).isTrue();
        assertThat(item.getVariants()).isEmpty();
        assertThat(item.getAddonGroupIds()).isEmpty();
        assertThat(item.domainEvents()).extracting(e -> e.getClass().getSimpleName())
                .containsExactly("ItemCreated", "ItemDeleted");
    }
}
