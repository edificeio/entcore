import { Button, Modal } from '@edifice.io/react';
import { useI18n } from '~/hooks/useI18n';

export interface SentToInactiveUsersModalProps {
  users: string[];
  total: number;
  onModalClose: () => void;
}

export function SentToInactiveUsersModal({
  users,
  total,
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

          {users.length < total ? (
            <div className="mt-16">
              <div>
                <strong>{t('warning.inactive.total', { total })}</strong>
              </div>
              <i>{t('warning.inactive.console')}</i>
            </div>
          ) : (
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
          )}
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
