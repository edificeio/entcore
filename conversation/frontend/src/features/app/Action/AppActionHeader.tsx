import { Dropdown, IconButton, IconButtonProps } from '@edifice.io/react';
import {
  IconCalendar,
  IconSettings,
  IconSignature,
} from '@edifice.io/react/icons';
import { Fragment, RefAttributes, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { NewMessageButton } from '~/components/NewMessageButton';
import { AbsenceModal } from '~/features';
import { useI18n } from '~/hooks/useI18n';
import { useRights } from '~/hooks/useRights';
import { useActionsStore } from '~/store/actions';
import { AppActionMenuOptions } from './AppActionMenuOptions';

export function AppActionHeader() {
  const { t, common_t } = useI18n();
  const { canManageAbsence } = useRights();
  const setOpenedModal = useActionsStore.use.setOpenedModal();
  const location = useLocation();
  const draftRoute = '/draft/create';

  const isDraft = location.pathname === draftRoute;

  const [isAbsenceModalOpen, setIsAbsenceModalOpen] = useState(false);

  const dropdownOptions: AppActionMenuOptions[] = [
    {
      id: 'signature',
      label: t('signature.menu.label'),
      icon: <IconSignature />,
      action: () => setOpenedModal('signature'),
      visibility: true,
    },
    {
      id: 'absence',
      label: t('conversation.absence.menu.label'),
      icon: <IconCalendar />,
      action: () => setIsAbsenceModalOpen(true),
      visibility: canManageAbsence,
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
                  icon={<IconSettings />}
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

      {isAbsenceModalOpen && (
        <AbsenceModal
          isOpen={isAbsenceModalOpen}
          onModalClose={() => setIsAbsenceModalOpen(false)}
        />
      )}
    </>
  );
}
