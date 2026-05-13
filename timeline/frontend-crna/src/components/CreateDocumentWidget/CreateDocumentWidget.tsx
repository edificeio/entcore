import { ButtonBeta, Flex } from '@edifice.io/react';
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
    labelKey: 'Writer',
    icon: 'W',
    colorVar: '#2B6CBF',
    href: '/collaborative',
  },
  {
    labelKey: 'Présentation',
    icon: 'P',
    colorVar: '#E07A5F',
    href: '/mindmap',
  },
  { labelKey: 'Tableur', icon: 'X', colorVar: '#3D9A6D', href: '/formulaire' },
  {
    labelKey: 'Formulaire',
    icon: <IconRecordVideo width={20} height={20} />,
    colorVar: '#7B61FF',
    href: '/formulaire',
  },
  {
    labelKey: 'Carte mentale',
    icon: <IconMic width={20} height={20} />,
    colorVar: '#F2A541',
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
            <a
              key={labelKey}
              href={href}
              title={labelKey}
              className="create-document-btn"
              style={{ background: colorVar }}
            >
              {icon}
            </a>
          ))}
        </Flex>
      </div>
    </WidgetPanel>
  );
}
