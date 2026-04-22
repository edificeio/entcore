import type { LastInfosProps } from '@edifice.io/react/homepage';
import { MOCK_LAST_INFOS } from '~/mocks/widgetsMockData';

export async function fetchLastInfos(): Promise<LastInfosProps[]> {
  // TODO: GET /timeline/lastinfos
  return MOCK_LAST_INFOS;
}
