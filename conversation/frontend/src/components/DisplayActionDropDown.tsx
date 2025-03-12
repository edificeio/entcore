import {
  Button,
  Dropdown,
  IconButton,
  IconButtonProps,
  useUser,
} from '@edifice.io/react';
import {
  IconDelete,
  IconOptions,
  IconPrint,
  IconRedo,
  IconRestore,
  IconSend,
  IconUndo,
  IconUndoAll,
  IconUnreadMail,
} from '@edifice.io/react/icons';
import { RefAttributes, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Message } from '~/models';
import {
  useDeleteMessage,
  useMarkUnread,
  useRestoreMessage,
  useTrashMessage,
} from '~/services';
import { useConfirmModalStore } from '~/store';

export function DisplayActionDropDown({ message }: { message: Message }) {
  const { t } = useTranslation('conversation');
  const markAsUnreadQuery = useMarkUnread();
  const navigate = useNavigate();
  const { openModal } = useConfirmModalStore();
  const deleteMessage = useDeleteMessage();
  const restoreQuery = useRestoreMessage();
  const moveToTrashQuery = useTrashMessage();
  const user = useUser();

  const handleDelete = () => {
    openModal({
      id: 'delete-modal',
      header: <>{t('delete.definitely')}</>,
      body: <p>{t('delete.definitely.confirm')}</p>,
      okText: t('confirm'),
      koText: t('cancel'),
      onSuccess: () => {
        deleteMessage.mutate({ id: message.id });
        navigate('/trash');
      },
    });
  };

  const buttonAction = [
    {
      label: t('reply'),
      id: 'reply',
      icon: <IconUndo />,
      action: () => {
        alert('reply');
      },
      hidden: message.state === 'DRAFT' || message.trashed,
    },
    {
      label: t('submit'),
      id: 'submit',
      icon: <IconSend />,
      action: () => {
        alert('submit');
      },
      hidden: message.state !== 'DRAFT' || message.trashed,
    },
    {
      label: t('restore'),
      id: 'restore',
      icon: <IconRestore />,
      action: () => {
        restoreQuery.mutate({ id: message.id });
        navigate('/trash');
      },
      hidden: !message.trashed,
    },
  ];

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ messages: [message] });
    navigate(`../..`, { relative: 'path' });
  };

  const canReplyAll = useMemo(() => {
      const { to, cc, cci } = message;

      // It's message to myself with cci
      const isMeWithCci = (
        to.users.length === 1 &&
        to.users[0].id === user.user?.userId &&
        (cci?.groups.length || cci?.users.length)
      );

      // Count number of recipients
      const hasRecipients = (to.users.length + to.groups.length + cc.users.length + cc.groups.length) > 1;

      return (isMeWithCci || hasRecipients) && message.state !== 'DRAFT' && !message.trashed;
    }, [message, user]);

  const options = [
    {
      label: t('tag.unread'),
      icon: <IconUnreadMail />,
      action: () => {
        handleMarkAsUnreadClick();
      },
      hidden: message.state === 'DRAFT' || message.trashed,
    },
    {
      label: t('replyall'),
      icon: <IconUndoAll />,
      action: () => {
        alert('reply all');
      },
      hidden: !canReplyAll,
    },
    {
      label: t('transfer'),
      icon: <IconRedo />,
      action: () => {
        alert('transfer');
      },
      hidden: message.state === 'DRAFT' || message.trashed,
    },
    {
      label: t('trash'),
      icon: <IconDelete />,
      action: () => {
        moveToTrashQuery.mutate({ id: message.id });
        navigate(`../..`, { relative: 'path' });
      },
      hidden: message.trashed,
    },
    {
      label: t('delete'),
      icon: <IconDelete />,
      action: handleDelete,
      hidden: !message.trashed,
    },
    {
      label: t('print'),
      icon: <IconPrint />,
      action: () => {
        alert('print');
      },
      hidden: message.state === 'DRAFT',
    },
  ];

  return (
    <>
      {buttonAction
        .filter((o) => !o.hidden)
        .map((option) => (
          <Button
            key={option.id}
            color="primary"
            variant="outline"
            leftIcon={option.icon}
            onClick={option.action}
          >
            {option.label}
          </Button>
        ))}
      <Dropdown>
        {(
          triggerProps: JSX.IntrinsicAttributes &
            Omit<IconButtonProps, 'ref'> &
            RefAttributes<HTMLButtonElement>,
        ) => (
          <div data-testid="dropdown">
            <IconButton
              {...triggerProps}
              type="button"
              size="sm"
              aria-label=""
              color="primary"
              variant="outline"
              icon={<IconOptions />}
            />
            <Dropdown.Menu>
              {options
                .filter((o) => !o.hidden)
                .map((option) => (
                  <Dropdown.Item
                    key={option.label}
                    icon={option.icon}
                    onClick={() => option.action()}
                  >
                    {option.label}
                  </Dropdown.Item>
                ))}
            </Dropdown.Menu>
          </div>
        )}
      </Dropdown>
    </>
  );
}
