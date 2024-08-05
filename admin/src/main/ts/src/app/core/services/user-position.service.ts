import { Injectable } from "@angular/core";
import {
  UserPosition,
  UserPositionCreation,
  UserPositionElementQuery,
  UserPositionSource,
} from "src/app/core/store/models/userPosition.model";

import http from "axios";

@Injectable()
export class UserPositionServices {
  private positionsURL = "/directory/positions";

  public async createUserPosition(
    userPositionCreation: UserPositionCreation
  ): Promise<UserPosition> {
    const result = {
      id: userPositionCreation.name,
      name: userPositionCreation.name,
      source: "MANUAL" as UserPositionSource,
    };
    console.log(JSON.stringify(result));
    return Promise.resolve(result);
    // return (
    //   await http.post<UserPosition>(
    //     this.positionsURL,
    //     userPositionCreation
    //   )
    // ).data;
  }

  public async updateUserPosition(
    userPosition: UserPosition
  ): Promise<UserPosition> {
    return Promise.resolve({...userPosition});
    // const res = await http.post<UserPosition>(
    //   `${this.positionsURL}/${userPosition.id}`,
    //   userPosition,
    // );
    // return res;
  }

  public async deleteUserPosition(id: string, structureId: string) {
    const res = await http.delete<UserPosition>(
      `${this.positionsURL}/${id}?structureId=${structureId}`
    );
    return res;
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
    return [
      {
        id: "1",
        name: "name",
        source: "MANUAL",
      },
      {
        id: "2",
        name: "name 2",
        source: "AAF",
      },
      {
        id: "3",
        name: "name 3",
        source: "CSV",
      },
    ];

    // const userPositions: UserPosition[] = (await http.get<UserPosition[]>(
    //   this.positionsURL,
    //   {
    //     params: params? params : {},
    //   }
    // )).data;
    // return userPositions;
  }
}
