import { EmptyScreen } from "@edifice.io/react";
import { useTranslation } from "react-i18next";
import { Category } from "~/models/category";
import emptyFavoritesImage from '~/assets/illu-empty-apps-favorite.svg';

export function EmptyCategory({ category }: { category: Category}) {
	const { t } = useTranslation('common');

	if(category === 'favorites') {
		return <EmptyScreen
			imageSrc={emptyFavoritesImage}
			text={t('my.apps.empty.favorite.text')}
			title={t('my.apps.empty.favorite.title')}
		/>
	}

	return <EmptyScreen
		imageSrc="https://edificeio.github.io/edifice-frontend-framework/assets/illu-search-DLlTIc41.svg"
		text={t('my.apps.empty.apps.text')}
		title={t('my.apps.empty.apps.title')}
	/>
}