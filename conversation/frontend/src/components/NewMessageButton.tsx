import { Button } from '@edifice.io/react';
import { IconEdit } from '@edifice.io/react/icons';
import { useNavigate } from 'react-router-dom';
import { useI18n } from '~/hooks/useI18n';

export const NewMessageButton = () => {
  const { t } = useI18n();
  const navigate = useNavigate();

  const handleCreateNewMessage = () => {
    navigate('/draft/create');
  };

  return (
    <Button
      leftIcon={<IconEdit />}
      onClick={handleCreateNewMessage}
      className="text-nowrap"
    >
      {t('new.message')}
    </Button>
  );
};
