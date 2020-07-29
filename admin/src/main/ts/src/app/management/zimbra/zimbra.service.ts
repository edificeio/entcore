import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {GroupModel} from '../../core/store/models/group.model';
import {RoleModel} from '../../core/store/models/role.model';
import {TraceResponse} from "../../types/trace";

@Injectable({
  providedIn: 'root'
})

export class ZimbraService {

  constructor(private httpClient: HttpClient) {}

    /**
     * Get the list of all groups by structure.
     * @param structureId Id for the current structure.
     */
  public getGroups(structureId: string): Observable<GroupModel[]> {
    return this.httpClient.get<GroupModel[]>(`/appregistry/groups/roles?structureId=${structureId}`);
  }
    /**
     * Get the role id of the zimbra component.
     */
  public getRoleId(): Observable<{role: RoleModel}> {
      return this.httpClient.get<{role: RoleModel}>('/zimbra/role');
  }

    /**
     * Give the permission to use zimbra outside communication.
     * @param id Group id.
     * @param roleId Zimbra role id.
     */
  public givePermission(id: string, roleId: string): Observable<void> {
        return this.httpClient.put<void>(`/appregistry/authorize/group/${id}/role/${roleId}`, {});
  }

    /**
     *
     * @param id Group id.
     * @param roleId Zimbra role id.
     */
  public deletePermission(id: string, roleId: string): Observable<void> {
        return this.httpClient.delete<void>(`/appregistry/authorize/group/${id}/role/${roleId}`);
  }

    /**
     * Get the value of the configuration key "displayZimbraAdmin"
     */
  public getZimbraConfKey(): Observable<{ displayZimbra: boolean }> {
        return this.httpClient.get<{ displayZimbra: boolean }>(`/admin/api/config/zimbra`);
  }
}

