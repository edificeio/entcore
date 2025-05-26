import { useId, useState } from 'react';
import { Button, LoadingScreen, Modal } from '@edifice.io/react';
import { useI18n } from '~/hooks/useI18n';
import { baseUrl } from '~/services';

interface OriginalFormatModalProps {
  messageId: string;
  isOpen: boolean;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export default function OriginalFormatModal({
  messageId,
  isOpen,
  onCancel = () => ({}),
}: OriginalFormatModalProps) {
  const { t, common_t } = useI18n();
  const [isLoaded, setIsLoaded] = useState(false);

  return (
    <Modal
      viewport
      isOpen={isOpen}
      onModalClose={onCancel}
      id={useId()}
      size="lg"
    >
      <Modal.Header onModalClose={onCancel}>
        {t('message.modal.original.title')}
      </Modal.Header>
      <Modal.Body className="d-flex flex-fill align-content-center justify-content-center ">
        {!isLoaded && (
          <div className="position-absolute top-0 start-0 bottom-0 end-0 d-flex align-items-center justify-content-center bg-white z-index-1">
            <LoadingScreen />
          </div>
        )}
        <iframe
          className="flex-fill"
          src={`${baseUrl}/oldformat/${messageId}`}
          title={t('message.modal.original.title')}
          onLoad={() => setIsLoaded(true)}
        ></iframe>
      </Modal.Body>
      <Modal.Footer>
        <Button color="secondary" onClick={onCancel}>
          {common_t('close')}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
