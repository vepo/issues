import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QueryLanguageHelpComponent } from './query-language-help.component';
import { QUERY_LANGUAGE_EXAMPLES, QUERY_LANGUAGE_FIELDS } from './query-language-reference';

describe('QueryLanguageHelpComponent', () => {
  let fixture: ComponentFixture<QueryLanguageHelpComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QueryLanguageHelpComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(QueryLanguageHelpComponent);
    fixture.detectChanges();
  });

  it('should render field reference rows', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('.data-table--cols-query-help .body .row');
    expect(rows.length).toBe(QUERY_LANGUAGE_FIELDS.length);
  });

  it('should render example queries', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const examples = compiled.querySelectorAll('.query-language-help__example-code');
    expect(examples.length).toBe(QUERY_LANGUAGE_EXAMPLES.length);
  });
});
