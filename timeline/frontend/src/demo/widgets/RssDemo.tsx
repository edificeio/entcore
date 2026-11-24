import {
  Rss,
  RssArticle,
  RssFeed,
  RssFeedsModal,
} from '@edifice.io/react/homepage';
import { useMemo, useState } from 'react';

const FEEDS: RssFeed[] = [
  { title: 'Lumni', link: 'https://www.lumni.fr/rss' },
  { title: '1jour1actu', link: 'https://www.1jour1actu.com/feed' },
  { title: 'Onisep', link: 'https://www.onisep.fr/rss' },
];

/** [title, description, hours elapsed since publication] */
type ArticleSeed = [string, string, number];

/** Latest articles of each feed, in the order of `FEEDS`. */
const ARTICLES: ArticleSeed[][] = [
  [
    [
      'Les fractions expliquées en 3 minutes',
      'Une vidéo pour comprendre enfin comment additionner et comparer des fractions, avec des exemples tirés de la vie quotidienne.',
      3,
    ],
    [
      'Charlemagne, l’empereur à la barbe fleurie ?',
      'Qui était vraiment Charlemagne et pourquoi son couronnement en l’an 800 a-t-il marqué l’histoire de l’Europe ?',
      27,
    ],
    [
      'Réviser l’anglais en chansons',
      'Une sélection de titres pour travailler le prétérit et enrichir son vocabulaire en s’amusant.',
      52,
    ],
  ],
  [
    [
      'Pourquoi les feuilles changent-elles de couleur ?',
      'Chlorophylle, lumière et températures : on t’explique ce qui se passe dans les arbres au fil des saisons.',
      5,
    ],
    [
      'C’est quoi, le harcèlement en ligne ?',
      'Comment le reconnaître, à qui en parler et pourquoi il ne faut jamais rester seul face à lui.',
      30,
    ],
    [
      'Le sport au collège, ça sert à quoi ?',
      'Bouger, coopérer, se dépasser : des élèves et un professeur d’EPS racontent.',
      76,
    ],
  ],
  [
    [
      'Découvrir les métiers de l’environnement',
      'Agent de l’ONF, technicien de l’eau, chargé de mission biodiversité : des métiers qui recrutent et qui ont du sens.',
      8,
    ],
    [
      'Mon stage de 3e : comment m’y préparer dès la 5e ?',
      'Repérer ses centres d’intérêt et explorer les secteurs d’activité pour bien choisir son stage d’observation.',
      49,
    ],
    [
      'Les métiers du numérique en vidéo',
      'Développeuse, game designer, technicien réseau : ils présentent leur quotidien en quelques minutes.',
      98,
    ],
  ],
];

const toArticles = (seeds: ArticleSeed[], feed: RssFeed): RssArticle[] =>
  seeds.map(([title, description, hoursAgo]) => ({
    title,
    description,
    link: new URL(feed.link).origin,
    pubDate: new Date(Date.now() - hoursAgo * 60 * 60 * 1000).toUTCString(),
  }));

/**
 * Demo "Flux RSS": three feeds suited to a collège student, whose latest
 * articles are dated from now. The management modal opens, its actions do
 * nothing.
 */
export function RssDemo() {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const articles = useMemo(
    () => toArticles(ARTICLES[selectedIndex], FEEDS[selectedIndex]),
    [selectedIndex],
  );

  return (
    <>
      <Rss
        feeds={FEEDS}
        selectedIndex={selectedIndex}
        onSelectFeed={setSelectedIndex}
        articles={articles}
        articlesStatus="success"
        onEditClick={() => setIsModalOpen(true)}
      />
      <RssFeedsModal
        isOpen={isModalOpen}
        feeds={FEEDS}
        canAddFeed
        onClose={() => setIsModalOpen(false)}
        onAddClick={() => {}}
        onEditFeed={() => {}}
        onDeleteFeed={() => {}}
      />
    </>
  );
}
