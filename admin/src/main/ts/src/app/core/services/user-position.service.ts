import { Injectable } from "@angular/core";
import {
  UserPosition,
  UserPositionCreation,
  UserPositionElementQuery,
} from "src/app/core/store/models/userPosition.model";

import http from "axios";
import { Session } from "../store/mappings/session";
import { SessionModel } from "../store/models/session.model";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";

type DirectoryPublicConf = {
  userPosition: {
    restrictCRUDToADMC: boolean;
  }
}

@Injectable()
export class UserPositionService {
  private positionsURL = "/directory/positions";
  private _restrictCrud;
  private $restrictCrud = new Observable<boolean>( subscriber => {
    Promise
    .resolve(typeof this._restrictCrud === "undefined")
    .then( async needChecking => needChecking ? await this.checkRestricted() : this._restrictCrud)
    .then( isRestricted => {
      subscriber.next(isRestricted);
      subscriber.complete();
    })
    .catch( e => subscriber.error(e));
  });

  public async createUserPosition(userPositionCreation: UserPositionCreation) {
    return (
      await http.post<UserPosition>(
        this.positionsURL,
        userPositionCreation
      )
    ).data;
  }

  public async updateUserPosition(userPosition: UserPosition) {
    const res = await http.put<UserPosition>(
      `${this.positionsURL}/${userPosition.id}`,
      userPosition,
    );
    return res.data;
  }

  public async deleteUserPosition(id: string) {
    const res = await http.delete<void>(`${this.positionsURL}/${id}`);
    return res.data;
  }

  public async isCrudRestricted(): Promise<boolean> {
    return this.$restrictCrud.toPromise();
  }

  /**
   * Non-AMDC users may be restricted in usage.
   */
  private async checkRestricted(): Promise<boolean> {
    const session:Session = await SessionModel.getSession();
    this._restrictCrud = !session.isADMC();
    if( this._restrictCrud ) {
      try {
        const conf = (await http.get<DirectoryPublicConf>(`/directory/conf/public?_=${new Date().getMilliseconds()}`)).data;
        this._restrictCrud = conf.userPosition.restrictCRUDToADMC;
      } catch(e) {
        console.log("Could not read parameter 'restrictCRUDToADMC' from /directory/conf/public");
        this._restrictCrud = false;
      }
    }
    return Promise.resolve(this._restrictCrud);
  }

  public isTabEnabled(): Observable<boolean> {
    return this.$restrictCrud.pipe( map(restricted => !restricted) );
  }

  /**
   * Get user positions depending on the query
   * @param params if provided, will be used to filter the user positions by structureId
   *               and/or filter with prefix
   *               if structureId is not provided, will return all user position from all the structures he is ADML of
   * @returns list of user positions
   */
  public async searchUserPositions(
    params?: UserPositionElementQuery
  ): Promise<UserPosition[]> {
    const userPositions: UserPosition[] = (await http.get<UserPosition[]>(
      this.positionsURL,
      {
        params: params? params : {},
      }
    )).data;
    return userPositions.map(position => {
      position.name = position.name.trim();
      return position;
    });
  }
}
