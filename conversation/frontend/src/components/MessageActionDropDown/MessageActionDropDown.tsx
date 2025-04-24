import {
  Button,
  ButtonColors,
  ButtonVariants,
  Dropdown,
  IconButton,
  IconButtonProps,
} from '@edifice.io/react';
import { IconOptions } from '@edifice.io/react/icons';
import { RefAttributes, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SentToInactiveUsersModal } from '~/features/message-edit/components/SentToInactiveUsersModal';
import { UndeliveredUsersModal } from '~/features/message-edit/components/UndeliveredUsersModal';
import { Message } from '~/models';
import { useMessageActionDropDown } from './hooks/useMessageActionDropDown';

export interface MessageActionDropDownProps {
  message: Message;
  appearance?: {
    mainButtonVariant?: ButtonVariants;
    dropdownVariant?: ButtonVariants;
    buttonColor?: ButtonColors;
  };
  actions?: string[];
}

export function MessageActionDropDown({
  message,
  actions,
  appearance = {
    mainButtonVariant: 'outline',
    dropdownVariant: 'outline',
    buttonColor: 'primary',
  },
}: MessageActionDropDownProps) {
  const [inactiveUsers, setInactiveUsers] = useState<string[]>([]);
  const [undeliveredUsers, setUndeliveredUsers] = useState<string[]>([]);
  const [openedIncativeUsersModal, setOpenedInactiveUsersModal] =
    useState(false);
  const [openedUndeliveredUsersModal, setOpenedUndeliveredUsersModal] =
    useState(false);
  const { actionButtons, dropdownOptions } = useMessageActionDropDown({
    message,
    actions,
    setInactiveUsers,
    setUndeliveredUsers,
    setOpenedInactiveUsersModal,
    setOpenedUndeliveredUsersModal,
  });
  const navigate = useNavigate();

  const visibleOptions = dropdownOptions.filter((o) => !o.hidden);

  const handleCloseInactiveUsersModal = () => {
    setOpenedInactiveUsersModal(false);
    setInactiveUsers([]);
    if (!undeliveredUsers) {
      navigate(`/inbox`);
    } else {
      setOpenedUndeliveredUsersModal(true);
    }
  };

  const handleCloseUndeliveredUsersModal = () => {
    setOpenedUndeliveredUsersModal(false);
    setUndeliveredUsers([]);
    if (!inactiveUsers) {
      navigate(`/inbox`);
    }
  };

  return (
    <div className="d-flex align-items-center gap-12">
      {actionButtons
        .filter((o) => !o.hidden)
        .map((option) => (
          <Button
            key={option.id}
            color={appearance.buttonColor}
            variant={appearance.mainButtonVariant}
            leftIcon={option.icon}
            onClick={option.action}
            disabled={option.disabled}
          >
            {option.label}
          </Button>
        ))}
      <Dropdown>
        {(
          triggerProps: JSX.IntrinsicAttributes &
            Omit<IconButtonProps, 'ref'> &
            RefAttributes<HTMLButtonElement>,
        ) => {
          return visibleOptions.length === 0 ? null : (
            <div data-testid="dropdown">
              <IconButton
                {...triggerProps}
                type="button"
                size="sm"
                color={appearance.buttonColor}
                variant={appearance.dropdownVariant}
                icon={<IconOptions />}
              />
              <Dropdown.Menu>
                {visibleOptions.flatMap((option, index, array) => {
                  const elements = [
                    <Dropdown.Item
                      key={option.id}
                      icon={option.icon}
                      onClick={option.action}
                    >
                      {option.label}
                    </Dropdown.Item>,
                  ];

                  // Separator
                  const separatorAfterIds = ['replyall', 'transfer'];
                  if (
                    separatorAfterIds.includes(option.id) &&
                    array
                      .slice(index + 1)
                      .some((o) => !separatorAfterIds.includes(o.id))
                  ) {
                    elements.push(
                      <Dropdown.Separator key={`separator-${option.id}`} />,
                    );
                  }

                  return elements;
                })}
              </Dropdown.Menu>
            </div>
          );
        }}
      </Dropdown>
      {openedIncativeUsersModal && (
        <SentToInactiveUsersModal
          open={openedIncativeUsersModal}
          onModalClose={handleCloseInactiveUsersModal}
          users={inactiveUsers}
        />
      )}
      {openedUndeliveredUsersModal && (
        <UndeliveredUsersModal
          open={openedUndeliveredUsersModal}
          onModalClose={handleCloseUndeliveredUsersModal}
          users={undeliveredUsers}
        />
      )}
    </div>
  );
}
