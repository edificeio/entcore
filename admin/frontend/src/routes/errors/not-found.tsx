import Button from '@mui/material/Button';
import { t } from 'i18next';
import { useNavigate, useRouteError } from 'react-router-dom';

export const NotFound = () => {
  const error = useRouteError();
  const navigate = useNavigate();
  console.error(error);

  return (
    <div>
      <p>{t('oops')}</p>
      <div className="text">{t('e404.page')}</div>
      <Button color="primary" onClick={() => navigate(-1)}>
        {t('back')}
      </Button>
    </div>
  );
};
