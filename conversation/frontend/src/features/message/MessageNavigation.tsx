import { Button } from '@edifice.io/react';
import { IconArrowLeft } from '@edifice.io/react/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  MessageActionDropdown,
  MessageActionDropdownProps,
} from '~/components/MessageActionDropdown/MessageActionDropdown';
import { useI18n, useSelectedFolder } from '~/hooks';
import { Message } from '~/models';
import Pagination from './components/Pagination';
import { useMessageNavigation } from './hooks/useMessageNavigation';

export function MessageNavigation({ message }: { message: Message }) {
  const navigate = useNavigate();
  const { common_t } = useI18n();
  const { folderId } = useSelectedFolder();
  const { currentMessagePosition, totalMessagesCount, getMessageIdAtPosition } =
    useMessageNavigation(message.id);
  const [searchParams] = useSearchParams();

  const actionDropdownProps: MessageActionDropdownProps = {
    message,
    appearance: {
      dropdownVariant: 'ghost',
      mainButtonVariant: 'ghost',
      buttonColor: 'tertiary',
    },
  };

  const handleGoBack = () => {
    navigate({
      pathname: `/${folderId}`,
      search: searchParams.toString(),
    });
  };

  const handleMessageChange = async (nextPosition: number) => {
    const messageId = await getMessageIdAtPosition?.(nextPosition);
    if (!messageId) return;
    navigate({
      pathname: `/${folderId}/message/${messageId}`,
      search: searchParams.toString(),
    });
  };

  return (
    <nav className="d-print-none border-bottom px-16 py-4 d-flex align-items-center justify-content-between w-100">
      <Button
        color="tertiary"
        variant="ghost"
        leftIcon={<IconArrowLeft />}
        onClick={handleGoBack}
        disabled={false}
      >
        {common_t('back')}
      </Button>
      {!!currentMessagePosition && (
        <Pagination
          current={currentMessagePosition}
          total={totalMessagesCount}
          onChange={handleMessageChange}
        />
      )}
      <MessageActionDropdown {...actionDropdownProps} className="gap-8" />
    </nav>
  );
}
