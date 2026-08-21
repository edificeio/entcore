import { ButtonBeta, ButtonSkeleton, Flex } from '@edifice.io/react';
import { useCustomizeForm } from '~/hooks/useCustomizeForm';
import { useI18n } from '~/hooks/useI18n';

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
          {fonts && selectedFont
            ? fonts.map(({ _id, displayName }) => (
                <ButtonBeta
                  key={_id}
                  variant={_id === selectedFont ? 'filled' : 'outline'}
                  onClick={() => handleFontChange(_id)}
                >
                  {common_t(displayName)}
                </ButtonBeta>
              ))
            : [1, 2, 3].map((num) => <ButtonSkeleton key={num} size="lg" />)}
        </Flex>
      </Flex>
      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.themes')}</h3>
      </Flex>
      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.languages')}</h3>
        <Flex>
          {languages
            ? languages.map((lang) => (
                <ButtonBeta
                  key={lang}
                  variant={lang === selectedLanguage ? 'filled' : 'ghost'}
                  onClick={() => handleLanguageChange(lang)}
                >
                  {t(`language.${lang}`)}
                </ButtonBeta>
              ))
            : [1, 2, 3].map((num) => <ButtonSkeleton key={num} size="lg" />)}
        </Flex>
      </Flex>
    </Flex>
  );
};
