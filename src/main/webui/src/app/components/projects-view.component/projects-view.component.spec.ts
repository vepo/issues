import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';

import { createTranslocoTestingModule } from '../../core/testing/transloco-testing';
import { ProjectsViewComponent } from './projects-view.component';
import { of } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

describe('ProjectsViewComponent', () => {
  let component: ProjectsViewComponent;
  let fixture: ComponentFixture<ProjectsViewComponent>;
  let mockActivatedRoute: any;

  beforeEach(async () => {
    mockActivatedRoute = {
      data: of({
        projects: []
      })
    };
    await TestBed.configureTestingModule({
      imports: [
        ProjectsViewComponent,
        createTranslocoTestingModule(
          { project: { list: { title: 'Projetos', create: 'Novo projeto' } } },
          { project: { list: { title: 'Projects', create: 'New project' } } },
        ),
      ],
      providers: [
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProjectsViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should rerender project actions immediately when locale changes from Portuguese to English', async () => {
    expect(fixture.nativeElement.textContent).toContain('Projetos');
    expect(fixture.nativeElement.textContent).toContain('Novo projeto');

    TestBed.inject(TranslocoService).setActiveLang('en');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Projects');
    expect(fixture.nativeElement.textContent).toContain('New project');
    expect(fixture.nativeElement.textContent).not.toContain('Novo projeto');
  });
});
