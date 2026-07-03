import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, TemplateRef } from '@angular/core';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="toast"
      [class]="'toast-' + type"
      [class.show]="isVisible"
      [class.hide]="!isVisible"
      [attr.role]="'alert'"
      [attr.aria-live]="'assertive'"
      [attr.aria-atomic]="'true'">

      <div class="toast-content">
        <div class="toast-icon">
          <span *ngIf="type === 'success'">✓</span>
          <span *ngIf="type === 'error'">✗</span>
          <span *ngIf="type === 'warning'">⚠</span>
          <span *ngIf="type === 'info'">ℹ</span>
        </div>

        <div class="toast-message">
          <ng-container *ngIf="template; else defaultTemplate">
            <ng-container *ngTemplateOutlet="template; context: context"></ng-container>
          </ng-container>
          <ng-template #defaultTemplate>
            {{ message }}
          </ng-template>
        </div>

        <button
          *ngIf="closeable"
          class="toast-close"
          (click)="onClose()"
          aria-label="Fechar">
          ×
        </button>
      </div>
    </div>
  `
})
export class ToastComponent {
  @Input() message: string = '';
  @Input() type: 'success' | 'error' | 'warning' | 'info' = 'info';
  @Input() position: string = 'top-right';
  @Input() closeable: boolean = true;
  @Input() template?: TemplateRef<any>;
  @Input() context?: any;

  @Output() close = new EventEmitter<void>();

  isVisible: boolean = true;

  onClose(): void {
    this.close.emit();
  }
}
