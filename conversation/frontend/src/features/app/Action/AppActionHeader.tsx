import {
  Button,
  Dropdown,
  IconButton,
  IconButtonProps,
  useEdificeClient,
} from '@edifice.io/react';
import { IconOptions, IconPlus, IconPrint } from '@edifice.io/react/icons';
import { Fragment, RefAttributes } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useI18n } from '~/hooks';
import { AppActionMenuOptions } from './AppActionMenuOptions';

export const AppActionHeader = () => {
  const { appCode } = useEdificeClient();
  const { t, common_t } = useI18n();
  const navigate = useNavigate();
  const location = useLocation();
  const draftRoute = '/draft/create';

  const isDraft = location.pathname === draftRoute;

  const dropdownOptions: AppActionMenuOptions[] = [
    {
      id: 'print',
      label: t('print'),
      icon: <IconPrint />,
      action: () => {
        console.log(appCode);
      },
      visibility: true,
    },
  ];

  const handleCreateNewClick = () => {
    navigate(draftRoute);
  };

  return (
    <>
      {!isDraft && (
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
      )}
    </>
  );
};
