import { Component, Input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import {
  ActivityItem,
  activityIcon,
  activitySummary,
  hasValueChange,
  trackActivityItem,
} from './activity-feed.utils';

@Component({
  selector: 'app-ticket-activity-feed',
  templateUrl: './ticket-activity-feed.component.html',
  imports: [DatePipe, MatIconModule],
})
export class TicketActivityFeedComponent {
  @Input({ required: true }) items: ActivityItem[] = [];

  protected readonly activityIcon = activityIcon;
  protected readonly activitySummary = activitySummary;
  protected readonly hasValueChange = hasValueChange;
  protected readonly trackActivityItem = trackActivityItem;

  protected expandedIds = new Set<string>();

  protected isExpanded(item: ActivityItem): boolean {
    return this.expandedIds.has(trackActivityItem(item));
  }

  protected toggleExpanded(item: ActivityItem): void {
    const key = trackActivityItem(item);
    if (this.expandedIds.has(key)) {
      this.expandedIds.delete(key);
    } else {
      this.expandedIds.add(key);
    }
  }

  protected shouldTruncate(value: string | undefined): boolean {
    return (value?.length ?? 0) > 200;
  }

  protected displayValue(value: string | undefined, item: ActivityItem): string {
    if (!value) {
      return '';
    }
    if (!this.shouldTruncate(value) || this.isExpanded(item)) {
      return value;
    }
    return `${value.slice(0, 200)}…`;
  }
}
