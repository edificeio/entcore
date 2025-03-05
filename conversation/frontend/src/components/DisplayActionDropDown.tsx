import {
  Button,
  Dropdown,
  IconButton,
  IconButtonProps,
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
import { RefAttributes } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Message } from '~/models';
import { useMarkUnread } from '~/services';

export function DisplayActionDropDown({ message }: { message: Message }) {
  const { t } = useTranslation('conversation');
  const markAsUnreadQuery = useMarkUnread();
  const navigate = useNavigate();

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ id: [message.id] });
    navigate(`../..`, { relative: 'path' });
  };

  const buttonAction = [
    {
      label: t('reply'),
      icon: <IconUndo />,
      action: () => {
        alert('reply');
      },
      hidden: message.state === 'DRAFT' || message.trashed,
    },
    {
      label: t('submit'),
      icon: <IconSend />,
      action: () => {
        alert('submit');
      },
      hidden: message.state !== 'DRAFT' || message.trashed,
    },
    {
      label: t('restore'),
      icon: <IconRestore />,
      action: () => {
        alert('restore');
      },
      hidden: !message.trashed,
    },
  ];

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
      hidden: message.state === 'DRAFT' || message.trashed,
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
        alert('delete');
      },
      hidden: message.trashed,
    },
    {
      label: t('delete'),
      icon: <IconDelete />,
      action: () => {
        alert('delete');
      },
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
