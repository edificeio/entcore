import { useToast } from '@edifice.io/react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { preferenceService } from '~/services';

export function useBetaSwitch() {
  const { t: common_t } = useTranslation('common');
  const toast = useToast();
  const [isSwitching, setIsSwitching] = useState(false);

  const onSwitchClick = async () => {
    setIsSwitching(true);

    try {
      await preferenceService.deactivateHomepage();
      window.location.reload();
    } catch {
      toast.error(common_t('betaSwitch.error'));
      setIsSwitching(false);
    }
  };

  return {
    isSwitching,
    onSwitchClick,
  };
}
