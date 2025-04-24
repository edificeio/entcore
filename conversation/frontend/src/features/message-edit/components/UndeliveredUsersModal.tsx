import { Button, Modal } from '@edifice.io/react';
import { useI18n } from '~/hooks';

export interface UndeliveredUsersModalProps {
  open: boolean;
  users: string[];
  onModalClose: () => void;
}

export function UndeliveredUsersModal({
  open,
  users,
  onModalClose,
}: UndeliveredUsersModalProps) {
  const { t } = useI18n();

  return (
    <Modal
      size="sm"
      id="modalSentToInactiveUsers"
      isOpen={open}
      onModalClose={onModalClose}
    >
      <Modal.Header onModalClose={onModalClose}>
        {t('warning.undelivered.title')}
      </Modal.Header>
      <Modal.Body>
        <div>
          <div>{t('warning.undelivered.text')}</div>
          <div>
            <div>
              <strong>{t('warning.undelivered.users')}</strong>
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
          onClick={onModalClose}
        >
          {t('warning.undelivered.action')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
