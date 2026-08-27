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
        <Flex>
          {languages ? (
            languages.map((lang) => (
              <ChoiceButton
                key={lang}
                isSelected={lang === selectedLanguage}
                onClick={() => handleLanguageChange(lang)}
              >
                {t(`language.${lang}`)}
              </ChoiceButton>
            ))
          ) : (
            <ChoiceSkeleton />
          )}
        </Flex>
      </Flex>
    </Flex>
  );
};
