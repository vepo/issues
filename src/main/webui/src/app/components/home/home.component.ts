import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HomeActivity, HomeService, HomeTicket } from '../../services/home.service';
import { HomeSavedQuerySection } from '../../services/saved-query.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
  imports: [CommonModule, RouterModule, DatePipe, MatIconModule],
  standalone: true
})
export class HomeComponent implements OnInit {
  private readonly homeService = inject(HomeService);

  currentTickets: HomeTicket[] = [];
  assignedTickets: HomeTicket[] = [];
  activity: HomeActivity[] = [];
  savedQuerySections: HomeSavedQuerySection[] = [];

  ngOnInit(): void {
    forkJoin({
      current: this.homeService.listCurrentTickets(),
      assigned: this.homeService.listAssignedTickets(),
      activity: this.homeService.listActivity(),
      savedQueries: this.homeService.listSavedQuerySections()
    }).subscribe(({ current, assigned, activity, savedQueries }) => {
      this.currentTickets = current;
      this.assignedTickets = assigned;
      this.activity = activity;
      this.savedQuerySections = savedQueries.filter(section => section.tickets.length > 0);
    });
  }

  activityTrack(item: HomeActivity): string {
    return `${item.type}-${item.ticketId}-${item.occurredAt}`;
  }
}
