import { Flex, RadioTile } from '@edifice.io/react';
import corsica from '../../assets/Corsica.png';
import { useCustomizationForm } from '../../hooks/useCustomizationForm';
import { useI18n } from '../../hooks/useI18n';
import { ChoiceSkeleton } from './ChoiceSkeleton';
import './CustomizationForm.css';

type CustomizationFormProps = {
  form: Omit<
    ReturnType<typeof useCustomizationForm>,
    'isDirty' | 'saveChanges' | 'isSaving'
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

function getFlagUrl(lang: string) {
  switch (lang.toLowerCase()) {
    // For Corse language, display a dedicated svg (and not the Colombia flag!)
    case 'co':
      return corsica;
  }
  return `https://flagcdn.com/w80/${getCountryCode(lang)}.png`;
}

export const CustomizationForm = ({ form }: CustomizationFormProps) => {
  const { t, common_t } = useI18n();

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
      <Flex
        as="section"
        direction="column"
        gap="16"
        className="customization-form-section"
      >
        <h5 className="customization-form-section-title">
          {t('homepage.customize.form.fonts')}
        </h5>
        <Flex gap="8" wrap="wrap">
          {fonts && selectedFont ? (
            fonts.map(({ _id, displayName }) => (
              <RadioTile
                key={_id}
                label={common_t(displayName)}
                value={_id}
                checked={_id === selectedFont}
                onClick={() => handleFontChange(_id)}
              />
            ))
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>

      <Flex
        as="section"
        direction="column"
        gap="16"
        className="customization-form-section"
      >
        <h5 className="customization-form-section-title">
          {t('homepage.customize.form.themes')}
        </h5>
        <Flex gap="12" wrap="wrap">
          {backgrounds ? (
            backgrounds.map((background) => (
              <RadioTile
                key={background}
                value={background}
                checked={background === selectedBackground}
                image={
                  <span
                    data-background={background}
                    className="customization-form-background-preview"
                    aria-hidden="true"
                  />
                }
                hideLabel={true}
                label={background}
                onClick={() => handleBackgroundChange(background)}
              />
            ))
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>

      <Flex
        as="section"
        direction="column"
        gap="16"
        className="customization-form-section"
      >
        <h5 className="customization-form-section-title">
          {t('homepage.customize.form.languages')}
        </h5>
        <Flex gap="12" wrap="wrap">
          {languages ? (
            languages.map((lang) => (
              <RadioTile
                key={lang}
                checked={lang === selectedLanguage}
                value={lang}
                label={t(`language.${lang}`)}
                image={getFlagUrl(lang)}
                orientation="vertical"
                onClick={() => handleLanguageChange(lang)}
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
