import { Button } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks';
import { Message } from '~/models';
import { MessageRecipientListEdit } from './MessageRecipientListEdit';

export interface MessageHeaderProps {
  message: Message;
}

export function MessageHeaderEdit({ message }: MessageHeaderProps) {
  const { t } = useI18n();

  const [showCC, setShowCC] = useState(false);
  const [showCCI, setShowCCI] = useState(false);

  const { to, cc, cci } = message;

  return (
    <>
      <div className="d-flex flex-fill flex-column overflow-hidden">
        <div className="d-flex align-items-center justify-content-between gap-12 border-bottom pe-16">
          <MessageRecipientListEdit
            head={<span>{t('at')} :</span>}
            recipients={to}
            hasLink
          />
          <div>
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
            <MessageRecipientListEdit
              head={<span>{t('cc')} :</span>}
              recipients={cc}
              hasLink
            />
          </div>
        )}
        {showCCI && (
          <div className="border-bottom">
            <MessageRecipientListEdit
              head={<span>{t('cci')} :</span>}
              recipients={cci!}
              hasLink
            />
          </div>
        )}
      </div>
    </>
  );
}
