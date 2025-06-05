import { odeServices } from '@edifice.io/client';
import { ApplicationsResponse } from '~/models/application';

export const createApplicationsService = () => ({
    getApplications() {
      return odeServices
      .http()
      .get<ApplicationsResponse>('applications-list');
    }
})