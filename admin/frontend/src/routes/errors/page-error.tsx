import { useEdificeClient } from '@edifice.io/react';
import Button from '@mui/material/Button';
import { useTranslation } from 'react-i18next';
import { useNavigate, useRouteError } from 'react-router-dom';

export const PageError = () => {
  const error = useRouteError();
  const navigate = useNavigate();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  console.error(error);

  return (
    <div>
      <p>{t('oops')}</p>
      <div className="text">{t('boilerplate.notfound.or.unauthorized')}</div>
      <Button color="primary" onClick={() => navigate(-1)}>
        {t('back')}
      </Button>
    </div>
  );
};
