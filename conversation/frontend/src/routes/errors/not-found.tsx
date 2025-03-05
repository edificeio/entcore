import { Button, Heading, Layout } from '@edifice.io/react';
import { t } from 'i18next';
import { useNavigate, useRouteError } from 'react-router-dom';

export const NotFound = () => {
  const error = useRouteError();
  const navigate = useNavigate();
  console.error(error);

  return (
    <Layout>
      <div className="d-flex flex-column gap-16 align-items-center mt-64">
        <Heading level="h2" headingStyle="h2" className="text-secondary">
          {t('oops')}
        </Heading>
        <div className="text">{t('e404.page')}</div>
        <Button color="primary" onClick={() => navigate(-1)}>
          {t('back')}
        </Button>
      </div>
    </Layout>
  );
};
