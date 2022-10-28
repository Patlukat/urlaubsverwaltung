package org.synyx.urlaubsverwaltung.sicknote.comment;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.springframework.util.StringUtils.hasText;

/**
 * Class for validating {@link SickNoteCommentForm} object.
 */
@Component
public class SickNoteCommentFormValidator implements Validator {

    private static final String ERROR_MANDATORY_FIELD = "error.entry.mandatory";
    private static final String ERROR_LENGTH = "error.entry.tooManyChars";
    private static final String ATTRIBUTE_COMMENT = "text";

    private static final int MAX_CHARS = 200;

    @Override
    public boolean supports(Class<?> clazz) {
        return SickNoteCommentForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        final SickNoteCommentForm sickNoteCommentForm = (SickNoteCommentForm) target;
        validateComment(sickNoteCommentForm, errors);
    }

    public void validateComment(SickNoteCommentForm comment, Errors errors) {

        final String text = comment.getText();

        if (hasText(text)) {
            if (text.length() > MAX_CHARS) {
                errors.rejectValue(ATTRIBUTE_COMMENT, ERROR_LENGTH);
            }
        } else {
            errors.rejectValue(ATTRIBUTE_COMMENT, ERROR_MANDATORY_FIELD);
        }
    }
}
