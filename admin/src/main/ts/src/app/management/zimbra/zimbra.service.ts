import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {GroupModel} from '../../core/store/models/group.model';
import {RoleModel} from '../../core/store/models/role.model';

@Injectable({
  providedIn: 'root'
})

export class ZimbraService {

  constructor(private httpClient: HttpClient) {}

  getGroups(structureId: string): Observable<GroupModel[]> {
    return this.httpClient.get<GroupModel[]>(`/appregistry/groups/roles?structureId=${structureId}`);
  }
  getRoleId(): Observable<{role: RoleModel}> {
      return this.httpClient.get<{role: RoleModel}>('/zimbra/role');
  }
 givePermission(id: string, roleId: string): Observable<void> {
        return this.httpClient.put<void>(`/appregistry/authorize/group/${id}/role/${roleId}`, {});
  }
  deletePermission(id: string, roleId: string): Observable<void> {
        return this.httpClient.delete<void>(`/appregistry/authorize/group/${id}/role/${roleId}`);
  }
}

