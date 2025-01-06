import { Fragment, RefAttributes } from 'react';

import { Options, Plus, Print } from '@edifice-ui/icons';
import {
  Button,
  Dropdown,
  IconButton,
  IconButtonProps,
  useOdeClient,
} from '@edifice-ui/react';
import { useTranslation } from 'react-i18next';
import { AppActionMenuOptions } from './AppActionMenuOptions';
import { useAppActions } from '~/store/actions';

export const AppActionHeader = () => {
  const { appCode } = useOdeClient();
  const { t } = useTranslation(appCode);
  const { t: common_t } = useTranslation('common');
  const { setOpenPrintModal } = useAppActions();

  const dropdownOptions: AppActionMenuOptions[] = [
    {
      id: 'print',
      label: t('print'),
      icon: <Print />,
      action: () => setOpenPrintModal(true),
      visibility: true,
    },
  ];

  const handleCreateNewClick = () => {
    alert('Create new');
  };

  return (
    <div className="d-flex flex-fill align-items-center justify-content-end gap-12 align-self-end">
      <Button
        leftIcon={<Plus />}
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
              icon={<Options />}
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
