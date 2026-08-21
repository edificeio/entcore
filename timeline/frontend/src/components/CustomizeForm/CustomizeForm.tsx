import { ButtonBeta, ButtonSkeleton, Flex } from '@edifice.io/react';
import { useCustomizeForm } from '~/hooks/useCustomizeForm';
import { useI18n } from '~/hooks/useI18n';

type CustomizeFormProps = {
  form: ReturnType<typeof useCustomizeForm>;
};

export const CustomizeForm = ({ form }: CustomizeFormProps) => {
  const { t, common_t } = useI18n();

  const { languages, onLanguageChange } = form;

  return (
    <Flex direction="column" gap="32" className="w-100">
      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.fonts')}</h3>
      </Flex>
      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.themes')}</h3>
      </Flex>
      <Flex direction="column" gap="16" className="w-100">
        <h3>{t('homepage.customize.form.languages')}</h3>
        <Flex>
          {languages
            ? languages.map((e) => (
                <li>
                  <ButtonBeta onClick={() => onLanguageChange(e)}>
                    {t(`language.${e}`)}
                  </ButtonBeta>
                </li>
              ))
            : [1, 2, 3].map(() => <ButtonSkeleton size="lg" />)}
        </Flex>
      </Flex>
    </Flex>
  );
};
