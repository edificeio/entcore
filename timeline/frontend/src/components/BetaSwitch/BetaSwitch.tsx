import { odeServices } from '@edifice.io/client';
import { Button, Flex, useToast } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import './BetaSwitch.css';

type UserPrefs = { homePage: { betaEnabled: boolean } | null };

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

  return (
    <Flex direction="row">
      <p>
        <strong>{t('timeline.beta.switch.title')}</strong>
        {t('timeline.beta.switch.description')}
      </p>
      <Button
        data-testid="beta-switch-button"
        isLoading={isSwitching}
        color="tertiary"
        variant="outline"
        disabled={isSwitching}
        onClick={handleSwitchClick}
      >
        {t('timeline.beta.switch.button')}
      </Button>
    </Flex>
  );
}
