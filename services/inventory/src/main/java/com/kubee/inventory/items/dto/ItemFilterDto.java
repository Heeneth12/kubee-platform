package com.kubee.inventory.items.dto;

import com.kubee.inventory.items.entity.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemFilterDto {
    String searchQuery;
    Boolean active;
    List<ItemType> itemTypes;
    String brand;
    String category;
}
