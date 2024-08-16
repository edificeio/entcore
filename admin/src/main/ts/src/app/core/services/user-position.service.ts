import { Injectable } from "@angular/core";
import {
  UserPosition,
  UserPositionCreation,
  UserPositionElementQuery,
} from "src/app/core/store/models/userPosition.model";

import http from "axios";

@Injectable()
export class UserPositionServices {
  private positionsURL = "/directory/positions";

  public async createUserPosition(userPositionCreation: UserPositionCreation) {
    return (
      await http.post<UserPosition>(
        this.positionsURL,
        userPositionCreation
      )
    ).data;
  }

  public async updateUserPosition(userPosition: UserPosition) {
    const res = await http.post<UserPosition>(
      `${this.positionsURL}/${userPosition.id}`,
      userPosition,
    );
    return res.data;
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
