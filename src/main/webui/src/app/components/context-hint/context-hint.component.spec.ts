import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ContextHintComponent } from './context-hint.component';

describe('ContextHintComponent', () => {
  let fixture: ComponentFixture<ContextHintComponent>;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [ContextHintComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ContextHintComponent);
    fixture.componentInstance.hintId = 'home';
    fixture.componentInstance.message = 'Test hint';
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should show hint when not dismissed', () => {
    const hint = fixture.nativeElement.querySelector('.context-hint');
    expect(hint).toBeTruthy();
    expect(hint.textContent).toContain('Test hint');
  });

  it('should hide hint and persist dismissal', () => {
    fixture.nativeElement.querySelector('.context-hint__close').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.context-hint')).toBeNull();
    expect(localStorage.getItem('issues.hint.dismissed.home')).toBe('true');

    fixture.destroy();
    const next = TestBed.createComponent(ContextHintComponent);
    next.componentInstance.hintId = 'home';
    next.componentInstance.message = 'Test hint';
    next.detectChanges();
    expect(next.nativeElement.querySelector('.context-hint')).toBeNull();
  });
});
