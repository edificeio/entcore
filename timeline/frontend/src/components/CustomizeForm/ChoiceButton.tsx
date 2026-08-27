import { ButtonBeta, ButtonBetaProps } from '@edifice.io/react';

export type ChoiceButtonProps = ButtonBetaProps & { isSelected: boolean };

export const ChoiceButton = ({
  children,
  isSelected,
  onClick: handleClick,
}: ChoiceButtonProps) => {
  return (
    <>
      <ButtonBeta
        variant={isSelected ? 'filled' : 'outline'}
        aria-pressed={isSelected}
        onClick={handleClick}
      >
        {children}
      </ButtonBeta>
    </>
  );
};
