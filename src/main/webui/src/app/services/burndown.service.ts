import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { BurndownApi } from '../generated/api/burndown.service';
import { BurndownResponse } from '../generated/model/burndownResponse';
import { BurndownSeriesPoint } from '../generated/model/burndownSeriesPoint';
import { BurndownWarning } from '../generated/model/burndownWarning';
import { asLoaded, Loaded } from '../core/required-types';

export type Burndown = Loaded<BurndownResponse>;
export type BurndownPoint = Loaded<BurndownSeriesPoint>;
export type BurndownWarn = Loaded<BurndownWarning>;

@Injectable({
  providedIn: 'root'
})
export class BurndownService {
  private readonly api = inject(BurndownApi);

  load(projectId: number, phaseId: number): Observable<Burndown> {
    return this.api.loadProjectBurndown(projectId, phaseId).pipe(map(asLoaded));
  }
}
