import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { Category, CategoryService } from '../services/category.service';

export const categoriesResolver: ResolveFn<Category[]> = () => {
  return inject(CategoryService).findAll();
};
