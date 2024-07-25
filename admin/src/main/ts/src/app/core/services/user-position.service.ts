import { Injectable } from "@angular/core";
import {
  UserPosition,
  UserPositionCreation,
  UserPositionElementQuery,
} from "src/app/core/store/models/userPosition.model";

import http from "axios";

@Injectable()
export class UserPositionServices {

  public async createUserPosition(userPositionCreation: UserPositionCreation): Promise<UserPosition> {
    return { id: "10", name: userPositionCreation.name, source: "MANUAL"};
    // return (
    //   await http.post<UserPosition>(
    //     `/directory/positions`,
    //     userPositionCreation
    //   )
    // ).data;
  }

  public async updateUserPosition(userPosition: UserPosition): Promise<UserPosition> {
    return userPosition;
    // const res = await http.post<UserPosition>(
    //   `/directory/positions/${userPosition.id}`,
    //   userPosition,
    // );
    // return res;
  }

  public async deleteUserPosition(id: string) {
    const res = await http.delete<UserPosition>(
      `/directory/positions/${id}`,
    );
    return res;
  }

  public async getUserPosition(id: string): Promise<UserPosition> {
    const userPosition = (
      await http.get<UserPosition>(`/directory/positions/${id}`)
    ).data;
    return userPosition;
  }

  public async searchUserPositions(
    params: UserPositionElementQuery
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
      }
    ];
    
  //   const userPositions: UserPosition[] =
  //   params.structureIds?.length || params.prefix?.length
  //     ? await http.get<UserPosition[]>("/directory/positions", {
  //         queryParams: { ...params },
  //       })
  //     : [];
  // return userPositions;
  }
}
