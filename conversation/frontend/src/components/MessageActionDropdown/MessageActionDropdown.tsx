import {
  Button,
  ButtonColors,
  ButtonVariants,
  Dropdown,
  IconButton,
  IconButtonProps,
  Tooltip,
  useBreakpoint,
} from '@edifice.io/react';
import { IconOptions } from '@edifice.io/react/icons';
import { RefAttributes, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SentToInactiveUsersModal } from '~/components/MessageActionDropdown/modals/SentToInactiveUsersModal';
import { Message } from '~/models';
import { useMessageActionDropdown } from './hooks/useMessageActionDropdown';

export interface MessageActionDropdownProps {
  message: Message;
  appearance?: {
    mainButtonVariant?: ButtonVariants;
    dropdownVariant?: ButtonVariants;
    buttonColor?: ButtonColors;
  };
  actions?: string[];
  className?: string;
}

export function MessageActionDropdown({
  message,
  actions,
  appearance = {
    mainButtonVariant: 'outline',
    dropdownVariant: 'outline',
    buttonColor: 'primary',
  },
  className,
}: MessageActionDropdownProps) {
  const [inactiveUsers, setInactiveUsers] = useState<string[] | undefined>();
  const { md } = useBreakpoint();

  const { actionButtons, dropdownOptions } = useMessageActionDropdown({
    message,
    actions,
    setInactiveUsers,
  });
  const navigate = useNavigate();

  const visibleOptions = dropdownOptions.filter(
    (o) => !o.hidden && !actionButtons.some((a) => !a.hidden && a.id === o.id),
  );

  const handleCloseInactiveUsersModal = () => {
    setInactiveUsers(undefined);
    navigate(`/inbox`);
  };

  const classNameContainer = `d-flex ${className}`;

  return (
    <div className={classNameContainer}>
      {actionButtons
        .filter((o) => !o.hidden)
        .map((option) => (
          <Tooltip
            key={option.id}
            message={md ? undefined : option.label}
            placement="top"
          >
            <Button
              color={appearance.buttonColor}
              variant={appearance.mainButtonVariant}
              leftIcon={option.icon}
              onClick={option.action}
              aria-label={option.label}
              disabled={option.disabled}
            >
              {md && option.label}
            </Button>
          </Tooltip>
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
      {inactiveUsers?.length && (
        <SentToInactiveUsersModal
          onModalClose={handleCloseInactiveUsersModal}
          users={inactiveUsers}
        />
      )}
    </div>
  );
}
