import { Flex, IconButton } from '@edifice.io/react';
import { IconDelete } from '@edifice.io/react/icons';
import { IconSettings } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { WidgetCard } from '~/components/WidgetCard';

export interface PinnedResource {
  id: string;
  title: string;
  subtitle?: string;
  href: string;
  iconSrc?: string;
}

export interface PinnedResourcesWidgetProps {
  resources: PinnedResource[];
  onUnpin?: (id: string) => void;
}

export function PinnedResourcesWidget({
  resources,
  onUnpin,
}: PinnedResourcesWidgetProps) {
  const { t } = useTranslation();

  return (
    <WidgetCard
      title={t('homepage.widget.pinned.title', 'Ressources épinglées')}
      action={
        <IconButton
          aria-label={t('homepage.widget.pinned.settings', 'Paramètres')}
          color="tertiary"
          variant="ghost"
          size="sm"
          icon={<IconSettings />}
        />
      }
      backgroundColor="#F2F2F2"
    >
      <Flex direction="column" gap="8">
        {resources.map((resource) => (
          <WidgetCard key={resource.id} padding="8">
          <Flex key={resource.id} justify="between" align="center" gap="8">
            <a
              href={resource.href}
              className="pinned-resource-link"
              style={{ flex: 1, minWidth: 0 }}
            >
              <p className="mb-0 fw-semibold text-truncate">{resource.title}</p>
              {resource.subtitle && (
                <p className="mb-0 small text-muted text-truncate">
                  {resource.subtitle}
                </p>
              )}
            </a>
            {onUnpin && (
              <IconButton
                aria-label={t('homepage.widget.pinned.remove', 'Retirer')}
                color="tertiary"
                variant="ghost"
                size="sm"
                icon={<IconDelete />}
                onClick={() => onUnpin(resource.id)}
              />
            )}
          </Flex>
        </WidgetCard>
      ))}
    </Flex>
  </WidgetCard>
);
}
