import type { ListWidgetItem } from '~/models';
import { MOCK_LIENS_UTILES } from '~/mocks/widgetsMockData';

export async function fetchLiensUtiles(): Promise<ListWidgetItem[]> {
  // TODO: GET /liens-utiles
  return MOCK_LIENS_UTILES;
}
