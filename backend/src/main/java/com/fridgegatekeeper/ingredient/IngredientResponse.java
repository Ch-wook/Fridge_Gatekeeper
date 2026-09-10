package com.fridgegatekeeper.ingredient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record IngredientResponse(Long id, String name, Category category, BigDecimal quantity, Unit unit,
                                 LocalDate purchaseDate, LocalDate expirationDate, StorageType storageType,
                                 long version, ExpiryStatus status, long daysUntilExpiration) {
    public static IngredientResponse from(Ingredient ingredient, LocalDate today) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getCategory(),
            ingredient.getQuantity(), ingredient.getUnit(), ingredient.getPurchaseDate(), ingredient.getExpirationDate(),
            ingredient.getStorageType(), ingredient.getVersion(), ExpiryStatus.of(ingredient.getExpirationDate(), today),
            ChronoUnit.DAYS.between(today, ingredient.getExpirationDate()));
    }
}
