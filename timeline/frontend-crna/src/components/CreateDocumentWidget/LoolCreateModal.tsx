import { FormControl, Input, Label, Modal, RadioCard } from '@edifice.io/react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useLoolProviders, type LoolDocTypeId } from '~/hooks/useLoolProviders';
import { ButtonBeta } from '@edifice.io/react';

const DOC_TYPE_TO_EXT: Record<LoolDocTypeId, 'docx' | 'pptx' | 'xlsx'> = {
  word: 'docx',
  powerpoint: 'pptx',
  excel: 'xlsx',
};

export interface LoolCreateModalProps {
  isOpen: boolean;
  docTypeId: LoolDocTypeId;
  onClose: () => void;
}

export function LoolCreateModal({ isOpen, docTypeId, onClose }: LoolCreateModalProps) {
  const { t } = useTranslation();
  const { data: docTypes = [], isLoading } = useLoolProviders();

  const [selectedDocTypeId, setSelectedDocTypeId] = useState<LoolDocTypeId>(docTypeId);
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
    if (!trimmed) return;
    const params = new URLSearchParams({
      type: DOC_TYPE_TO_EXT[selectedDocTypeId],
      name: trimmed.replace(/[/\\<>|]/g, ''),
      protected: 'false',
    });
    window.open(`/lool/document?${params.toString()}`, '_blank');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <Modal id="lool-create-modal" isOpen={isOpen} onModalClose={onClose} size="md">
      <Modal.Header onModalClose={onClose}>
        {t('homepage.widget.create.modal.title', 'Créer un document')}
      </Modal.Header>

      <Modal.Body>
        <div className="d-flex flex-column gap-24">
          {isLoading ? (
            <p>{t('homepage.widget.create.modal.loading', 'Chargement…')}</p>
          ) : (
            <div className="d-flex gap-12">
              {docTypes.map((dt) => (
                <RadioCard
                  key={dt.id}
                  groupName="lool-doc-type"
                  value={dt.id}
                  label={dt.label}
                  selectedValue={selectedDocTypeId}
                  onChange={() => setSelectedDocTypeId(dt.id)}
                />
              ))}
            </div>
          )}

          <FormControl id="lool-filename">
            <Label>
              {t('homepage.widget.create.modal.filename', 'Nom du document')}
            </Label>
            <Input
              type="text"
              value={filename}
              onChange={(e) => setFilename(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
              placeholder={t('homepage.widget.create.modal.filename.placeholder', 'Saisissez un nom…')}
              size="md"
            />
          </FormControl>
        </div>
      </Modal.Body>

      <Modal.Footer>
        <ButtonBeta color="tertiary" variant="ghost" onClick={onClose}>
          {t('homepage.widget.create.modal.cancel', 'Annuler')}
        </ButtonBeta>
        <ButtonBeta
          color="default"
          disabled={!filename.trim()}
          onClick={handleCreate}
        >
          {t('homepage.widget.create.modal.create', 'Créer')}
        </ButtonBeta>
      </Modal.Footer>
    </Modal>
  );
}
