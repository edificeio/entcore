import { odeServices } from '@edifice.io/client';

import {
  type Case,
  type CaseSearch,
  type CaseSearchPayload,
  type DecisionCreatedResponse,
  type DecisionPayload,
} from '~/models';

/**
 * Creates a moderation service with the specified base URL.
 *
 * @param baseURL The base URL of the moderation API.
 * @returns A service to interact with moderation cases.
 */
export const createModerationService = (baseURL: string) => ({
  /** Load a single case with its reports and decisions. */
  getCase(id: string) {
    return odeServices.http().get<Case>(`${baseURL}/cases/${id}`);
  },

  /** Search a paginated list of cases. */
  searchCases(payload: CaseSearchPayload, page = 0) {
    return odeServices
      .http()
      .post<CaseSearch>(`${baseURL}/cases/_search?page=${page}`, payload);
  },

  /** Record a moderation decision on a case. */
  addDecision(id: string, payload: DecisionPayload) {
    return odeServices
      .http()
      .post<DecisionCreatedResponse>(
        `${baseURL}/cases/${id}/decisions`,
        payload,
      );
  },
});
