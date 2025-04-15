import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import { IconOptions, IconSignature } from '@edifice.io/react/icons';
import { Fragment, RefAttributes } from 'react';
import { useLocation } from 'react-router-dom';
import { NewMessageButton } from '~/components/NewMessageButton';
import { useI18n } from '~/hooks';
import { useAppActions } from '~/store';
import { AppActionMenuOptions } from './AppActionMenuOptions';

export const AppActionHeader = () => {
  const { t, common_t } = useI18n();
  const { setOpenedModal } = useAppActions();
  const location = useLocation();
  const draftRoute = '/draft/create';

  const isDraft = location.pathname === draftRoute;

  const dropdownOptions: AppActionMenuOptions[] = [
    {
      id: 'signature',
      label: t('signature.menu.label'),
      icon: <IconSignature />,
      action: () => setOpenedModal('signature'),
      visibility: true,
    },
  ];

  return (
    <>
      {!isDraft && (
        <div className="d-flex flex-fill align-items-center justify-content-end gap-12 align-self-end">
          <NewMessageButton />

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
      )}
    </>
  );
};
