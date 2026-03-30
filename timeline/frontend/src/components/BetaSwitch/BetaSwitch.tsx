import { odeServices } from '@edifice.io/client';
import { Button, useToast } from '@edifice.io/react';
import { useState } from 'react';
import { createPortal } from 'react-dom';
import { useI18n } from '~/hooks/useI18n';
import './BetaSwitch.css';

type UserPrefs = { homePage: { betaEnabled: boolean } | null };
/*
async function getUserPrefs() {
  const userPrefs = await odeServices
    .http()
    .get<UserPrefs>('/userbook/api/preferences');
  return odeServices.http().isResponseError()
    ? Promise.reject('error')
    : userPrefs;
}
*/

function deactivateHomepage() {
  return odeServices.http().put<UserPrefs>('/userbook/api/preferences', {
    homePage: { betaEnabled: false },
  });
}

export function BetaSwitch() {
  const { t } = useI18n();
  const { error } = useToast();
  const [isSwitching, setIsSwitching] = useState(false);

  const handleSwitchClick = async () => {
    setIsSwitching(true);

    try {
      await deactivateHomepage();
      window.location.reload();
    } catch {
      error(t('timeline.beta.switch.error'));
      setIsSwitching(false);
    }
  };

  const rootElement = document.getElementById('beta-switch');

  return rootElement
    ? createPortal(
        <Button
          data-testid="beta-switch-button"
          isLoading={isSwitching}
          color="tertiary"
          variant="outline"
          disabled={isSwitching}
          onClick={handleSwitchClick}
        >
          {t('timeline.beta.switch.previous')}
        </Button>,
        rootElement,
      )
    : null;
}
