import { Flex } from '@edifice.io/react';
import { useBackgroundImage } from '~/hooks/useBackgroundImage';
import { useCustomizationForm } from '~/hooks/useCustomizationForm';
import { useI18n } from '~/hooks/useI18n';
import { ChoiceButton } from './ChoiceButton';
import { ChoiceSkeleton } from './ChoiceSkeleton';
import './CustomizationForm.css';

type CustomizationFormProps = {
  form: Omit<
    ReturnType<typeof useCustomizationForm>,
    'resetChanges' | 'saveChanges' | 'isSaving'
  >;
};

/* Map a lang code to a country code. */
function getCountryCode(lang: string) {
  switch (lang) {
    case 'en':
      return 'gb';
  }
  return lang;
}

export const CustomizationForm = ({ form }: CustomizationFormProps) => {
  const { t, common_t } = useI18n();
  const { getBackgroundImgUrl } = useBackgroundImage();

  const {
    fonts,
    selectedFont,
    handleFontChange,
    backgrounds,
    selectedBackground,
    handleBackgroundChange,
    languages,
    selectedLanguage,
    handleLanguageChange,
  } = form;

  return (
    <Flex direction="column" gap="32" className="customization-form">
      <Flex direction="column" gap="16" className="customization-form-section">
        <h3>{t('homepage.customize.form.fonts')}</h3>
        <Flex gap="8" wrap="wrap">
          {fonts && selectedFont ? (
            fonts.map(({ _id, displayName }) => (
              <ChoiceButton
                key={_id}
                choice={{
                  variant: 'font',
                  _id,
                  label: common_t(displayName),
                  onClick: handleFontChange,
                }}
                isSelected={_id === selectedFont}
              />
            ))
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>

      <Flex direction="column" gap="16" className="customization-form-section">
        <h3>{t('homepage.customize.form.themes')}</h3>
        <Flex gap="12" wrap="wrap">
          {backgrounds ? (
            backgrounds.map((background) => (
              <ChoiceButton
                key={background}
                isSelected={background === selectedBackground}
                choice={{
                  variant: 'background',
                  background,
                  label: background,
                  imgSrc: getBackgroundImgUrl(background),
                  onClick: handleBackgroundChange,
                }}
              />
            ))
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>

      <Flex direction="column" gap="16" className="customization-form-section">
        <h3>{t('homepage.customize.form.languages')}</h3>
        <Flex gap="12" wrap="wrap">
          {languages ? (
            languages.map((lang) => (
              <ChoiceButton
                key={lang}
                isSelected={lang === selectedLanguage}
                choice={{
                  variant: 'language',
                  lang,
                  label: t(`language.${lang}`),
                  imgSrc: `https://flagcdn.com/w80/${getCountryCode(lang)}.png`,
                  onClick: handleLanguageChange,
                }}
              />
            ))
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>
    </Flex>
  );
};
