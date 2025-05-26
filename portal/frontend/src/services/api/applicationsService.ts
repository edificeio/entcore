import { odeServices } from '@edifice.io/client';
import { Application, ApplicationsResponse } from '~/models/application';

export const createApplicationsService = () => ({
  async getApplications() {
    const res = await odeServices
      .http()
      .get<ApplicationsResponse>('applications-list');
    return res;
  },
  async getApplicationConfig() {
    const response = await odeServices
      .http()
      .get<Application[]>('myApps/config');
    return response || [];
  },
});
