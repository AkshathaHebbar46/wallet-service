package org.walletservice.wallet_service.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.walletservice.wallet_service.validation.validator.TransactionAmountValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = TransactionAmountValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTransactionAmount {
        String message() default "Transaction amount must be greater than zero";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
