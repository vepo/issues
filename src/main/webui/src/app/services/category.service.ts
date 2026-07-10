import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { CategoryApi } from '../generated/api/category.service';
import { CategoryResponse } from '../generated/model/categoryResponse';
import { CreateCategoryRequest } from '../generated/model/createCategoryRequest';
import { UpdateCategoryRequest } from '../generated/model/updateCategoryRequest';

import { asLoaded, asLoadedArray, Loaded } from '../core/required-types';

export type Category = Loaded<CategoryResponse>;
export type { CreateCategoryRequest, UpdateCategoryRequest };

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private readonly api = inject(CategoryApi);

  findAll(): Observable<Category[]> {
    return this.api.listCategories().pipe(map(asLoadedArray));
  }

  create(request: CreateCategoryRequest): Observable<Category> {
    return this.api.createCategory(request).pipe(map(asLoaded));
  }

  update(categoryId: number, request: UpdateCategoryRequest): Observable<Category> {
    return this.api.updateCategory(categoryId, request).pipe(map(asLoaded));
  }

  delete(categoryId: number): Observable<void> {
    return this.api.deleteCategory(categoryId);
  }
}
