import { ButtonBeta, ButtonBetaProps, Flex } from '@edifice.io/react';
import clsx from 'clsx';
import { Background } from '~/services';
import './ChoiceButton.css';

type FontChoice = {
  variant: 'font';
  _id: string;
  label: string;
  onClick: (id: string) => void;
};

type BackgroundChoice = {
  variant: 'background';
  background: Background;
  label: string;
  imgSrc: string;
  onClick: (background: Background) => void;
};

type LanguageChoice = {
  variant: 'language';
  lang: string;
  label: string;
  imgSrc: string;
  onClick: (lang: string) => void;
};

export type Choice = FontChoice | BackgroundChoice | LanguageChoice;

export type ChoiceButtonProps = Omit<
  ButtonBetaProps,
  'variant' | 'onClick' | 'children'
> & {
  isSelected: boolean;
  choice: Choice;
};

export const ChoiceButton = ({
  className: baseClassName,
  isSelected,
  choice,
  ...buttonBetaProps
}: ChoiceButtonProps) => {
  const className = clsx(
    'choice-button',
    `choice-button--${choice.variant}`,
    baseClassName,
    {
      isSelected,
    },
  );

  const handleClick = () => {
    switch (choice.variant) {
      case 'font':
        return choice.onClick(choice._id);
      case 'background':
        return choice.onClick(choice.background);
      case 'language':
        return choice.onClick(choice.lang);
    }
  };

  return (
    <ButtonBeta
      variant={isSelected ? 'filled' : 'outline'}
      aria-pressed={isSelected}
      aria-label={choice.label}
      className={className}
      onClick={handleClick}
      {...buttonBetaProps}
    >
      {choice.variant === 'font' && choice.label}
      {choice.variant === 'background' && (
        <span
          className="choice-button__img"
          style={{ backgroundImage: `url(${choice.imgSrc})` }}
        />
      )}
      {choice.variant === 'language' && (
        <Flex direction="column" align="center" gap="8">
          <img
            className="choice-button__img"
            width={60}
            height={40}
            src={choice.imgSrc}
            alt=""
            loading="lazy"
          />
          <span>{choice.label}</span>
        </Flex>
      )}
    </ButtonBeta>
  );
};
