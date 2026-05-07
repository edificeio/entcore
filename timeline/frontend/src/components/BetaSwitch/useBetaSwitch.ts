import { odeServices } from '@edifice.io/client';
import { useToast } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks/useI18n';

type UserPrefs = { homePage: { betaEnabled: boolean } | null };

function deactivateHomepage() {
  return odeServices.http().put<UserPrefs>('/userbook/api/preferences', {
    homePage: { betaEnabled: false },
  });
}

export function useBetaSwitch() {
  const { common_t } = useI18n();
  const toast = useToast();
  const [isSwitching, setIsSwitching] = useState(false);

  const onSwitchClick = async () => {
    setIsSwitching(true);

    try {
      await deactivateHomepage();
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
