import { plainTextLength, plainTextLengthValidator, optionalPlainTextLengthValidator } from './plain-text-length';
import { FormControl } from '@angular/forms';

describe('plainTextLength', () => {
  it('should strip tags when counting', () => {
    expect(plainTextLength('<b>hello</b>')).toBe(5);
    expect(plainTextLength('<p>hi</p>')).toBe(2);
  });

  it('should return 0 for blank', () => {
    expect(plainTextLength('')).toBe(0);
    expect(plainTextLength(null)).toBe(0);
  });
});

describe('plainTextLengthValidator', () => {
  it('should reject plain text below min', () => {
    const control = new FormControl('<p>hi</p>');
    expect(plainTextLengthValidator(5, 1200)(control)).toEqual(
      jasmine.objectContaining({ plainTextMinLength: jasmine.anything() }),
    );
  });

  it('should accept HTML whose plain text is within max', () => {
    const control = new FormControl('<p><b>hello world</b></p>');
    expect(plainTextLengthValidator(5, 1200)(control)).toBeNull();
  });
});

describe('optionalPlainTextLengthValidator', () => {
  it('should allow blank', () => {
    expect(optionalPlainTextLengthValidator(5, 1200)(new FormControl(''))).toBeNull();
  });
});
