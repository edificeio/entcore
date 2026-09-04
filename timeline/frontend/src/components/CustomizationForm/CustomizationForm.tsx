import { Flex } from '@edifice.io/react';
import { useCustomizationForm } from '~/hooks/useCustomizationForm';
import { useI18n } from '~/hooks/useI18n';
import { Background } from '~/services';
import { ChoiceButton } from './ChoiceButton';
import { ChoiceSkeleton } from './ChoiceSkeleton';

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

const backgroundImages = import.meta.glob('../../assets/*.png', {
  eager: true,
  import: 'default',
  query: '?url',
});

function getBackgroundImgSrc(background: Background) {
  return backgroundImages[`../../assets/${background}.png`] as string;
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
    <Flex direction="column" gap="32" className="w-100">
      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.fonts')}</h3>
        <Flex gap="8" wrap="wrap">
          {fonts && selectedFont ? (
            fonts.map(({ _id, displayName }) => (
              <ChoiceButton
                key={_id}
                variant="font"
                isSelected={_id === selectedFont}
                onClick={() => handleFontChange(_id)}
              >
                {common_t(displayName)}
              </ChoiceButton>
            ))
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>

      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.themes')}</h3>
        <Flex gap="12" wrap="wrap">
          {backgrounds ? (
            backgrounds.map((background) => (
              <ChoiceButton
                key={background}
                variant="background"
                isSelected={background === selectedBackground}
                onClick={() => handleBackgroundChange(background)}
              >
                <span
                  className="choice-button--background__img"
                  style={{
                    backgroundImage: `url(${getBackgroundImgSrc(background)})`,
                  }}
                />
              </ChoiceButton>
            ))
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>

      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.languages')}</h3>
        <Flex gap="12" wrap="wrap">
          {languages ? (
            languages.map((lang) => {
              const label = t(`language.${lang}`);
              return (
                <ChoiceButton
                  key={lang}
                  variant="language"
                  isSelected={lang === selectedLanguage}
                  onClick={() => handleLanguageChange(lang)}
                >
                  <Flex direction="column" align="center" gap="8">
                    <img
                      className="choice-button--language__img"
                      width={60}
                      height={40}
src={`https://flagcdn.com/w80/${getCountryCode(lang)}.png`}
alt=""
loading="lazy"
                    />
                    <span>{label}</span>
                  </Flex>
                </ChoiceButton>
              );
            })
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>
    </Flex>
  );
};
