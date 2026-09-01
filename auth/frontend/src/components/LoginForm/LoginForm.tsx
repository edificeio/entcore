import { ButtonBeta, Checkbox, FormControl } from '@edifice.io/react';
import { IconLock, IconRafterLeft, IconUser } from '@edifice.io/react/icons';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import './LoginForm.css';

interface LoginFormProps {
  onBack: () => void;
}

export const LoginForm = ({ onBack }: LoginFormProps) => {
  const { t } = useTranslation('auth');
  const [rememberMe, setRememberMe] = useState(false);

  return (
    <div className="wayf-login-form" data-testid="wayf-form-login">
      <button
        type="button"
        className="wayf-back-btn"
        onClick={onBack}
        data-testid="wayf-button-back"
      >
        <IconRafterLeft aria-hidden="true" width={20} height={20} />
        <span>{t('wayf.link.back') || 'Retour'}</span>
      </button>

      <form
        className="wayf-login-form__fields"
        method="post"
        action="/auth/login"
      >
        <FormControl id="wayf-login-username">
          <FormControl.Label leftIcon={<IconUser width={18} height={18} />}>
            {t('wayf.login.username')}
          </FormControl.Label>
          <FormControl.Input
            size="md"
            type="text"
            name="email"
            autoComplete="username"
          />
        </FormControl>

        <FormControl id="wayf-login-password">
          <FormControl.Label leftIcon={<IconLock width={18} height={18} />}>
            {t('wayf.login.password')}
          </FormControl.Label>
          <FormControl.Input
            size="md"
            type="password"
            name="password"
            autoComplete="current-password"
          />
        </FormControl>

        <Checkbox
          name="rememberMe"
          value="true"
          label={t('wayf.login.remember')}
          checked={rememberMe}
          onChange={(event) => setRememberMe(event.target.checked)}
        />

        <ButtonBeta type="submit" className="wayf-login-form__submit">
          {t('wayf.login.submit')}
        </ButtonBeta>
      </form>

      <div className="wayf-login-form__forgot-links">
        <a href="/auth/forgot#/id" className="wayf-login-form__forgot-link">
          {t('wayf.login.forgot-id')}
        </a>
        <a
          href="/auth/forgot#/password"
          className="wayf-login-form__forgot-link"
        >
          {t('wayf.login.forgot-password')}
        </a>
      </div>
    </div>
  );
};
