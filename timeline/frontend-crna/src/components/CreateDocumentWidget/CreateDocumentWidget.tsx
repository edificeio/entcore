import { Alert, ButtonBeta, Flex, IconButton, useHasWorkflow } from '@edifice.io/react';
import {
  IconExternalLink,
  IconMic,
  IconRecordVideo,
} from '@edifice.io/react/icons';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { LoolDocTypeId } from '~/models/createDocument';
import { WidgetPanel } from '../ui/WidgetPanel';
import { LoolCreateModal } from './LoolCreateModal';
import { MediaRecordModal } from './MediaRecordModal';
import './CreateDocumentWidget.css';

const LOOL_ACTIONS = [
  { labelKey: 'tiptap.toolbar.text',         label: 'Document Texte', icon: 'W', colorVar: 'blue',   docTypeId: 'word'        as LoolDocTypeId },
  { labelKey: 'tiptap.toolbar.presentation', label: 'Présentation',   icon: 'P', colorVar: 'orange', docTypeId: 'powerpoint'  as LoolDocTypeId },
  { labelKey: 'tiptap.toolbar.spreadsheet',  label: 'Classeur',       icon: 'X', colorVar: 'green',  docTypeId: 'excel'       as LoolDocTypeId },
] as const;

const APP_ACTIONS = [
  {
    labelKey: 'tiptap.toolbar.video',
    label: 'Ajout Vidéo',
    icon: <IconRecordVideo width={20} height={20} color="var(--edifice-yellow-800)" />,
    colorVar: 'yellow',
    mediaType: 'video' as const,
    workflowKey: 'com.opendigitaleducation.video.controllers.VideoController|capture',
  },
  {
    labelKey: 'tiptap.toolbar.audio',
    label: 'Ajout Audio',
    icon: <IconMic width={20} height={20} color="var(--edifice-pink-800)" />,
    colorVar: 'pink',
    mediaType: 'audio' as const,
    workflowKey: null,
  },
] as const;

export function CreateDocumentWidget() {
  const { t } = useTranslation();
  const hasLoolRight = useHasWorkflow("fr.openent.lool.controller.LoolController|createDocumentFromTemplate");
  const hasVideoRight = useHasWorkflow("com.opendigitaleducation.video.controllers.VideoController|capture");
  const [selectedDocTypeId, setSelectedDocTypeId] = useState<LoolDocTypeId | null>(null);
  const [mediaRecordType, setMediaRecordType] = useState<'video' | 'audio' | null>(null);
  const [successType, setSuccessType] = useState<'video' | 'audio' | null>(null);

  return (
    <WidgetPanel
      title={t('homepage.widget.create.title', 'Créer un document')}
      action={
        <ButtonBeta
          color="default"
          variant="ghost"
          onClick={() => window.open('/welcome', '_self')}
          rightIcon={<IconExternalLink />}
        >
          {t('homepage.widget.see.all', 'Voir tout')}
        </ButtonBeta>
      }
    >
      <div className="create-document-apps-card">
        <Flex gap="8" align="center">
          {hasLoolRight === true &&
            LOOL_ACTIONS.map(({ labelKey, label, icon, colorVar, docTypeId }) => (
              <IconButton
                key={labelKey}
                onClick={() => setSelectedDocTypeId(docTypeId)}
                title={t(labelKey, label)}
                className="create-document-btn"
                aria-label={t(labelKey, label)}
                style={
                  {
                    background: `var(--edifice-${colorVar}-200)`,
                    color: `var(--edifice-${colorVar}-800)`,
                    '--edifice-btn-border-color': `var(--edifice-${colorVar}-800)`,
                    '--edifice-btn-hover-border-color': `var(--edifice-${colorVar}-800)`,
                  } as React.CSSProperties
                }
                icon={icon}
              />
            ))}

          {APP_ACTIONS.map(({ labelKey, label, icon, colorVar, mediaType, workflowKey }) => {
            if (workflowKey && hasVideoRight !== true) return null;
            return (
              <IconButton
                key={labelKey}
                onClick={() => setMediaRecordType(mediaType)}
                title={t(labelKey, label)}
                className="create-document-btn"
                aria-label={t(labelKey, label)}
                style={
                  {
                    background: `var(--edifice-${colorVar}-200)`,
                    color: `var(--edifice-${colorVar}-800)`,
                    '--edifice-btn-border-color': `var(--edifice-${colorVar}-800)`,
                    '--edifice-btn-hover-border-color': `var(--edifice-${colorVar}-800)`,
                  } as React.CSSProperties
                }
                icon={icon}
              />
            );
          })}
        </Flex>
      </div>

      {selectedDocTypeId && (
        <LoolCreateModal
          isOpen={true}
          docTypeId={selectedDocTypeId}
          onClose={() => setSelectedDocTypeId(null)}
        />
      )}

      {successType && (
        <Alert
          type="success"
          isToast
          position="top-right"
          isDismissible
          autoClose
          onClose={() => setSuccessType(null)}
        >
          {successType === 'video'
            ? t('homepage.widget.create.video.success', 'Votre vidéo a été enregistrée avec succès.')
            : t('homepage.widget.create.audio.success', 'Votre audio a été enregistré avec succès.')}
        </Alert>
      )}

      {mediaRecordType && (
        <MediaRecordModal
          type={mediaRecordType}
          isOpen={true}
          onClose={() => setMediaRecordType(null)}
          onSuccess={() => setSuccessType(mediaRecordType)}
        />
      )}
    </WidgetPanel>
  );
}
