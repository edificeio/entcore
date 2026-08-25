import { Flex } from '@edifice.io/react';
import { useCustomizeForm } from '~/hooks/useCustomizeForm';
import { useI18n } from '~/hooks/useI18n';
import { ChoiceButton } from './ChoiceButton';
import { ChoiceSkeleton } from './ChoiceSkeleton';

type CustomizeFormProps = {
  form: Omit<
    ReturnType<typeof useCustomizeForm>,
    'resetChanges' | 'saveChanges'
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

export const CustomizeForm = ({ form }: CustomizeFormProps) => {
  const { t, common_t } = useI18n();

  const {
    fonts,
    selectedFont,
    handleFontChange,
    languages,
    selectedLanguage,
    handleLanguageChange,
  } = form;

  return (
    <Flex direction="column" gap="32" className="w-100">
      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.fonts')}</h3>
        <Flex>
          {fonts && selectedFont ? (
            fonts.map(({ _id, displayName }) => (
              <ChoiceButton
                key={_id}
                className="font-choice"
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
                  className="customize-language-choice"
                  isSelected={lang === selectedLanguage}
                  onClick={() => handleLanguageChange(lang)}
                >
                  <Flex direction="column" align="center">
                    <div>
                      <img
                        width={56}
                        height={36}
                        src={`https://flagcdn.com/w80/${getCountryCode(lang)}.png`}
                        alt={label}
                        loading="lazy"
                      />
                    </div>
                    <span className="customize-language-label">{label}</span>
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
