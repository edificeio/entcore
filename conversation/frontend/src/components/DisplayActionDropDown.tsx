import {
  Button,
  Dropdown,
  IconButton,
  IconButtonProps,
  useEdificeClient,
} from '@edifice.io/react';
import {
  IconDelete,
  IconOptions,
  IconPrint,
  IconRedo,
  IconRestore,
  IconSave,
  IconSend,
  IconUndo,
  IconUndoAll,
  IconUnreadMail,
} from '@edifice.io/react/icons';
import { RefAttributes, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useI18n, useSelectedFolder } from '~/hooks';
import { Message } from '~/models';
import {
  isInRecipient,
  useCreateDraft,
  useDeleteMessage,
  useMarkUnread,
  useRestoreMessage,
  useTrashMessage,
  useUpdateDraft,
} from '~/services';
import { useConfirmModalStore } from '~/store';

export function DisplayActionDropDown({ message }: { message: Message }) {
  const { t } = useI18n();
  const markAsUnreadQuery = useMarkUnread();
  const navigate = useNavigate();
  const { openModal } = useConfirmModalStore();
  const deleteMessage = useDeleteMessage();
  const restoreQuery = useRestoreMessage();
  const moveToTrashQuery = useTrashMessage();
  const createDraft = useCreateDraft();
  const updateDraft = useUpdateDraft();
  const { folderId } = useSelectedFolder();
  const { user } = useEdificeClient();

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
        console.log('submit', message);
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

  const canReplyAll = useMemo(() => {
    const { to, cc, cci } = message;

    // It's message to myself with cci
    const isMeWithCci =
      to.users.length === 1 &&
      to.users[0].id === user?.userId &&
      (cci?.groups.length || cci?.users.length);

    // Count number of recipients
    const hasRecipients =
      to.users.length + to.groups.length + cc.users.length + cc.groups.length >
      1;

    return (
      (isMeWithCci || hasRecipients) &&
      message.state !== 'DRAFT' &&
      !message.trashed
    );
  }, [message, user]);

  const canMarkUnread = useMemo(() => {
    return (
      message.state !== 'DRAFT' &&
      !message.trashed &&
      !['draft', 'outbox', 'trash'].includes(folderId!) &&
      isInRecipient(message, user!.userId)
    );
  }, [message, folderId, user]);

  const handleDeleteClick = () => {
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

  const handleDraftSaveClick = () => {
    const payload = {
      subject: message.subject,
      body: message.body,
      to: [
        ...message.to.users.map((u) => u.id),
        ...message.to.groups.map((g) => g.id),
      ],
      cc: [
        ...message.cc.users.map((u) => u.id),
        ...message.cc.groups.map((g) => g.id),
      ],
      cci: [
        ...(message.cci?.users.map((u) => u.id) ?? []),
        ...(message.cci?.groups?.map((g) => g.id) ?? []),
      ],
    };

    if (message.id) {
      updateDraft.mutate({
        draftId: message.id,
        payload,
      });
    } else {
      createDraft.mutate({ payload });
    }
  };

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ messages: [message] });
    navigate(`../..`, { relative: 'path' });
  };

  const options = [
    {
      label: t('tag.unread'),
      icon: <IconUnreadMail />,
      action: () => {
        handleMarkAsUnreadClick();
      },
      hidden: !canMarkUnread,
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
      label: t('draft.save'),
      icon: <IconSave />,
      action: handleDraftSaveClick,
      hidden: message.state !== 'DRAFT' && !message.trashed,
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
      action: handleDeleteClick,
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
    <div className="d-flex align-items-center gap-12">
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
    </div>
  );
}
