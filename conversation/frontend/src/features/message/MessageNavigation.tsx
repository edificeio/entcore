import {
  MessageActionDropDown,
  MessageActionDropDownProps,
} from '~/components/MessageActionDropDown/MessageActionDropDown';
import { MessageProps } from '.';
import { Button } from '@edifice.io/react';
import { IconArrowLeft } from '@edifice.io/react/icons';
import { useI18n, useSelectedFolder } from '~/hooks';
import { useNavigate } from 'react-router-dom';
import Pagination from './components/Pagination';

export function MessageNavigation({ message }: MessageProps) {
  const navigate = useNavigate();
  const { common_t } = useI18n();
  const { folderId } = useSelectedFolder();

  const actionDropDownProps: MessageActionDropDownProps = {
    message,
    appearance: {
      dropdownVariant: 'ghost',
      mainButtonVariant: 'ghost',
      buttonColor: 'tertiary',
    },
  };

  const handleGoBack = () => {
    navigate(`/folder/${folderId}`);
  };

  const handleMessageChange = (index: number) => {
    console.log('index:', index);
  };

  return (
    <nav className="border-bottom px-16 py-4 d-flex d-flex align-items-center justify-content-between w-100">
      <Button
        color="tertiary"
        variant="ghost"
        leftIcon={<IconArrowLeft />}
        onClick={handleGoBack}
        disabled={false}
      >
        {common_t('back')}
      </Button>
      <Pagination current={2} total={5} onChange={handleMessageChange} />
      <MessageActionDropDown {...actionDropDownProps} />
    </nav>
  );
}
