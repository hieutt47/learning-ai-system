package vn.edu.learningai.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import vn.edu.learningai.service.lesson.YouTubeUrlPolicy;

public class YouTubeUrlValidator implements ConstraintValidator<YouTubeUrl, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return YouTubeUrlPolicy.isValidOptional(value);
    }
}
