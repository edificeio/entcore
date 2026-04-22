import { Button, Flex } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { WidgetCard } from '../WidgetCard';

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
          style={{ color: '#3030D1' }}
        >
          {t('homepage.widget.see.all', 'Voir tout')} →
        </Button>
      }
      backgroundColor="#F2F2F2"
    >
      <Flex gap="8" align="center">
        {CREATE_ACTIONS.map(({ labelKey, icon, color, href }) => (
          <a
            key={labelKey}
            href={href}
            title={labelKey}
            className="create-document-btn"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 36,
              height: 36,
              borderRadius: 8,
              background: color,
              color: '#fff',
              fontWeight: 700,
              fontSize: 16,
              textDecoration: 'none',
            }}
          >
            {icon}
          </a>
        ))}
      </Flex>
    </WidgetCard>
  );
}
