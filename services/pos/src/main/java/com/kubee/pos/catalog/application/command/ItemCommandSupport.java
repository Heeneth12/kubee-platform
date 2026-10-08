package com.kubee.pos.catalog.application.command;

import com.kubee.pos.catalog.application.port.TaxGroupLookup;
import com.kubee.pos.catalog.domain.AddonGroup;
import com.kubee.pos.catalog.domain.AddonGroupRepository;
import com.kubee.pos.catalog.domain.CatalogConstraints;
import com.kubee.pos.catalog.domain.CategoryRepository;
import com.kubee.pos.catalog.domain.Item;
import com.kubee.pos.catalog.domain.ItemDetails;
import com.kubee.pos.catalog.domain.ItemPricing;
import com.kubee.pos.common.application.ConflictException;
import com.kubee.pos.common.application.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Shared by create/update item: resolves uuids to ids and checks shop-wide uniqueness. */
@Component
@RequiredArgsConstructor
class ItemCommandSupport {

    record Resolved(ItemDetails details, ItemPricing pricing, List<Long> addonGroupIds) {
    }

    private final CategoryRepository categoryRepository;
    private final AddonGroupRepository addonGroupRepository;
    private final TaxGroupLookup taxGroupLookup;
    private final CatalogConstraints constraints;

    Resolved resolve(ItemInput in) {
        Long categoryId = in.categoryUuid() == null ? null
                : categoryRepository.findByUuid(in.categoryUuid())
                        .orElseThrow(() -> NotFoundException.of("Category", in.categoryUuid()))
                        .getId();
        Long taxGroupId = in.taxGroupUuid() == null ? null
                : taxGroupLookup.findIdByUuid(in.taxGroupUuid())
                        .orElseThrow(() -> NotFoundException.of("Tax group", in.taxGroupUuid()));

        var details = new ItemDetails(in.name(), in.shortName(), in.itemCode(), in.barcode(), categoryId,
                in.itemType(), in.foodType(), in.unitOfMeasure(), in.hsnSacCode(), in.imageUrl(),
                in.description(), in.sortOrder());
        var pricing = new ItemPricing(in.sellingPrice(), in.mrp(), in.priceIncludesTax(), taxGroupId, in.openPrice());
        return new Resolved(details, pricing, resolveAddonGroups(in.addonGroupUuids()));
    }

    void checkUnique(Item item) {
        for (String code : item.allItemCodes()) {
            if (constraints.isItemCodeTaken(code, item.getId())) {
                throw new ConflictException("Item code " + code + " is already used by another item");
            }
        }
        for (String barcode : item.allBarcodes()) {
            if (constraints.isBarcodeTaken(barcode, item.getId())) {
                throw new ConflictException("Barcode " + barcode + " is already used by another item");
            }
        }
    }

    private List<Long> resolveAddonGroups(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }
        List<String> wanted = List.copyOf(new LinkedHashSet<>(uuids));
        Map<String, AddonGroup> found = addonGroupRepository.findByUuidIn(wanted).stream()
                .collect(Collectors.toMap(AddonGroup::getUuid, Function.identity()));
        return wanted.stream()
                .map(uuid -> {
                    AddonGroup group = found.get(uuid);
                    if (group == null) throw NotFoundException.of("Add-on group", uuid);
                    return group.getId();
                })
                .toList();
    }
}
