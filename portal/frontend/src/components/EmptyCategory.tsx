import { EmptyScreen } from "@edifice.io/react";
import { useTranslation } from "react-i18next";
import { Category } from "~/models/category";
import emptyFavoritesImage from '~/assets/illu-empty-apps-favorite.svg';
import emptyImage from '~/assets/illu-empty-apps.svg';

export function EmptyCategory({ category }: { category: Category }) {
  const { t } = useTranslation('common');

  if (category === 'favorites') {
    return (
      <EmptyScreen
        imageSrc={emptyFavoritesImage}
        text={t('my.apps.empty.favorite.text')}
        title={t('my.apps.empty.favorite.title')}
      />
    );
  }

  if (category === 'search') {
    return (
      <EmptyScreen
        imageSrc={emptyImage}
        text={t('my.apps.empty.search.text')}
        title={t('my.apps.empty.search.title')}
      />
    );
  }

  return (
    <EmptyScreen
      imageSrc={emptyImage}
      text={t('my.apps.empty.apps.text')}
      title={t('my.apps.empty.apps.title')}
    />
  );
}