import { ButtonBeta, ButtonBetaProps } from '@edifice.io/react';
import clsx from 'clsx';
import './ChoiceButton.css';

export type ChoiceButtonProps = Omit<ButtonBetaProps, 'variant'> & {
  isSelected: boolean;
  variant: 'font' | 'background' | 'language';
};

export const ChoiceButton = ({
  children,
  className: baseClassName,
  isSelected,
  variant,
  ...buttonBetaProps
}: ChoiceButtonProps) => {
  const className = clsx(
    'choice-button',
    `choice-button--${variant}`,
    baseClassName,
    {
      isSelected,
    },
  );
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
