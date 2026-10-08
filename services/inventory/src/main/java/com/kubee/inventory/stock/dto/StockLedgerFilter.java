package com.kubee.inventory.stock.dto;

import com.kubee.inventory.stock.entity.MovementType;
import com.kubee.inventory.utils.common.CommonFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StockLedgerFilter extends CommonFilter {
     private Long itemId;
     private List<MovementType> transactionTypes;
     private List<String> referenceTypes;
}
