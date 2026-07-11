import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { AgentApi } from '../generated/api/agent.service';
import { ApiTokenApi } from '../generated/api/apiToken.service';
import { AgentSetupConfigResponse } from '../generated/model/agentSetupConfigResponse';
import { ApiTokenResponse } from '../generated/model/apiTokenResponse';
import { CreateApiTokenRequest } from '../generated/model/createApiTokenRequest';
import { CreatedApiTokenResponse } from '../generated/model/createdApiTokenResponse';
import { asLoaded, asLoadedArray, Loaded } from '../core/required-types';

export type ApiToken = Loaded<ApiTokenResponse>;
export type CreatedApiToken = Loaded<CreatedApiTokenResponse>;
export type AgentSetupConfig = Loaded<AgentSetupConfigResponse>;

@Injectable({ providedIn: 'root' })
export class ApiTokenService {
  private readonly apiTokenApi = inject(ApiTokenApi);
  private readonly agentApi = inject(AgentApi);

  list(): Observable<ApiToken[]> {
    return this.apiTokenApi.listApiTokens().pipe(map(asLoadedArray));
  }

  create(name: string): Observable<CreatedApiToken> {
    return this.apiTokenApi.createApiToken({ name } as CreateApiTokenRequest).pipe(map(asLoaded));
  }

  revoke(id: number): Observable<void> {
    return this.apiTokenApi.revokeApiToken(id).pipe(map(() => undefined));
  }

  getAgentSetupConfig(preset = 'cursor'): Observable<AgentSetupConfig> {
    return this.agentApi.getAgentSetupConfig(preset).pipe(map(asLoaded));
  }
}
