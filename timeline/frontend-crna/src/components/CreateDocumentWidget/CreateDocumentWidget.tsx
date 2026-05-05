import { Button, Flex } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { WidgetCard } from '../WidgetCard';
import './CreateDocumentWidget.css';

const CREATE_ACTIONS = [
  { labelKey: 'Writer', icon: 'W', color: '#2B6CBF', href: '/collaborative' },
  { labelKey: 'Présentation', icon: 'P', color: '#E07A5F', href: '/mindmap' },
  { labelKey: 'Tableur', icon: 'X', color: '#3D9A6D', href: '/formulaire' },
  { labelKey: 'Formulaire', icon: '⊞', color: '#7B61FF', href: '/formulaire' },
  { labelKey: 'Carte mentale', icon: '⊕', color: '#F2A541', href: '/mindmap' },
] as const;

export function CreateDocumentWidget() {
  const { t } = useTranslation();

  return (
    <WidgetCard
      className="create-document-widget"
      title={t('homepage.widget.create.title', 'Créer un document')}
      action={
        <Button
          color="tertiary"
          variant="ghost"
          size="sm"
          onClick={() => window.open('/welcome', '_self')}
          className="voir-tout"
        >
          {t('homepage.widget.see.all', 'Voir tout')} →
        </Button>
      }
      backgroundColor="#f7f7f7"
    >
      <Flex gap="8" align="center">
        {CREATE_ACTIONS.map(({ labelKey, icon, color, href }) => (
          <a
            key={labelKey}
            href={href}
            title={labelKey}
            className="create-document-btn"
            style={{ background: color }}
          >
            {icon}
          </a>
        ))}
      </Flex>
    </WidgetCard>
  );
}
