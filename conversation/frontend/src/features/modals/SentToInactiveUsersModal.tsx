import { Button, Modal } from '@edifice.io/react';
import { useI18n } from '~/hooks';
import { useAppActions, useInactiveUsers } from '~/store';

export function SentToInactiveUsersModal() {
  const { setOpenedModal } = useAppActions();
  const users = useInactiveUsers();
  const { t, common_t } = useI18n();

  const handleCloseFolderModal = () => setOpenedModal(undefined);

  return (
    <Modal
      size="sm"
      id="modalSentToInactiveUsers"
      isOpen={true}
      onModalClose={handleCloseFolderModal}
    >
      <Modal.Header onModalClose={handleCloseFolderModal}>
        {t('warning.inactive.title')}
      </Modal.Header>
      <Modal.Body>
        <div>
          <div>{t('warning.inactive.text')}</div>
          <div>
            <div>
              <strong>{t('warning.inactive.users')}</strong>
            </div>
            <li className="list-group">
              {users.map((user) => (
                <ul className="ps-0 mb-4" key={user}>
                  <span>{user}</span>
                </ul>
              ))}
            </li>
          </div>
        </div>
      </Modal.Body>
      <Modal.Footer>
        <Button
          type="button"
          color="primary"
          variant="filled"
          onClick={handleCloseFolderModal}
        >
          {common_t('warning.inactive.action')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
