import type { ListWidgetItem } from '~/models';
import { MOCK_MES_EMPRUNTS } from '~/mocks/widgetsMockData';

export async function fetchMesEmprunts(): Promise<ListWidgetItem[]> {
  // TODO: GET /mediacentre/emprunts
  return MOCK_MES_EMPRUNTS;
}
