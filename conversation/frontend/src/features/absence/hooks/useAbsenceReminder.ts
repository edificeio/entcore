import { useState } from 'react';
import { AbsenceSettings } from '~/models/absence';
import { useAbsenceSettings } from '~/services/queries';

/**
 * An absence message is only ever "active" between its bounds while
 * enabled — deactivation and an elapsed `endAt` both make it inactive.
 */
export function isAbsenceActive(
  settings: AbsenceSettings | null | undefined,
  now: Date = new Date(),
): boolean {
  if (!settings?.enabled) return false;
  return now >= new Date(settings.startAt) && now <= new Date(settings.endAt);
}

/**
 * Drives the absence reminder banner: whether it should be shown, and the
 * open/close state of the settings modal opened from its "Edit" button.
 */
export function useAbsenceReminder() {
  const { data: settings } = useAbsenceSettings();
  const [isModalOpen, setIsModalOpen] = useState(false);

  return {
    isActive: isAbsenceActive(settings),
    endAt: settings?.endAt,
    isModalOpen,
    openModal: () => setIsModalOpen(true),
    closeModal: () => setIsModalOpen(false),
  };
}
