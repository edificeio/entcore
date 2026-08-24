import { ButtonBeta, ButtonBetaProps } from '@edifice.io/react';

export type ChoiceButtonProps = ButtonBetaProps & { isSelected: boolean };

export const ChoiceButton = ({
  key,
  children,
  isSelected,
  onClick: handleClick,
}: ChoiceButtonProps) => {
  return (
    <>
      <ButtonBeta
        key={key}
        variant={isSelected ? 'filled' : 'outline'}
        aria-pressed={isSelected}
        onClick={handleClick}
      >
        {children}
      </ButtonBeta>
    </>
  );
};
