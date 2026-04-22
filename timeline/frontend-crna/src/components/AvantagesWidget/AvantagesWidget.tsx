import { useTranslation } from 'react-i18next';
import imgTransport from './assets/avantage-transport.jpg';
import imgRegion from './assets/region-nouvelle-aquitaine.png';
import imgBooks from './assets/avantage-books.jpg';
import imgPermis from './assets/avantage-permis.jpg';
import imgTrain from './assets/avantage-train.jpg';
import { ListWidget, ListWidgetItem } from '../ui/ListWidget';

export function AvantagesWidget({
  onSeeMore = () =>
    window.open('https://jeunes.nouvelle-aquitaine.fr/', '_blank'),
}: {
  onSeeMore?: () => void;
}) {
  const { t } = useTranslation();
  const items: ListWidgetItem[] = [
    {
      id: '1',
      imageUrl: imgBooks,
      label: t('homepage.widget.avantages.manuels', 'Manuels scolaires gratuits'),
      sublabel: t('homepage.widget.avantages.manuels.desc', 'Fourniture gratuite pour tous les lycéens'),
      href: 'https://jeunes.nouvelle-aquitaine.fr/formation/accompagnement-scolaire/gratuite-des-manuels-scolaires-pour-les-lyceens',
    },
    {
      id: '2',
      imageUrl: imgTransport,
      label: t('homepage.widget.avantages.transport', 'Transport scolaire'),
      sublabel: t('homepage.widget.avantages.transport.desc', 'Transports gratuits ou réduits pour les élèves'),
      href: 'https://jeunes.nouvelle-aquitaine.fr/vie-quotidienne/se-deplacer/transport-et-abonnements-scolaires',
    },
    {
      id: '3',
      imageUrl: imgRegion,
      label: t('homepage.widget.avantages.soutien', 'Soutien scolaire gratuit'),
      sublabel: t('homepage.widget.avantages.soutien.desc', 'Aide aux devoirs et accompagnement scolaire'),
      href: 'https://jeunes.nouvelle-aquitaine.fr/formation/accompagnement-scolaire/aide-aux-devoirs-et-soutien-scolaire-gratuits',
    },
    {
      id: '4',
      imageUrl: imgPermis,
      label: t('homepage.widget.avantages.permis', 'Aide au permis B'),
      sublabel: t('homepage.widget.avantages.permis.desc', 'Subvention pour le passage du permis de conduire'),
      href: 'https://jeunes.nouvelle-aquitaine.fr/vie-quotidienne/se-deplacer/aide-au-financement-du-permis-b',
    },
    {
      id: '5',
      imageUrl: imgTrain,
      label: t('homepage.widget.avantages.ter', 'TER à prix réduits'),
      sublabel: t('homepage.widget.avantages.ter.desc', 'Se déplacer en train régional à tarif réduit'),
      href: 'https://jeunes.nouvelle-aquitaine.fr/vie-quotidienne/se-deplacer/ter-se-deplacer-prix-reduits',
    },
  ];
  return (
    <ListWidget
      title={t('homepage.widget.avantages.title', 'Mes avantages')}
      items={items}
      onSeeMore={onSeeMore}
      externalLink={true}
    />
  );
}
