import { TranslocoPipe } from '@jsverse/transloco';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Workflow } from '../../services/workflow.service';

@Component({
  selector: 'app-workflows-view',
  imports: [TranslocoPipe, RouterLink, MatIconModule, MatButtonModule],
  templateUrl: './workflows-view.component.html'
})
export class WorkflowsViewComponent implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);

  workflows: Workflow[] = [];

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ workflows }) => this.workflows = workflows);
  }
}
