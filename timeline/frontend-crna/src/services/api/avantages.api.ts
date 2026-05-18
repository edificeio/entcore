import type { ListWidgetItem } from '~/models';
import { MOCK_AVANTAGES } from '~/mocks/widgetsMockData';

export async function fetchAvantages(): Promise<ListWidgetItem[]> {
  // TODO: GET /avantages
  return MOCK_AVANTAGES;
}
