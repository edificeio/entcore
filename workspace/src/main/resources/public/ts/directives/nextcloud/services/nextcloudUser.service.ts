import http, { AxiosResponse } from "axios";
import { ng } from "entcore";
import { UserNextcloud } from "../models/nextcloudUser.model";

export interface INextcloudUserService {
  resolveUser(userid: string): Promise<AxiosResponse>;

  getUserInfo(userid: string): Promise<UserNextcloud>;
}

export const nextcloudUserService: INextcloudUserService = {
  resolveUser: async (userid: string): Promise<AxiosResponse> => {
    return http.get(decodeURI(`/nextcloud/user/${userid}/provide/token`));
  },

  getUserInfo: async (userid: string): Promise<UserNextcloud> => {
    return http
      .get(`/nextcloud/user/${userid}`)
      .then((response: AxiosResponse) =>
        new UserNextcloud().build(response.data),
      );
  },
};

export const NextcloudUserService = ng.service(
  "NextcloudUserService",
  (): INextcloudUserService => nextcloudUserService,
);
