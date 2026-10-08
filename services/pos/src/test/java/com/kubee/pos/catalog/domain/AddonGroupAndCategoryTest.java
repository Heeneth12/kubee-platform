package com.kubee.pos.catalog.domain;

import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Money;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddonGroupAndCategoryTest {

    private static AddonSpec addon(String name, String price) {
        return new AddonSpec(null, name, price == null ? null : Money.of(price), FoodType.VEG, 0, true);
    }

    @Test
    void addonGroupWithAddons() {
        AddonGroup group = AddonGroup.create("Toppings", 0, 2,
                List.of(addon("Extra Cheese", "20"), addon("Extra Butter", null)));

        assertThat(group.getAddons()).extracting(Addon::getName).containsExactly("Extra Cheese", "Extra Butter");
        assertThat(group.getAddons().get(1).getPrice()).isEqualTo(Money.ZERO);
    }

    @Test
    void addonSelectionRulesAreChecked() {
        assertThatThrownBy(() -> AddonGroup.create("Toppings", 2, 1, List.of(addon("A", "1"), addon("B", "1"))))
                .hasMessageContaining("cannot be less than minimum");
        assertThatThrownBy(() -> AddonGroup.create("Toppings", 3, null, List.of(addon("A", "1"))))
                .hasMessageContaining("more than the active add-ons");
        assertThatThrownBy(() -> AddonGroup.create("Toppings", 0, null, List.of(addon("A", "1"), addon("a", "2"))))
                .hasMessageContaining("Duplicate add-on name");
    }

    @Test
    void addonUpdateRemovesMissingAddons() {
        AddonGroup group = AddonGroup.create("Toppings", 0, null, List.of(addon("A", "1"), addon("B", "2")));
        String aUuid = group.getAddons().getFirst().getUuid();

        group.update("Toppings", 0, null, true,
                List.of(new AddonSpec(aUuid, "A", Money.of("5"), null, 0, true)));

        assertThat(group.getAddons()).hasSize(1);
        assertThat(group.getAddons().getFirst().getPrice()).isEqualTo(Money.of("5"));
    }

    @Test
    void categoryIsAtMostTwoLevelsDeep() {
        Category food = Category.create("Food", null, null, 0);
        ReflectionTestUtils.setField(food, "id", 1L); // as if saved
        Category maggi = Category.create("Maggi", food, null, 0);
        ReflectionTestUtils.setField(maggi, "id", 2L);

        assertThatThrownBy(() -> Category.create("Cheese Maggi", maggi, null, 0))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("top-level");
        assertThatThrownBy(() -> food.update("Food", food, false, null, 0, true))
                .hasMessageContaining("own parent");
        assertThatThrownBy(() -> food.update("Food", Category.create("Drinks", null, null, 0), true, null, 0, true))
                .hasMessageContaining("has sub-categories");
    }

    @Test
    void categoryNameIsRequired() {
        assertThatThrownBy(() -> Category.create("  ", null, null, 0)).hasMessageContaining("required");
    }
}
