import { Flex, LinkPill } from '@edifice.io/react';
import { HomeCard } from '@edifice.io/react/homepage';
import { IconExternalLink } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import type { LinkItem } from '~/models';
import imgTransport from './assets/avantage-transport.jpg';
import imgRegion from './assets/region-nouvelle-aquitaine.png';
import imgBooks from './assets/avantage-books.jpg';
import imgPermis from './assets/avantage-permis.jpg';
import imgTrain from './assets/avantage-train.jpg';

export function AvantagesWidget({
  onSeeMore = () =>
    window.open('https://jeunes.nouvelle-aquitaine.fr/', '_blank'),
}: {
  onSeeMore?: () => void;
}) {
  const { t } = useTranslation('timeline');
  const items: LinkItem[] = [
    {
      id: '1',
      imageUrl: imgBooks,
      label: t(
        'homepage.crna.widget.avantages.manuels',
        'Manuels scolaires gratuits',
      ),
      sublabel: t(
        'homepage.crna.widget.avantages.manuels.desc',
        'Fourniture gratuite pour tous les lycéens',
      ),
      href: 'https://jeunes.nouvelle-aquitaine.fr/formation/accompagnement-scolaire/gratuite-des-manuels-scolaires-pour-les-lyceens',
    },
    {
      id: '2',
      imageUrl: imgTransport,
      label: t(
        'homepage.crna.widget.avantages.transport',
        'Transport scolaire',
      ),
      sublabel: t(
        'homepage.crna.widget.avantages.transport.desc',
        'Transports gratuits ou réduits pour les élèves',
      ),
      href: 'https://jeunes.nouvelle-aquitaine.fr/vie-quotidienne/se-deplacer/transport-et-abonnements-scolaires',
    },
    {
      id: '3',
      imageUrl: imgRegion,
      label: t(
        'homepage.crna.widget.avantages.soutien',
        'Soutien scolaire gratuit',
      ),
      sublabel: t(
        'homepage.crna.widget.avantages.soutien.desc',
        'Aide aux devoirs et accompagnement scolaire',
      ),
      href: 'https://jeunes.nouvelle-aquitaine.fr/formation/accompagnement-scolaire/aide-aux-devoirs-et-soutien-scolaire-gratuits',
    },
    {
      id: '4',
      imageUrl: imgPermis,
      label: t('homepage.crna.widget.avantages.permis', 'Aide au permis B'),
      sublabel: t(
        'homepage.crna.widget.avantages.permis.desc',
        'Subvention pour le passage du permis de conduire',
      ),
      href: 'https://jeunes.nouvelle-aquitaine.fr/vie-quotidienne/se-deplacer/aide-au-financement-du-permis-b',
    },
    {
      id: '5',
      imageUrl: imgTrain,
      label: t('homepage.crna.widget.avantages.ter', 'TER à prix réduits'),
      sublabel: t(
        'homepage.crna.widget.avantages.ter.desc',
        'Se déplacer en train régional à tarif réduit',
      ),
      href: 'https://jeunes.nouvelle-aquitaine.fr/vie-quotidienne/se-deplacer/ter-se-deplacer-prix-reduits',
    },
  ];
  return (
    <HomeCard variant="user">
      <HomeCard.Header
        title={t('homepage.crna.widget.avantages.title', 'Mes avantages')}
        actionLabel={t('homepage.crna.widget.see.more', 'Voir plus')}
        actionRightIcon={<IconExternalLink />}
        onActionClick={onSeeMore}
      />
      <HomeCard.Content>
        <Flex direction="column" gap="8">
          {items.map((item) => (
            <LinkPill
              key={item.id}
              href={item.href}
              label={item.label}
              subtitle={item.sublabel}
              illustrationType="img"
              illustration={<img src={item.imageUrl} alt="" />}
            />
          ))}
        </Flex>
      </HomeCard.Content>
    </HomeCard>
  );
}
