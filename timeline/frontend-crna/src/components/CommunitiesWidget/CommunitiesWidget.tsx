import { Button, Flex } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';

export interface Community {
  id: string;
  name: string;
  coverUrl?: string;
  href: string;
}

export interface CommunitiesWidgetProps {
  communities: Community[];
}

export function CommunitiesWidget({ communities }: CommunitiesWidgetProps) {
  const { t } = useTranslation();

  return (
    <div className="communities-widget">
      <Flex justify="between" align="center" className="mb-12">
        <span className="fw-bold">
          {t('homepage.widget.communities.title', 'Mes communautés')}
        </span>
        <Button
          color="tertiary"
          variant="ghost"
          size="sm"
          onClick={() => window.open('/community', '_self')}
        >
          {t('homepage.widget.see.all', 'Voir tout')} →
        </Button>
      </Flex>
      <Flex gap="12" className="communities-list">
        {communities.map((community) => (
          <a
            key={community.id}
            href={community.href}
            className="community-card"
            title={community.name}
          >
            <div
              className="community-card-cover"
              style={{
                backgroundImage: community.coverUrl
                  ? `url(${community.coverUrl})`
                  : undefined,
              }}
            />
            <span className="community-card-name fw-semibold small">
              {community.name}
            </span>
          </a>
        ))}
      </Flex>
    </div>
  );
}
