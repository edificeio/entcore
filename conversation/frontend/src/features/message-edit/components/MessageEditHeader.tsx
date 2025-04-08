import { Button } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { RecipientListEdit } from './RecipientListEdit';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageEditHeader({ message }: MessageHeaderProps) {
  const { t } = useI18n();

  const [showCC, setShowCC] = useState(false);
  const [showCCI, setShowCCI] = useState(false);

  const { to, cc, cci } = message;

  return (
    <>
      <div className="d-flex flex-fill flex-column overflow-hidden">
        <div className="d-flex align-items-center justify-content-between gap-12 border-bottom pe-16">
          <RecipientListEdit
            head={<span className="me-4">{t('at')} :</span>}
            recipients={to}
            recipientType="to"
          />
          <div className="d-flex align-items-center">
            <Button
              onClick={() => setShowCC((prev) => !prev)}
              variant="ghost"
              size="sm"
              color="secondary"
            >
              {t('button.cc')}
            </Button>
            <Button
              onClick={() => setShowCCI((prev) => !prev)}
              variant="ghost"
              size="sm"
              color="secondary"
            >
              {t('button.cci')}
            </Button>
          </div>
        </div>
        {showCC && (
          <div className="border-bottom">
            <RecipientListEdit
              head={<span className="me-4">{t('cc')} :</span>}
              recipients={cc}
              recipientType="cc"
            />
          </div>
        )}
        {showCCI && (
          <div className="border-bottom">
            <RecipientListEdit
              head={<span className="me-4">{t('cci')} :</span>}
              recipients={cci || { groups: [], users: [] }}
              recipientType="cci"
            />
          </div>
        )}
      </div>
    </>
  );
}
