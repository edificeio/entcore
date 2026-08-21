import { ButtonSkeleton, Flex } from '@edifice.io/react';
import { useI18n } from '~/hooks/useI18n';

type CustomizeFormProps = {
  form: {
    languages?: string[];
  };
};

export const CustomizeForm = ({ form }: CustomizeFormProps) => {
  const { t } = useI18n();

  const { languages } = form;

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
        <ul>
          {languages
            ? languages.map((e) => <li>{e}</li>)
            : [1, 2, 3].map(() => <ButtonSkeleton size="lg" />)}
        </ul>
      </Flex>
    </Flex>
  );
};
