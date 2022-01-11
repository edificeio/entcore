import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {GroupModel} from '../../core/store/models/group.model';
import {RoleModel} from '../../core/store/models/role.model';
import {ReturnedMail, ReturnedMailStatut} from './ReturnedMail';

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
   * Get the list of all returned mails.
   * @param structureId Id for the current structure.
   */
  public getReturnedMails(structureId: string): Observable<ReturnedMail[]> {
    return this.httpClient.get<ReturnedMail[]>(`/zimbra/return/list?structureId=${structureId}`);
  }

  /**
   * Delete the selected returned mail.
   * @param id Id of the selected returnedMail.
   */
  public deleteReturnedMail(id: number) {
    return this.httpClient.delete<ReturnedMailStatut>(`/zimbra/return/delete/${id}`);
  }

  /**
   * Remove selected mails.
   * @param mailIds List of mails ids to be removed from mailbox.
   */
  public removeReturnedMails(mailIds: number[]): Observable<ReturnedMailStatut[]> {
    let params = '';
    mailIds.forEach(id => params += `id=${id}&`);
    return this.httpClient.delete<ReturnedMailStatut[]>(`/zimbra/delete/sent?${params}`);
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

