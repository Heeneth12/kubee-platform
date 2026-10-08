package com.kubee.inventory.stock.dto;
import java.math.BigDecimal;

public interface StockSearchProjection {
    Long getItemId();
    String getItemName();
    String getItemCode();
    String getItemSku();
    String getHsnSacCode();
    String getImageUrl();
    BigDecimal getSellingPrice();
    BigDecimal getDiscountPercentage();
    BigDecimal getTaxPercentage();

    String getBatchNumber();
    BigDecimal getBuyPrice();
    Integer getRemainingQty();
    Long getExpiryDate();
}