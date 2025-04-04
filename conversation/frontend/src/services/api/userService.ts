import { odeServices } from 'edifice-ts-client';
import { Visible } from '~/models/visible';

/**
 */
export const createUserService = () => ({
  /**
   * Fully load a message.
   * @returns
   */
  searchVisible(search: string) {
    return odeServices.http().get<Visible[]>(`/communication/visible/search`, {
      queryParams: { query: search },
    });
  },
});
