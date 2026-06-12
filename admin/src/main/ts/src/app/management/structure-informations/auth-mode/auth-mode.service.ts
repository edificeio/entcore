import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { DefaultAuthConfig } from 'src/app/core/store/models/structure.model';

export type { AuthMode, AuthConfig, DefaultAuthConfig } from 'src/app/core/store/models/structure.model';

@Injectable({
  providedIn: 'root',
})
export class AuthModeService {
  constructor(private httpClient: HttpClient) {}

  getDefaultAuthConfig(structureId: string): Observable<DefaultAuthConfig> {
    return this.httpClient.get<DefaultAuthConfig>(`/directory/structure/${structureId}/default-auth-config`);
  }

  updateDefaultAuthConfig(structureId: string, config: DefaultAuthConfig): Observable<void> {
    return this.httpClient.put<void>(`/directory/structure/${structureId}/default-auth-config`, config);
  }
}
