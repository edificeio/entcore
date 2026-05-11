import { useToast } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { preferenceService } from '~/services';

export function useBetaSwitch() {
  const { common_t } = useI18n();
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
