import {
  Component,
  Input,
  Output,
  EventEmitter,
  ViewChild,
  ElementRef,
  AfterViewInit,
  OnChanges,
  SimpleChanges,
  forwardRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-rich-text-editor',
  templateUrl: './rich-text-editor.component.html',
  styleUrls: ['./rich-text-editor.component.scss'],
  standalone: true,
  imports: [CommonModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RichTextEditorComponent),
      multi: true,
    },
  ],
})
export class RichTextEditorComponent implements AfterViewInit, OnChanges, ControlValueAccessor {
  @Input() placeholder = 'Digite seu texto...';
  @Input() value = '';
  @Input() disabled = false;
  @Output() valueChange = new EventEmitter<string>();

  @ViewChild('editor') editorRef!: ElementRef<HTMLDivElement>;

  isBold = false;
  isItalic = false;
  isUnderline = false;
  isList = false;

  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;
  private pendingValue: string | null = null;

  ngAfterViewInit(): void {
    this.setupEditor();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.editorRef) {
      return;
    }
    if (changes['value']) {
      const next = changes['value'].currentValue ?? '';
      if (this.editorRef.nativeElement.innerHTML !== next) {
        this.editorRef.nativeElement.innerHTML = next;
      }
    }
    if (changes['disabled']) {
      this.editorRef.nativeElement.contentEditable = this.disabled ? 'false' : 'true';
    }
  }

  writeValue(value: string | null): void {
    const next = value ?? '';
    this.value = next;
    if (this.editorRef) {
      if (this.editorRef.nativeElement.innerHTML !== next) {
        this.editorRef.nativeElement.innerHTML = next;
      }
    } else {
      this.pendingValue = next;
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.editorRef) {
      this.editorRef.nativeElement.contentEditable = isDisabled ? 'false' : 'true';
    }
  }

  setupEditor(): void {
    if (!this.editorRef) {
      return;
    }
    const initial = this.pendingValue ?? this.value;
    this.pendingValue = null;
    this.editorRef.nativeElement.innerHTML = initial;
    this.editorRef.nativeElement.contentEditable = this.disabled ? 'false' : 'true';
    this.editorRef.nativeElement.addEventListener('input', () => this.emitChange());
    this.editorRef.nativeElement.addEventListener('blur', () => this.onTouched());
  }

  formatText(command: string, value = ''): void {
    if (this.disabled) {
      return;
    }
    document.execCommand(command, false, value);
    this.editorRef.nativeElement.focus();
    this.updateToolbarState();
    this.emitChange();
  }

  updateToolbarState(): void {
    this.isBold = document.queryCommandState('bold');
    this.isItalic = document.queryCommandState('italic');
    this.isUnderline = document.queryCommandState('underline');
  }

  emitChange(): void {
    if (!this.editorRef?.nativeElement) {
      return;
    }
    this.value = this.editorRef.nativeElement.innerHTML;
    this.valueChange.emit(this.value);
    this.onChange(this.value);
  }

  insertLink(): void {
    const url = prompt('Digite a URL:');
    if (url) {
      this.formatText('createLink', url);
    }
  }

  insertList(): void {
    this.formatText('insertUnorderedList');
    this.isList = !this.isList;
  }

  clearFormatting(): void {
    this.formatText('removeFormat');
    this.updateToolbarState();
  }

  getPlainText(): string {
    return this.editorRef?.nativeElement?.innerText ?? '';
  }

  getHtml(): string {
    return this.editorRef?.nativeElement?.innerHTML ?? '';
  }
}
