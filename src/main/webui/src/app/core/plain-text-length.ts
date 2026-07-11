import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Strip HTML tags and return plain-text character count (matches server PlainTextLength). */
export function plainTextLength(htmlOrText: string | null | undefined): number {
  if (htmlOrText == null || htmlOrText.trim() === '') {
    return 0;
  }
  return htmlOrText.replace(/<[^>]+>/g, '').length;
}

export function plainTextLengthValidator(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const length = plainTextLength(control.value as string);
    if (length < min) {
      return { plainTextMinLength: { requiredLength: min, actualLength: length } };
    }
    if (length > max) {
      return { plainTextMaxLength: { requiredLength: max, actualLength: length } };
    }
    return null;
  };
}

/** Allows blank; when non-blank, enforces min–max plain-text length. */
export function optionalPlainTextLengthValidator(min: number, max: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const length = plainTextLength(control.value as string);
    if (length === 0) {
      return null;
    }
    return plainTextLengthValidator(min, max)(control);
  };
}
