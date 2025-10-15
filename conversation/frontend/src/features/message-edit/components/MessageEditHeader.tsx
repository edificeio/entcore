import { Button } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { Message } from '~/models';
import { MessageEditHeaderAlerts } from './MessageEditHeaderAlerts';
import { useRecipientGroupAlert } from './useRecipientGroupAlert';
import { RecipientListEdit } from './RecipientListEdit';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageEditHeader({ message }: MessageHeaderProps) {
  const { to, cc, cci } = message;
  const { t } = useI18n();
  const { hasAlertOnGroups } = useRecipientGroupAlert({ to, cc });
  const [showRecipientsNotVisibleAlert, setShowRecipientsNotVisibleAlert] =
    useState(false);
  const [showCC, setShowCC] = useState(
    cc.groups.length > 0 || cc.users.length > 0,
  );
  const [showCCI, setShowCCI] = useState(
    cci && (cci.groups.length > 0 || cci.users.length > 0),
  );

  const handleRecipientsNotVisible = (hasNotVisibleCount: boolean) => {
    setShowRecipientsNotVisibleAlert(hasNotVisibleCount);
  };

  return (
    <>
      <MessageEditHeaderAlerts
        hasAlertOnGroups={hasAlertOnGroups}
        hasRecipientsNotVisibleAlert={showRecipientsNotVisibleAlert}
      />
      <div className="d-flex flex-fill flex-column overflow-hidden">
        <div className="d-flex align-items-center justify-content-between gap-12 border-bottom pe-16">
          <RecipientListEdit
            head={<span className="me-4">{t('at')}</span>}
            recipients={to}
            recipientType="to"
            onRecipientsNotVisible={handleRecipientsNotVisible}
          />
          <div className="d-flex align-items-center">
            <Button
              onClick={() => setShowCC((prev) => !prev)}
              variant="ghost"
              size="sm"
              color="secondary"
              hidden={showCC}
            >
              {t('button.cc')}
            </Button>
            <Button
              onClick={() => setShowCCI((prev) => !prev)}
              variant="ghost"
              size="sm"
              color="secondary"
              hidden={showCCI}
            >
              {t('button.cci')}
            </Button>
          </div>
        </div>
        {showCC && (
          <div className="border-bottom">
            <RecipientListEdit
              head={<span className="me-4">{t('cc')}</span>}
              recipients={cc}
              recipientType="cc"
              onRecipientsNotVisible={handleRecipientsNotVisible}
            />
          </div>
        )}
        {showCCI && (
          <div className="border-bottom">
            <RecipientListEdit
              head={<span className="me-4">{t('cci')}</span>}
              recipients={cci || { groups: [], users: [] }}
              recipientType="cci"
              onRecipientsNotVisible={handleRecipientsNotVisible}
            />
          </div>
        )}
      </div>
    </>
  );
}
