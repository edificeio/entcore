import { Button, Modal } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';

const Variant = {
  YES_NO: 'yes/no',
  OK_CANCEL: 'ok/cancel',
} as const;

export type ConfirmModalVariant = (typeof Variant)[keyof typeof Variant];

interface ConfirmModalProps {
  variant?: ConfirmModalVariant;
  id: string;
  isOpen: boolean;
  header: JSX.Element;
  body: JSX.Element;
  okText?: string;
  koText?: string;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export default function ConfirmModal({
  variant = Variant.YES_NO,
  id,
  isOpen,
  header,
  body,
  okText,
  koText,
  onSuccess = () => ({}),
  onCancel = () => ({}),
}: ConfirmModalProps) {
  const { t } = useTranslation('common');
  const ok = { 'yes/no': t('yes'), 'ok/cancel': t('ok') };
  const ko = { 'yes/no': t('no'), 'ok/cancel': t('cancel') };

  return (
    <Modal isOpen={isOpen} onModalClose={onCancel} id={id}>
      <Modal.Header onModalClose={onCancel}>{header}</Modal.Header>
      <Modal.Body>{body}</Modal.Body>
      <Modal.Footer>
        <Button
          color="tertiary"
          onClick={onCancel}
          type="button"
          variant="ghost"
        >
          {koText ? t(koText) : ko[variant]}
        </Button>
        <Button
          color="danger"
          onClick={onSuccess}
          type="button"
          variant="filled"
        >
          {okText ? t(okText) : ok[variant]}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
