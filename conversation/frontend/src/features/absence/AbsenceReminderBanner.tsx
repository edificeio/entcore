import { Alert, Button, useDate } from '@edifice.io/react';
import { useI18n } from '~/hooks/useI18n';
import { AbsenceModal } from './AbsenceModal';
import { useAbsenceReminder } from './hooks';

/**
 * Reminder banner shown on the message list screens (inbox, folders,
 * outbox, trash) while the user's absence message is active. Not rendered
 * on the message detail/compose screens, which don't mount this component.
 */
export function AbsenceReminderBanner() {
  const { t } = useI18n();
  const { formatDate } = useDate();
  const { isActive, endAt, isModalOpen, openModal, closeModal } =
    useAbsenceReminder();

  if (!isActive) return null;

  return (
    <>
      <Alert
        type="warning"
        className="mx-16 mx-lg-24 mt-16"
        button={
          <Button
            type="button"
            color="tertiary"
            variant="ghost"
            data-testid="conversation-absence-banner-button-edit"
            onClick={openModal}
          >
            {t('conversation.absence.banner.edit')}
          </Button>
        }
      >
        {t('conversation.absence.banner.text', {
          endDate: formatDate(
            endAt!,
            t('conversation.absence.modal.dateFormat'),
          ),
        })}
      </Alert>

      {isModalOpen && (
        <AbsenceModal isOpen={isModalOpen} onModalClose={closeModal} />
      )}
    </>
  );
}
