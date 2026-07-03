import { Component, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-context-hint',
  standalone: true,
  imports: [MatButtonModule],
  templateUrl: './context-hint.component.html'
})
export class ContextHintComponent implements OnInit {
  @Input({ required: true }) hintId!: string;
  @Input({ required: true }) message!: string;

  visible = true;

  ngOnInit(): void {
    if (localStorage.getItem(this.storageKey()) === 'true') {
      this.visible = false;
    }
  }

  dismiss(): void {
    localStorage.setItem(this.storageKey(), 'true');
    this.visible = false;
  }

  private storageKey(): string {
    return `issues.hint.dismissed.${this.hintId}`;
  }
}
