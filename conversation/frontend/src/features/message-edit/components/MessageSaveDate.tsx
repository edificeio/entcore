import { useDate } from '@edifice.io/react';
import { useEffect, useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { useMessageStore } from '~/store/messageStore';

export function MessageSaveDate() {
  const { fromNow } = useDate();
  const { t } = useI18n();
  const message = useMessageStore.use.message();
  const [dateKey, setDateKey] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => setDateKey((prev) => ++prev), 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    message?.date && (
      <div className="caption fst-italic" key={dateKey}>
        {t('message.saved') + ' ' + fromNow(message.date)}
      </div>
    )
  );
}

export default MessageSaveDate;
