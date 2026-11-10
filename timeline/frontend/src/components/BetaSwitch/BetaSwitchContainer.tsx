import { BetaSwitch } from '@edifice.io/react';
import { useBetaSwitch } from './useBetaSwitch';

export function BetaSwitchContainer() {
  const { onSwitchClick, isSwitching, canSwitch } = useBetaSwitch();

  if (canSwitch !== true) return null;

  return <BetaSwitch onSwitchClick={onSwitchClick} isSwitching={isSwitching} />;
}

BetaSwitchContainer.displayName = 'BetaSwitchContainer';
