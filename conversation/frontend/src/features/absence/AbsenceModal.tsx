import { Modal } from '@edifice.io/react';
import { useI18n } from '~/hooks/useI18n';
import { AbsenceModalForm } from './AbsenceModalForm';
import { AbsenceModalSkeleton } from './AbsenceModalSkeletonBody';
import { useAbsenceModalData } from './hooks';

export interface AbsenceModalProps {
  isOpen: boolean;
  onModalClose: () => void;
}

export function AbsenceModal({ isOpen, onModalClose }: AbsenceModalProps) {
  const { t } = useI18n();
  const { settings, isLoadingSettings, isSaving, handleSave } =
    useAbsenceModalData();

  return (
    <Modal
      size="lg"
      id="modalAbsence"
      isOpen={isOpen}
      onModalClose={onModalClose}
    >
      <Modal.Header onModalClose={onModalClose}>
        {t('conversation.absence.modal.title')}
      </Modal.Header>

      {isLoadingSettings ? (
        <AbsenceModalSkeleton />
      ) : (
        <AbsenceModalForm
          onModalClose={onModalClose}
          settings={settings}
          isSaving={isSaving}
          onSave={handleSave}
        />
      )}
    </Modal>
  );
}
