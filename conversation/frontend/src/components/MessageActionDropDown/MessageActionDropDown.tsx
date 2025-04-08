import {
  Button,
  Dropdown,
  IconButton,
  IconButtonProps,
} from '@edifice.io/react';
import { IconOptions } from '@edifice.io/react/icons';
import { RefAttributes } from 'react';
import { Message } from '~/models';
import { useMessageActionDropDown } from './hooks/useMessageActionDropDown';

export interface MessageActionDropDownProps {
  message: Message;
  appearance?: {
    variant?: 'outline' | 'ghost';
    btnColor?: 'tertiary' | 'primary';
  };
  actions?: string[];
}

export function MessageActionDropDown({
  message,
  actions,
  appearance = {
    variant: 'outline',
    btnColor: 'primary',
  },
}: MessageActionDropDownProps) {
  const { actionButtons, dropdownOptions } = useMessageActionDropDown(
    message,
    actions,
  );

  const visibleOptions = dropdownOptions.filter((o) => !o.hidden);

  return (
    <div className="d-flex align-items-center gap-12">
      {actionButtons
        .filter((o) => !o.hidden)
        .map((option) => (
          <Button
            key={option.id}
            color={appearance.btnColor}
            variant={appearance.variant}
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
                color={appearance.btnColor}
                variant={appearance.variant}
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
    </div>
  );
}
