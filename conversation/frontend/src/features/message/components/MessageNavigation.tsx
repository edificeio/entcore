import { Button, useBreakpoint } from '@edifice.io/react';
import { IconArrowLeft } from '@edifice.io/react/icons';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  MessageActionDropdown,
  MessageActionDropdownProps,
} from '~/components/MessageActionDropdown/MessageActionDropdown';
import { useI18n } from '~/hooks/useI18n';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { Message } from '~/models';
import { useToggleUnreadMessagesFromQueryCache } from '~/services/queries/hooks/useToggleUnreadMessageFromQueryCache';
import { useGoBackToList } from '../hooks/useGoBackToList';
import { useMessageNavigation } from '../hooks/useMessageNavigation';
import Pagination from './Pagination';

export function MessageNavigation({ message }: { message: Message }) {
  const navigate = useNavigate();
  const { common_t } = useI18n();
  const { folderId } = useSelectedFolder();
  const { lg } = useBreakpoint();
  const { currentMessagePosition, totalMessagesCount, getMessageAtPosition } =
    useMessageNavigation(message.id);
  const { goBackToList } = useGoBackToList();
  const [searchParams] = useSearchParams();
  const { toggleUnreadMessagesFromQueryCache } =
    useToggleUnreadMessagesFromQueryCache();
  const actionDropdownProps: MessageActionDropdownProps = {
    message,
    appearance: {
      dropdownVariant: 'ghost',
      mainButtonVariant: 'ghost',
      buttonColor: 'tertiary',
    },
  };

  const handleMessageChange = async (nextPosition: number) => {
    const message = await getMessageAtPosition?.(nextPosition);
    if (!message) return;
    toggleUnreadMessagesFromQueryCache([message], false);
    navigate({
      pathname: `/${folderId}/message/${message.id}`,
      search: searchParams.toString(),
    });
  };

  return (
    <nav className="d-print-none border-bottom px-lg-16 py-4 d-flex align-items-center justify-content-between w-100">
      <Button
        color="tertiary"
        variant="ghost"
        leftIcon={<IconArrowLeft />}
        onClick={goBackToList}
        disabled={false}
      >
        {common_t('back')}
      </Button>
      {!!currentMessagePosition && lg && (
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
