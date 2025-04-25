import { Button, Modal } from '@edifice.io/react';
import { useI18n } from '~/hooks';

export interface SentToInactiveUsersModalProps {
  users: string[];
  onModalClose: () => void;
}

export function SentToInactiveUsersModal({
  users,
  onModalClose,
}: SentToInactiveUsersModalProps) {
  const { t } = useI18n();

  return (
    <Modal
      size="sm"
      id="modalSentToInactiveUsers"
      isOpen={true}
      onModalClose={onModalClose}
    >
      <Modal.Header onModalClose={onModalClose}>
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
          onClick={onModalClose}
        >
          {t('warning.inactive.action')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
