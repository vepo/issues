import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

/** FQ3 B: 8–64 chars, uppercase, lowercase, digit */
export const STRONG_PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/;

export const strongPasswordValidators = [
  Validators.required,
  Validators.minLength(8),
  Validators.maxLength(64),
  Validators.pattern(STRONG_PASSWORD_PATTERN),
];

export function passwordsMatchValidator(passwordKey: string, confirmKey: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const password = group.get(passwordKey)?.value;
    const confirm = group.get(confirmKey)?.value;
    if (!password || !confirm) {
      return null;
    }
    return password === confirm ? null : { passwordMismatch: true };
  };
}
