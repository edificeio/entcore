import type { EmploiDuTempsData } from '~/models';
import { MOCK_EMPLOI_DU_TEMPS } from '~/mocks/widgetsMockData';

export async function fetchEmploiDuTemps(): Promise<EmploiDuTempsData> {
  // TODO: GET /edt/entries
  return {
    date: 'Lundi 19 janvier',
    entries: MOCK_EMPLOI_DU_TEMPS,
    currentTimeIndex: 0,
  };
}
