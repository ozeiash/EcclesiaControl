package br.com.bitabit.ecclesiacontrol.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** CNPJ opcional: nulo ou vazio passa; preenchido precisa ter DV válido (aceita com ou sem máscara). */
@Documented
@Constraint(validatedBy = ValidCnpj.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCnpj {

    String message() default "CNPJ inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidCnpj, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext ctx) {
            String n = Cnpj.normalize(value);
            return n == null || Cnpj.isValid(n);
        }
    }
}
