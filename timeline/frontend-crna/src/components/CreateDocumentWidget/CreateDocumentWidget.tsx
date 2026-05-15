import { ButtonBeta, Flex, IconButton } from '@edifice.io/react';
import {
  IconExternalLink,
  IconMic,
  IconRecordVideo,
} from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { WidgetPanel } from '../WidgetPanel';
import './CreateDocumentWidget.css';

const CREATE_ACTIONS = [
  {
    labelKey: `('tiptap.toolbar.text', 'Document Texte')`,
    icon: 'W',
    colorVar: 'blue',
    href: '/collaborative',
  },
  {
    labelKey: `('tiptap.toolbar.presentation', 'Présentation')`,
    icon: 'P',
    colorVar: 'orange',
    href: '/mindmap',
  },
  {
    labelKey: `('tiptap.toolbar.spreadsheet', 'Classeur')`,
    icon: 'X',
    colorVar: 'green',
    href: '/formulaire',
  },
  {
    labelKey: `('tiptap.toolbar.video', 'Ajout Vidéo')`,
    icon: (
      <IconRecordVideo
        width={20}
        height={20}
        color="var(--edifice-yellow-800)"
      />
    ),
    colorVar: 'yellow',
    href: '/formulaire',
  },
  {
    labelKey: `('tiptap.toolbar.audio', 'Ajout Audio')`,
    icon: <IconMic width={20} height={20} color="var(--edifice-pink-800)" />,
    colorVar: 'pink',
    href: '/mindmap',
  },
] as const;

export function CreateDocumentWidget() {
  const { t } = useTranslation();

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
          {CREATE_ACTIONS.map(({ labelKey, icon, colorVar, href }) => (
            <IconButton
              key={labelKey}
              onClick={() => window.open(href, '_self')}
              title={t(labelKey)}
              className="create-document-btn"
              aria-label={t(labelKey)}
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
          ))}
        </Flex>
      </div>
    </WidgetPanel>
  );
}
