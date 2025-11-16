package org.walletservice.wallet_service.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.walletservice.wallet_service.validation.annotation.ValidTransactionAmount;

public class TransactionAmountValidator implements ConstraintValidator<ValidTransactionAmount, Double> {

    private static final double MAX_AMOUNT = 5_00_000;

    @Override
    public boolean isValid(Double amount, ConstraintValidatorContext context) {
        if (amount == null) return true; // Null-check handled elsewhere if required

        // Minimum amount enforcement
        if (amount < 1) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Transaction amount must be at least 1")
                    .addConstraintViolation();
            return false;
        }

        // Maximum amount check
        if (amount > MAX_AMOUNT) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Transaction amount cannot exceed " + MAX_AMOUNT
            ).addConstraintViolation();
            return false;
        }

        return true; // Valid amount
    }
}
