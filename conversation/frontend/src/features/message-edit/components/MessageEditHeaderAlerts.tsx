import { Alert, Flex } from '@edifice.io/react';
import { useI18n } from '~/hooks/useI18n';

export interface MessageEditHeaderAlertsProps {
  hasAlertOnGroups: boolean;
  hasRecipientsNotVisibleAlert: boolean;
}

export function MessageEditHeaderAlerts({
  hasAlertOnGroups,
  hasRecipientsNotVisibleAlert,
}: MessageEditHeaderAlertsProps) {
  const { t } = useI18n();

  if (!hasAlertOnGroups && !hasRecipientsNotVisibleAlert) {
    return null;
  }

  return (
    <Flex className="mx-16 mt-12" gap="12" direction="column">
      {hasAlertOnGroups && (
        <Alert type="warning" isDismissible={true}>
          <p>{t('conversation.warn.message.grouped')}</p>
        </Alert>
      )}
      {hasRecipientsNotVisibleAlert && (
        <Alert type="warning" isDismissible>
          <p>{t('conversation.edit.warn.no.visible.recipients')}</p>
        </Alert>
      )}
    </Flex>
  );
}
