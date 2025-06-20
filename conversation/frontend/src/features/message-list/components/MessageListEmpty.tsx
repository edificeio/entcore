import { EmptyScreen, useEdificeClient } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { useParams, useSearchParams } from 'react-router-dom';

import illuMessagerie from '@images/emptyscreen/illu-messagerie.svg';
import illuNoContent from '@images/emptyscreen/illu-no-content-in-folder.svg';
import illuSearch from '@images/emptyscreen/illu-search.svg';
import illuTrash from '@images/emptyscreen/illu-trash.svg';
import { NewMessageButton } from '~/components/NewMessageButton';

export function MessageListEmpty() {
  const { folderId } = useParams();
  const [searchParams] = useSearchParams();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);

  const getEmptyScreenData = () => {
    const search = searchParams.get('search');
    const unread = searchParams.get('unread');
    if ((search && search !== '') || !!unread) {
      return {
        illu: illuSearch,
        title: 'search.empty.title',
        text: 'search.empty.text',
        withNewMessage: false,
      };
    } else {
      if (['draft', 'inbox', 'outbox'].includes(folderId!)) {
        return {
          illu: illuMessagerie,
          title: 'messagerie.empty.title',
          text: 'messagerie.empty.text',
          withNewMessage: true,
        };
      } else if (folderId === 'trash') {
        return {
          illu: illuTrash,
          title: 'trash.empty.title',
          text: 'trash.empty.text',
          withNewMessage: false,
        };
      } else {
        return {
          illu: illuNoContent,
          title: 'noContent.empty.title',
          text: 'noContent.empty.text',
          withNewMessage: false,
        };
      }
    }
  };

  return (
    <div className="d-flex flex-column gap-24 align-items-center justify-content-center pt-24">
      <EmptyScreen
        imageSrc={getEmptyScreenData().illu}
        imageAlt={t(getEmptyScreenData().title)}
        title={t(getEmptyScreenData().title)}
        text={t(getEmptyScreenData().text)}
      />
      {getEmptyScreenData().withNewMessage && (
        <div>
          <NewMessageButton />
        </div>
      )}
    </div>
  );
}
