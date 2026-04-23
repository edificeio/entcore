import { useEdificeClient } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';

export function useI18n() {
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { t: common_t } = useTranslation('common');

  return { t, common_t };
}
