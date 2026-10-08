package com.kubee.pos.ordering.application.command;

import com.kubee.pos.common.application.NotFoundException;
import com.kubee.pos.common.domain.DomainException;
import com.kubee.pos.common.domain.Guard;
import com.kubee.pos.common.domain.Money;
import com.kubee.pos.ordering.application.port.CatalogPort;
import com.kubee.pos.ordering.application.port.CatalogPort.SellableAddon;
import com.kubee.pos.ordering.application.port.CatalogPort.SellableAddonGroup;
import com.kubee.pos.ordering.application.port.CatalogPort.SellableItem;
import com.kubee.pos.ordering.application.port.CatalogPort.SellableVariant;
import com.kubee.pos.ordering.domain.LineAddonSpec;
import com.kubee.pos.ordering.domain.LineSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns what the cashier picked into a {@link LineSpec}: checks the item, variant and add-ons are
 * sellable right now and takes the price / tax snapshot from the catalog.
 */
@Component
@RequiredArgsConstructor
class OrderLineFactory {

    private final CatalogPort catalog;

    LineSpec build(LineInput in) {
        Guard.isTrue(in.itemUuid() != null, "Item is required");
        SellableItem item = catalog.findItem(in.itemUuid())
                .orElseThrow(() -> NotFoundException.of("Item", in.itemUuid()));
        Guard.isTrue(item.active(), item.name() + " is not available right now");

        SellableVariant variant = pickVariant(item, in.variantUuid());
        Money price = pickPrice(item, variant, in);
        List<LineAddonSpec> addons = pickAddons(item, in.addons());

        return new LineSpec(item.id(), variant == null ? null : variant.id(), item.name(),
                variant == null ? null : variant.name(), item.hsnSacCode(), item.unitOfMeasure(), in.quantity(),
                price, item.priceIncludesTax(), item.taxGroupId(), item.taxRate(), addons, in.discount(), in.notes());
    }

    private static SellableVariant pickVariant(SellableItem item, String variantUuid) {
        if (!item.hasVariants()) {
            Guard.isTrue(variantUuid == null, item.name() + " has no variants");
            return null;
        }
        Guard.isTrue(variantUuid != null, "Pick a variant of " + item.name());
        SellableVariant variant = item.variants().stream()
                .filter(v -> v.uuid().equals(variantUuid))
                .findFirst()
                .orElseThrow(() -> new DomainException("Variant " + variantUuid + " is not part of " + item.name()));
        Guard.isTrue(variant.active(), item.name() + " (" + variant.name() + ") is not available right now");
        return variant;
    }

    private static Money pickPrice(SellableItem item, SellableVariant variant, LineInput in) {
        if (item.openPrice()) {
            Guard.isTrue(in.unitPrice() != null, "Enter the price for " + item.name());
            return Money.of(in.unitPrice());
        }
        Guard.isTrue(in.unitPrice() == null, "Price can only be entered for open-price items");
        return Money.of(variant == null ? item.sellingPrice() : variant.sellingPrice());
    }

    /** Every chosen add-on must belong to an active group linked to the item; each group's min/max is enforced. */
    private static List<LineAddonSpec> pickAddons(SellableItem item, List<LineInput.AddonChoice> choices) {
        Set<String> seen = new HashSet<>();
        for (LineInput.AddonChoice choice : choices) {
            Guard.isTrue(choice.addonUuid() != null, "Add-on is required");
            Guard.isTrue(seen.add(choice.addonUuid()), "Add-on listed twice: " + choice.addonUuid());
        }

        List<LineAddonSpec> specs = new ArrayList<>();
        Set<String> matched = new HashSet<>();
        for (SellableAddonGroup group : item.addonGroups()) {
            if (!group.active()) {
                continue;
            }
            int picked = 0;
            for (SellableAddon addon : group.addons()) {
                LineInput.AddonChoice choice = choices.stream()
                        .filter(c -> c.addonUuid().equals(addon.uuid()))
                        .findFirst().orElse(null);
                if (choice == null) {
                    continue;
                }
                Guard.isTrue(addon.active(), addon.name() + " is not available right now");
                specs.add(new LineAddonSpec(addon.id(), addon.name(), choice.quantity(), Money.of(addon.price())));
                matched.add(addon.uuid());
                picked++;
            }
            Guard.isTrue(picked >= group.minSelect(),
                    "Choose at least " + group.minSelect() + " from " + group.name());
            Guard.isTrue(group.maxSelect() == null || picked <= group.maxSelect(),
                    "Choose at most " + group.maxSelect() + " from " + group.name());
        }
        choices.stream()
                .filter(c -> !matched.contains(c.addonUuid()))
                .findFirst()
                .ifPresent(c -> {
                    throw new DomainException("Add-on " + c.addonUuid() + " is not offered with " + item.name());
                });
        return specs;
    }
}
