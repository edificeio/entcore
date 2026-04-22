export type EmploiDuTempsColor = 'green' | 'pink' | 'orange' | 'blue' | 'grey';

export interface EmploiDuTempsEntry {
  id: string;
  subject: string;
  room?: string;
  teacher?: string;
  startTime: string;
  color?: EmploiDuTempsColor;
}

export interface EmploiDuTempsData {
  date: string;
  entries: EmploiDuTempsEntry[];
  currentTimeIndex: number;
}
