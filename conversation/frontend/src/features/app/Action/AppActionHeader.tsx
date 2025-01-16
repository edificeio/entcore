import { Fragment, RefAttributes } from 'react';
import { useEdificeClient } from '@edifice.io/react';
import { IconOptions, IconPlus, IconPrint } from '@edifice.io/react/icons';
import {
  Button,
  Dropdown,
  IconButton,
  IconButtonProps,
} from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { AppActionMenuOptions } from './AppActionMenuOptions';

export const AppActionHeader = () => {
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);
  const { t: common_t } = useTranslation('common');

  const dropdownOptions: AppActionMenuOptions[] = [
    {
      id: 'print',
      label: t('print'),
      icon: <IconPrint />,
      action: () => {console.log(appCode)},
      visibility: true,
    },
  ];

  const handleCreateNewClick = () => {
    alert('Create new');
  };

  return (
    <div className="d-flex flex-fill align-items-center justify-content-end gap-12 align-self-end">
      <Button
        leftIcon={<IconPlus />}
        onClick={handleCreateNewClick}
        className="text-nowrap"
      >
        {t('new.message')}
      </Button>

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
              aria-label={common_t('tiptap.tooltip.plus')}
              color="primary"
              variant="outline"
              icon={<IconOptions />}
            />

            <Dropdown.Menu>
              {dropdownOptions.map((option) => (
                <Fragment key={option.id}>
                  {option.type === 'divider' ? (
                    <Dropdown.Separator />
                  ) : (
                    option.visibility && (
                      <Dropdown.Item
                        icon={option.icon}
                        onClick={() => option.action(null)}
                      >
                        {option.label}
                      </Dropdown.Item>
                    )
                  )}
                </Fragment>
              ))}
            </Dropdown.Menu>
          </div>
        )}
      </Dropdown>
    </div>
  );
};
