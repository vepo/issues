import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { ContextBarComponent } from './context-bar.component';

describe('ContextBarComponent', () => {
  let fixture: ComponentFixture<ContextBarComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContextBarComponent],
      providers: [
        provideRouter([
          { path: '', component: ContextBarComponent },
          {
            path: 'project/:projectId/kanban',
            component: ContextBarComponent,
            data: { project: { id: 1, name: 'Alpha', prefix: 'A', description: '', workflow: '' } }
          },
          {
            path: 'ticket/:ticketIdentifier',
            component: ContextBarComponent,
            data: {
              ticket: {
                id: 10,
                identifier: 'A-1',
                title: 'Bug',
                project: { id: 1, name: 'Alpha' }
              }
            }
          }
        ])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContextBarComponent);
    router = TestBed.inject(Router);
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show kanban breadcrumb on project kanban route', async () => {
    await router.navigate(['/project/1/kanban']);
    fixture.detectChanges();

    const current = fixture.nativeElement.querySelector('.breadcrumb__current');
    expect(current?.textContent?.trim()).toBe('Kanban');
    expect(fixture.nativeElement.textContent).toContain('Alpha');
  });

  it('should show ticket identifier on ticket route', async () => {
    await router.navigate(['/ticket/A-1']);
    fixture.detectChanges();

    const current = fixture.nativeElement.querySelector('.breadcrumb__current');
    expect(current?.textContent?.trim()).toBe('#A-1');
    expect(fixture.nativeElement.textContent).toContain('Alpha');
  });

  it('should hide on home route', async () => {
    await router.navigate(['/']);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.context-bar')).toBeNull();
  });
});
