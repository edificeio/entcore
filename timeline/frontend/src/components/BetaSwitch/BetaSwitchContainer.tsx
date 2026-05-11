import { BetaSwitch } from '@edifice.io/react';
import { useBetaSwitch } from './useBetaSwitch';

export function BetaSwitchContainer() {
  const { onSwitchClick, isSwitching } = useBetaSwitch();
  return <BetaSwitch onSwitchClick={onSwitchClick} isSwitching={isSwitching} />;
}

BetaSwitchContainer.displayName = 'BetaSwitchContainer';
