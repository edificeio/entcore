import { FormControl, Input, Label, Modal, RadioCard } from '@edifice.io/react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { DOC_TYPES, type DocTypeId } from '~/models/createDocument';
import { ButtonBeta } from '@edifice.io/react';

export interface CreateDocumentModalProps {
  isOpen: boolean;
  docTypeId: DocTypeId;
  nextcloudAddress: string | undefined;
  onClose: () => void;
}

export function CreateDocumentModal({
  isOpen,
  docTypeId,
  nextcloudAddress,
  onClose,
}: CreateDocumentModalProps) {
  const { t } = useTranslation('timeline');

  const [selectedDocTypeId, setSelectedDocTypeId] =
    useState<DocTypeId>(docTypeId);
  const [filename, setFilename] = useState('');

  // Sync pre-selection when modal opens with a new docTypeId
  useEffect(() => {
    if (isOpen) {
      setSelectedDocTypeId(docTypeId);
      setFilename('');
    }
  }, [isOpen, docTypeId]);

  const handleCreate = () => {
    const trimmed = filename.trim();
    if (!trimmed || !nextcloudAddress) return;

    const redirectUrlMarkerIndex = nextcloudAddress.indexOf('redirectUrl=');
    if (redirectUrlMarkerIndex === -1) return;

    const redirectUrl = `/index.php/apps/edifice_documents/create?type=${selectedDocTypeId}&name=${encodeURIComponent(trimmed)}`;
    const addressPrefix = nextcloudAddress.slice(0, redirectUrlMarkerIndex);
    const oidcUrl = `${addressPrefix}redirectUrl=${encodeURIComponent(redirectUrl)}`;

    window.open(oidcUrl, '_blank', 'noopener,noreferrer');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <Modal
      id="create-document-modal"
      isOpen={isOpen}
      onModalClose={onClose}
      size="md"
    >
      <Modal.Header onModalClose={onClose}>
        {t('homepage.crna.widget.create.modal.title', 'Créer un document')}
      </Modal.Header>

      <Modal.Body>
        <div className="d-flex flex-column gap-24">
          <div className="d-flex gap-12">
            {DOC_TYPES.map((dt) => (
              <RadioCard
                key={dt.id}
                groupName="create-document-doc-type"
                value={dt.id}
                label={dt.label}
                selectedValue={selectedDocTypeId}
                onChange={() => setSelectedDocTypeId(dt.id)}
              />
            ))}
          </div>

          <FormControl id="create-document-filename">
            <Label>
              {t(
                'homepage.crna.widget.create.modal.filename',
                'Nom du document',
              )}
            </Label>
            <Input
              type="text"
              value={filename}
              onChange={(e) => setFilename(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
              placeholder={t(
                'homepage.crna.widget.create.modal.filename.placeholder',
                'Saisissez un nom…',
              )}
              size="md"
            />
          </FormControl>
        </div>
      </Modal.Body>

      <Modal.Footer>
        <ButtonBeta color="tertiary" variant="ghost" onClick={onClose}>
          {t('homepage.crna.widget.create.modal.cancel', 'Annuler')}
        </ButtonBeta>
        <ButtonBeta
          color="default"
          disabled={!filename.trim() || !nextcloudAddress}
          onClick={handleCreate}
        >
          {t('homepage.crna.widget.create.modal.create', 'Créer')}
        </ButtonBeta>
      </Modal.Footer>
    </Modal>
  );
}
