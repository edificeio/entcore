import { useQuery } from '@tanstack/react-query';
import type { EmploiDuTempsEntry } from '~/components/EmploiDuTempsWidget';
import type { ListWidgetItem } from '~/components/ListWidget';
import type { VieScolaireChild } from '~/components/VieScolaireWidget';
import {
  MOCK_AVANTAGES,
  MOCK_EMPLOI_DU_TEMPS,
  MOCK_LIENS_UTILES,
  MOCK_MEDIACENTRE,
  MOCK_MES_EMPRUNTS,
  MOCK_VIE_SCOLAIRE,
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

export function useVieScolaire() {
  return useQuery<VieScolaireChild[]>({
    queryKey: ['vie-scolaire'],
    queryFn: async () => MOCK_VIE_SCOLAIRE,
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
    queryFn: async () => MOCK_MEDIACENTRE,
  });
}
