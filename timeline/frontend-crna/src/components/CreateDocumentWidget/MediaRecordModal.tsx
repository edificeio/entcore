import { Modal } from '@edifice.io/react';
import { AudioRecorder, VideoRecorder } from '@edifice.io/react/multimedia';
import type { WorkspaceElement } from '@edifice.io/client';
import { useTranslation } from 'react-i18next';

interface MediaRecordModalProps {
  type: 'video' | 'audio';
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function MediaRecordModal({ type, isOpen, onClose, onSuccess }: MediaRecordModalProps) {
  const { t } = useTranslation();

  if (!isOpen) return null;

  const handleAudioSave = (_resource: WorkspaceElement) => { onSuccess(); onClose(); };
  const handleVideoSuccess = (_resources: WorkspaceElement[]) => { onSuccess(); onClose(); };

  return (
    <Modal id="media-record-modal" isOpen={isOpen} onModalClose={onClose} size="md">
      <Modal.Header onModalClose={onClose}>
        {type === 'video'
          ? t('homepage.widget.create.video.title', 'Enregistrer une vidéo')
          : t('homepage.widget.create.audio.title', 'Enregistrer un audio')}
      </Modal.Header>
      <Modal.Body>
        {type === 'audio' ? (
          <AudioRecorder
            visibility="protected"
            onSaveSuccess={handleAudioSave}
          />
        ) : (
          <VideoRecorder
            appCode="workspace"
            onSuccess={handleVideoSuccess}
            onError={(err) => console.error('[VideoRecorder]', err)}
          />
        )}
      </Modal.Body>
    </Modal>
  );
}
