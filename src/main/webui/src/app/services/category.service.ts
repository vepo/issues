import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { CategoryApi } from '../generated/api/category.service';
import { CategoryResponse } from '../generated/model/categoryResponse';

import { asLoadedArray, Loaded } from '../core/required-types';

export type Category = Loaded<CategoryResponse>;

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private readonly api = inject(CategoryApi);

  findAll(): Observable<Category[]> {
    return this.api.listCategories().pipe(map(asLoadedArray));
  }
}
