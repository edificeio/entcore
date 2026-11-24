import { useDate } from '@edifice.io/react';
import { BriefMe, BriefMeProps } from '@edifice.io/react/homepage';
import { useMemo, useState } from 'react';

type BriefMeArticle = BriefMeProps['articles'][number];

/** Option values of the `BriefMe` category selector. */
type BriefMeCategory = 'briefme' | 'brief-eco' | 'brief-science';

const BRIEFME_URL = 'https://brief.me';

/** Article titles per category, from the most recent edition to the oldest. */
const TITLES: Record<BriefMeCategory, string[]> = {
  'briefme': [
    'Comment fonctionne le Parlement européen ? | Les cantines misent sur les produits locaux | Quiz : les grandes dates du Moyen Âge',
    'Les records de chaleur de l’année expliqués | Le retour du loup dans les Vosges | Pourquoi le français emprunte autant à l’anglais',
    'Les élections municipales en questions | Le vélo progresse dans les grandes villes | Portrait : une navigatrice en solitaire autour du monde',
  ],
  'brief-eco': [
    'Pourquoi les prix de l’énergie varient-ils autant ? | Le marché du jeu vidéo en France | Qu’est-ce qu’une start-up ?',
    'L’argent de poche des collégiens en chiffres | Les métiers qui recrutent le plus | Comment se fixe le prix d’une baguette',
    'Le commerce en ligne continue sa progression | Les ports de la mer du Nord | À quoi sert une banque centrale ?',
  ],
  'brief-science': [
    'Un télescope spatial observe une planète semblable à la Terre | Les abeilles sauvages en déclin | Comment naissent les aurores boréales',
    'Une nouvelle espèce de dinosaure découverte en France | Le cerveau des adolescents en pleine évolution | Quiz : le système solaire',
    'Les océans absorbent toujours plus de chaleur | Comment fonctionne un vaccin ? | Le robot qui explore les fonds marins',
  ],
};

/** Demo "Brief.me": the last three editions of each category, dated from today. */
export function BriefMeDemo() {
  const { formatDate } = useDate();
  const [category, setCategory] = useState<BriefMeCategory>('briefme');

  const articles = useMemo<BriefMeArticle[]>(
    () =>
      TITLES[category].map((title, index) => {
        const date = new Date();
        date.setDate(date.getDate() - index);
        return {
          id: `${category}-${index}`,
          date: formatDate(date, 'long'),
          title,
          url: BRIEFME_URL,
        };
      }),
    [category, formatDate],
  );

  return (
    <BriefMe
      handleActionClick={() => {}}
      status="default"
      category={category}
      onCategoryChange={(value) => setCategory(value as BriefMeCategory)}
      articles={articles}
    />
  );
}
