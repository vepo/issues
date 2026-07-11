package dev.vepo.issues.infra;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PlainTextSizeValidator implements ConstraintValidator<PlainTextSize, String> {

    private int min;
    private int max;

    @Override
    public void initialize(PlainTextSize constraintAnnotation) {
        min = constraintAnnotation.min();
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        var length = PlainTextLength.of(value);
        return length >= min && length <= max;
    }
}
