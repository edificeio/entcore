import { Alert, Button } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { Group, Message } from '~/models';
import { RecipientListEdit } from './RecipientListEdit';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageEditHeader({ message }: MessageHeaderProps) {
  const { t } = useI18n();

  const { to, cc, cci } = message;

  const [showCC, setShowCC] = useState(
    cc.groups.length > 0 || cc.users.length > 0,
  );
  const [showCCI, setShowCCI] = useState(
    cci && (cci.groups.length > 0 || cci.users.length > 0),
  );

  const alertOnGroupsFilter = (group: Group) =>
    group.subType &&
    group.type == 'ProfileGroup' &&
    ['Student', 'Relative', 'ClassGroup'].includes(group.subType);
  const alertOnGroups =
    to.groups.findIndex(alertOnGroupsFilter) >= 0 ||
    cc.groups.findIndex(alertOnGroupsFilter) >= 0;

  return (
    <>
      {alertOnGroups && (
        <div className="pt-12 pb-8 px-24">
          <Alert type={'warning'} isDismissible={true}>
            <p className="pe-24">{t('conversation.warn.message.grouped')}</p>
          </Alert>
        </div>
      )}
      <div className="d-flex flex-fill flex-column overflow-hidden">
        <div className="d-flex align-items-center justify-content-between gap-12 border-bottom pe-16">
          <RecipientListEdit
            head={<span className="me-4">{t('at')}</span>}
            recipients={to}
            recipientType="to"
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
            />
          </div>
        )}
        {showCCI && (
          <div className="border-bottom">
            <RecipientListEdit
              head={<span className="me-4">{t('cci')}</span>}
              recipients={cci || { groups: [], users: [] }}
              recipientType="cci"
            />
          </div>
        )}
      </div>
    </>
  );
}
