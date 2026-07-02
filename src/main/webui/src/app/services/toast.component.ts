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
  `,
  styles: [`
    .toast {
      min-width: 250px;
      max-width: 400px;
      margin-bottom: 12px;
      padding: 16px;
      border-radius: 0;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      display: flex;
      align-items: center;
      justify-content: space-between;
      transition: opacity 0.2s ease;
      opacity: 0;
      pointer-events: none;
    }

    .toast.show {
      opacity: 1;
      pointer-events: all;
    }

    .toast.hide {
      opacity: 0;
      pointer-events: none;
    }

    .toast-success {
      background-color: #E8F5E9;
      color: #1B5E20;
      border-left: 4px solid #2E7D32;
    }

    .toast-error {
      background-color: #FFEBEE;
      color: #B71C1C;
      border-left: 4px solid #C62828;
    }

    .toast-warning {
      background-color: #FFF8E1;
      color: #E65100;
      border-left: 4px solid #F9A825;
    }

    .toast-info {
      background-color: #E3F2FD;
      color: #0D47A1;
      border-left: 4px solid #1565C0;
    }

    .toast-content {
      display: flex;
      align-items: center;
      width: 100%;
    }

    .toast-icon {
      margin-right: 12px;
      font-weight: bold;
      font-size: 18px;
    }

    .toast-message {
      flex: 1;
      font-size: 14px;
      line-height: 1.5;
    }

    .toast-close {
      background: none;
      border: none;
      font-size: 20px;
      cursor: pointer;
      padding: 0 0 0 12px;
      line-height: 1;
      opacity: 0.7;
      transition: opacity 0.2s;
    }

    .toast-close:hover {
      opacity: 1;
    }

    .toast-top-right {
      top: 20px;
      right: 20px;
    }

    .toast-top-left {
      top: 20px;
      left: 20px;
    }

    .toast-bottom-right {
      bottom: 20px;
      right: 20px;
    }

    .toast-bottom-left {
      bottom: 20px;
      left: 20px;
    }

    .toast-top-center {
      top: 20px;
      left: 50%;
      transform: translateX(-50%);
    }

    .toast-bottom-center {
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
    }
  `]
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