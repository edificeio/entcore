import { Flex, IconButton, useHasWorkflow } from '@edifice.io/react';
import {
  CaptationAudio,
  CaptationVideo,
  HomeCard,
} from '@edifice.io/react/homepage';
import {
  IconArrowRight,
  IconMic,
  IconRecordVideo,
} from '@edifice.io/react/icons';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { DocTypeId } from '~/models/createDocument';
import { CreateDocumentModal } from './CreateDocumentModal';
import './CreateDocumentWidget.css';

const DOC_TYPE_ACTIONS = [
  {
    labelKey: 'tiptap.toolbar.text',
    label: 'Document Texte',
    icon: 'W',
    colorVar: 'blue',
    docTypeId: 'word' as DocTypeId,
  },
  {
    labelKey: 'tiptap.toolbar.presentation',
    label: 'Présentation',
    icon: 'P',
    colorVar: 'orange',
    docTypeId: 'powerpoint' as DocTypeId,
  },
  {
    labelKey: 'tiptap.toolbar.spreadsheet',
    label: 'Classeur',
    icon: 'X',
    colorVar: 'green',
    docTypeId: 'excel' as DocTypeId,
  },
] as const;

const APP_ACTIONS = [
  {
    labelKey: 'tiptap.toolbar.video',
    label: 'Ajout Vidéo',
    icon: (
      <IconRecordVideo
        width={20}
        height={20}
        color="var(--edifice-yellow-800)"
      />
    ),
    colorVar: 'yellow',
    mediaType: 'video' as const,
    workflowKey:
      'com.opendigitaleducation.video.controllers.VideoController|capture',
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

type MediaType = (typeof APP_ACTIONS)[number]['mediaType'];

export function CreateDocumentWidget() {
  const { t } = useTranslation('timeline');
  const hasLoolRight = useHasWorkflow(
    'fr.openent.lool.controller.LoolController|createDocumentFromTemplate',
  );
  const hasVideoRight = useHasWorkflow(
    'com.opendigitaleducation.video.controllers.VideoController|capture',
  );
  const [selectedDocTypeId, setSelectedDocTypeId] = useState<DocTypeId | null>(
    null,
  );
  const [mediaRecordType, setMediaRecordType] = useState<MediaType | null>(
    null,
  );

  const closeMediaRecord = () => setMediaRecordType(null);

  return (
    <HomeCard variant="secondary">
      <HomeCard.Header
        title={t('homepage.crna.widget.create.title', 'Créer un document')}
        actionLabel={t('homepage.crna.widget.see.all', 'Voir tout')}
        onActionClick={() => window.open('/workspace', '_self')}
        actionRightIcon={<IconArrowRight />}
        actionProps={{ 'data-testid': 'create-document-see-all-button' }}
      />
      <HomeCard.Content>
        <div className="create-document-apps-card">
          <Flex gap="8" align="center">
            {hasLoolRight === true &&
              DOC_TYPE_ACTIONS.map(
                ({ labelKey, label, icon, colorVar, docTypeId }) => (
                  <IconButton
                    key={labelKey}
                    onClick={() => setSelectedDocTypeId(docTypeId)}
                    title={t(labelKey, label)}
                    className="create-document-btn"
                    aria-label={t(labelKey, label)}
                    data-testid={`create-document-${docTypeId}-button`}
                    style={
                      {
                        'background': `var(--edifice-${colorVar}-200)`,
                        'color': `var(--edifice-${colorVar}-800)`,
                        '--edifice-btn-border-color': `var(--edifice-${colorVar}-800)`,
                        '--edifice-btn-hover-border-color': `var(--edifice-${colorVar}-800)`,
                      } as React.CSSProperties
                    }
                    icon={icon}
                  />
                ),
              )}

            {APP_ACTIONS.map(
              ({ labelKey, label, icon, colorVar, mediaType, workflowKey }) => {
                if (workflowKey && hasVideoRight !== true) return null;
                return (
                  <IconButton
                    key={labelKey}
                    onClick={() => setMediaRecordType(mediaType)}
                    title={t(labelKey, label)}
                    className="create-document-btn"
                    aria-label={t(labelKey, label)}
                    data-testid={`create-document-${mediaType}-button`}
                    style={
                      {
                        'background': `var(--edifice-${colorVar}-200)`,
                        'color': `var(--edifice-${colorVar}-800)`,
                        '--edifice-btn-border-color': `var(--edifice-${colorVar}-800)`,
                        '--edifice-btn-hover-border-color': `var(--edifice-${colorVar}-800)`,
                      } as React.CSSProperties
                    }
                    icon={icon}
                  />
                );
              },
            )}
          </Flex>
        </div>

        {selectedDocTypeId && (
          <CreateDocumentModal
            isOpen={true}
            docTypeId={selectedDocTypeId}
            onClose={() => setSelectedDocTypeId(null)}
          />
        )}

        {/* Mounted only while open: the recorders request camera/mic access on mount. */}
        {mediaRecordType === 'video' && (
          <CaptationVideo
            isOpen={true}
            onClose={closeMediaRecord}
            appCode="workspace"
          />
        )}
        {mediaRecordType === 'audio' && (
          <CaptationAudio isOpen={true} onClose={closeMediaRecord} />
        )}
      </HomeCard.Content>
    </HomeCard>
  );
}
