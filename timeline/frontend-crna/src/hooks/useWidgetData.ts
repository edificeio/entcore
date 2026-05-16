import { useQuery } from '@tanstack/react-query';
import type { EmploiDuTempsEntry } from '~/components/EmploiDuTempsWidget';
import type { ListWidgetItem } from '~/components/ListWidget';
import type { LastInfosProps } from '@edifice.io/react/homepage';
import {
  MOCK_AVANTAGES,
  MOCK_EMPLOI_DU_TEMPS,
  MOCK_LAST_INFOS,
  MOCK_LIENS_UTILES,
  MOCK_MEDIACENTRE,
  MOCK_MES_EMPRUNTS,
} from '~/mocks/widgetsMockData';

export interface EmploiDuTempsData {
  date: string;
  entries: EmploiDuTempsEntry[];
  currentTimeIndex: number;
}

export function useEmploiDuTemps() {
  return useQuery<EmploiDuTempsData>({
    queryKey: ['emploi-du-temps'],
    queryFn: async (): Promise<EmploiDuTempsData> => ({
      date: 'Lundi 19 janvier',
      entries: MOCK_EMPLOI_DU_TEMPS,
      currentTimeIndex: 0,
    }),
  });
}


export function useMesEmprunts() {
  return useQuery<ListWidgetItem[]>({
    queryKey: ['mes-emprunts'],
    queryFn: async () => MOCK_MES_EMPRUNTS,
  });
}

export function useLiensUtiles() {
  return useQuery<ListWidgetItem[]>({
    queryKey: ['liens-utiles'],
    queryFn: async () => MOCK_LIENS_UTILES,
  });
}

export function useAvantages() {
  return useQuery<ListWidgetItem[]>({
    queryKey: ['avantages'],
    queryFn: async () => MOCK_AVANTAGES,
  });
}

export function useMediacentre() {
  return useQuery<ListWidgetItem[]>({
    queryKey: ['mediacentre'],
    queryFn: async () => [],
  });
}

export function useLastInfos() {
  return useQuery<LastInfosProps[]>({
    queryKey: ['last-infos'],
    queryFn: async () => MOCK_LAST_INFOS,
  });
}
