import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ServiceAccountApi } from '../generated/api/serviceAccount.service';
import { CreateServiceAccountRequest } from '../generated/model/createServiceAccountRequest';
import { CreateServiceAccountTokenRequest } from '../generated/model/createServiceAccountTokenRequest';
import { CreatedServiceAccountTokenResponse } from '../generated/model/createdServiceAccountTokenResponse';
import { ServiceAccountResponse } from '../generated/model/serviceAccountResponse';
import { asLoaded, asLoadedArray, Loaded } from '../core/required-types';

export type ServiceAccount = Loaded<ServiceAccountResponse>;
export type CreatedServiceAccountToken = Loaded<CreatedServiceAccountTokenResponse>;

@Injectable({ providedIn: 'root' })
export class ServiceAccountService {
  private readonly api = inject(ServiceAccountApi);

  list(projectId: number): Observable<ServiceAccount[]> {
    return this.api.listServiceAccounts(projectId).pipe(map(asLoadedArray));
  }

  create(projectId: number, name: string): Observable<ServiceAccount> {
    return this.api.createServiceAccount(projectId, { name } as CreateServiceAccountRequest).pipe(map(asLoaded));
  }

  deactivate(projectId: number, serviceAccountId: number): Observable<void> {
    return this.api.deactivateServiceAccount(projectId, serviceAccountId).pipe(map(() => undefined));
  }

  createToken(projectId: number, serviceAccountId: number, name: string): Observable<CreatedServiceAccountToken> {
    return this.api
      .createServiceAccountToken(projectId, serviceAccountId, { name } as CreateServiceAccountTokenRequest)
      .pipe(map(asLoaded));
  }

  revokeToken(projectId: number, serviceAccountId: number, tokenId: number): Observable<void> {
    return this.api.revokeServiceAccountToken(projectId, serviceAccountId, tokenId).pipe(map(() => undefined));
  }
}
