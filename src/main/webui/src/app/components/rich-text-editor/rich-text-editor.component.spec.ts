import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { RichTextEditorComponent } from './rich-text-editor.component';

describe('RichTextEditorComponent', () => {
  let component: RichTextEditorComponent;
  let fixture: ComponentFixture<RichTextEditorComponent>;
  let editorElement: HTMLDivElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormsModule, RichTextEditorComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(RichTextEditorComponent);
    component = fixture.componentInstance;
    editorElement = fixture.debugElement.query(By.css('.editor-area')).nativeElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Input Properties', () => {
    it('should set placeholder correctly', () => {
      const testPlaceholder = 'Enter your text here...';
      component.placeholder = testPlaceholder;
      fixture.detectChanges();
      
      expect(editorElement.getAttribute('placeholder')).toBe(testPlaceholder);
    });

    it('should set initial value correctly', () => {
      const testValue = '<p>Initial content</p>';
      fixture.componentRef.setInput('value', testValue);
      fixture.detectChanges();
      
      expect(editorElement.innerHTML).toBe(testValue);
    });

    it('should set disabled state correctly', () => {
      component.disabled = true;
      fixture.detectChanges();
      
      expect(editorElement.classList.contains('disabled')).toBe(true);
      expect(editorElement.getAttribute('contenteditable')).toBe('false');
    });
  });

  describe('Editor Functionality', () => {
    beforeEach(() => {
      component.ngAfterViewInit();
      fixture.detectChanges();
    });

    it('should emit value change on input', fakeAsync(() => {
      const emitSpy = spyOn(component.valueChange, 'emit');
      const testContent = '<p>Test content</p>';
      
      editorElement.innerHTML = testContent;
      editorElement.dispatchEvent(new Event('input'));
      tick();
      
      expect(emitSpy).toHaveBeenCalledWith(testContent);
      expect(component.value).toBe(testContent);
    }));

    it('should format text with bold command', () => {
      const execCommandSpy = spyOn(document, 'execCommand');
      const focusSpy = spyOn(editorElement, 'focus');
      
      component.formatText('bold');
      
      expect(execCommandSpy).toHaveBeenCalledWith('bold', false, '');
      expect(focusSpy).toHaveBeenCalled();
    });

    it('should format text with custom value', () => {
      const execCommandSpy = spyOn(document, 'execCommand');
      
      component.formatText('createLink', 'https://example.com');
      
      expect(execCommandSpy).toHaveBeenCalledWith('createLink', false, 'https://example.com');
    });

    it('should update toolbar state after formatting', () => {
      spyOn(document, 'queryCommandState').and.returnValue(true);
      
      component.updateToolbarState();
      
      expect(component.isBold).toBe(true);
      expect(component.isItalic).toBe(true);
      expect(component.isUnderline).toBe(true);
    });

    it('should insert link with valid URL', () => {
      const promptSpy = spyOn(window, 'prompt').and.returnValue('https://example.com');
      const formatTextSpy = spyOn(component, 'formatText');
      
      component.insertLink();
      
      expect(promptSpy).toHaveBeenCalledWith('Digite a URL:');
      expect(formatTextSpy).toHaveBeenCalledWith('createLink', 'https://example.com');
    });

    it('should not insert link with empty URL', () => {
      const promptSpy = spyOn(window, 'prompt').and.returnValue('');
      const formatTextSpy = spyOn(component, 'formatText');
      
      component.insertLink();
      
      expect(promptSpy).toHaveBeenCalled();
      expect(formatTextSpy).not.toHaveBeenCalled();
    });

    it('should insert unordered list', () => {
      const formatTextSpy = spyOn(component, 'formatText');
      
      component.insertList();
      
      expect(formatTextSpy).toHaveBeenCalledWith('insertUnorderedList');
      expect(component.isList).toBe(true);
    });

    it('should clear formatting', () => {
      const formatTextSpy = spyOn(component, 'formatText');
      const updateToolbarSpy = spyOn(component, 'updateToolbarState');
      
      component.clearFormatting();
      
      expect(formatTextSpy).toHaveBeenCalledWith('removeFormat');
      expect(updateToolbarSpy).toHaveBeenCalled();
    });

    it('should get plain text content', () => {
      editorElement.innerText = 'Test plain text';
      
      const result = component.getPlainText();
      
      expect(result).toBe('Test plain text');
    });

    it('should get HTML content', () => {
      editorElement.innerHTML = '<p>Test HTML</p>';
      
      const result = component.getHtml();
      
      expect(result).toBe('<p>Test HTML</p>');
    });
  });

  describe('Lifecycle Hooks', () => {
    it('should setup editor on ngAfterViewInit', () => {
      const setupSpy = spyOn(component, 'setupEditor');
      
      component.ngAfterViewInit();
      
      expect(setupSpy).toHaveBeenCalled();
    });

    it('should handle value changes in ngOnChanges', () => {
      const changes = {
        value: {
          currentValue: '',
          previousValue: '<p>Previous content</p>',
          firstChange: false,
          isFirstChange: () => false
        }
      };
      
      component.value = '<p>Previous content</p>';
      component.ngAfterViewInit();
      fixture.detectChanges();
      
      component.ngOnChanges(changes as any);
      
      expect(editorElement.innerHTML).toBe('');
    });

    it('should not clear editor when value changes to same content', () => {
      const changes = {
        value: {
          currentValue: '<p>Same content</p>',
          previousValue: '<p>Same content</p>',
          firstChange: false,
          isFirstChange: () => false
        }
      };
      
      component.value = '<p>Same content</p>';
      component.ngAfterViewInit();
      fixture.detectChanges();
      
      const initialHtml = editorElement.innerHTML;
      component.ngOnChanges(changes as any);
      
      expect(editorElement.innerHTML).toBe(initialHtml);
    });
  });

  describe('UI Interactions', () => {
    it('should call formatText when toolbar buttons are clicked', () => {
      const formatTextSpy = spyOn(component, 'formatText');
      const boldButton = fixture.debugElement.query(By.css('.toolbar-btn:first-child'));
      
      boldButton.triggerEventHandler('click', null);
      
      expect(formatTextSpy).toHaveBeenCalledWith('bold');
    });

    it('should call insertLink when link button is clicked', () => {
      const insertLinkSpy = spyOn(component, 'insertLink');
      const linkButton = fixture.debugElement.queryAll(By.css('.toolbar-btn'))[3];
      
      linkButton.triggerEventHandler('click', null);
      
      expect(insertLinkSpy).toHaveBeenCalled();
    });

    it('should apply active class based on toolbar state', () => {
      component.isBold = true;
      component.isItalic = false;
      fixture.detectChanges();
      
      const buttons = fixture.debugElement.queryAll(By.css('.toolbar-btn'));
      const boldButton = buttons[0];
      const italicButton = buttons[1];
      
      expect(boldButton.nativeElement.classList.contains('active')).toBe(true);
      expect(italicButton.nativeElement.classList.contains('active')).toBe(false);
    });
  });

  describe('Edge Cases', () => {
    it('should handle null editorRef gracefully', () => {
      component.editorRef = null as any;
      
      expect(() => component.setupEditor()).not.toThrow();
      expect(() => component.getPlainText()).not.toThrow();
      expect(() => component.getHtml()).not.toThrow();
    });

    it('should handle empty content gracefully', () => {
      editorElement.innerHTML = '';
      
      expect(component.getPlainText()).toBe('');
      expect(component.getHtml()).toBe('');
    });
  });

  describe('ControlValueAccessor', () => {
    it('should write value into the editor', () => {
      component.writeValue('<p>From form</p>');
      fixture.detectChanges();
      expect(editorElement.innerHTML).toBe('<p>From form</p>');
    });

    it('should notify onChange when content is edited', fakeAsync(() => {
      const onChange = jasmine.createSpy('onChange');
      component.registerOnChange(onChange);
      editorElement.innerHTML = '<p>Typed</p>';
      editorElement.dispatchEvent(new Event('input'));
      tick();
      expect(onChange).toHaveBeenCalledWith('<p>Typed</p>');
    }));
  });

});
