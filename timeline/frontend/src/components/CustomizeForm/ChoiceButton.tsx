import { ButtonBeta, ButtonBetaProps } from '@edifice.io/react';
import clsx from 'clsx';
import './ChoiceButton.css';

export type ChoiceButtonProps = ButtonBetaProps & { isSelected: boolean };

export const ChoiceButton = ({
  children,
  className: baseClassName,
  isSelected,
  ...buttonBetaProps
}: ChoiceButtonProps) => {
  const className = clsx('choice-button', baseClassName, {
    isSelected: isSelected,
  });
  return (
    <ButtonBeta
      variant={isSelected ? 'filled' : 'outline'}
      aria-pressed={isSelected}
      className={className}
      {...buttonBetaProps}
    >
      {children}
    </ButtonBeta>
  );
};
