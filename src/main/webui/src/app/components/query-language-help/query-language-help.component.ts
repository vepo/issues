import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { QUERY_LANGUAGE_EXAMPLES, QUERY_LANGUAGE_FIELDS } from './query-language-reference';

@Component({
  selector: 'app-query-language-help',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './query-language-help.component.html',
  styleUrl: './query-language-help.component.scss'
})
export class QueryLanguageHelpComponent {
  readonly fields = QUERY_LANGUAGE_FIELDS;
  readonly examples = QUERY_LANGUAGE_EXAMPLES;
}
