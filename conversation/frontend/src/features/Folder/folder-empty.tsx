import { Button, EmptyScreen, useEdificeClient } from '@edifice.io/react';
import { IconEdit } from '@edifice.io/react/icons';
import { useTranslation } from 'react-i18next';
import { useParams, useSearchParams } from 'react-router-dom';

import illuMessagerie from '@images/emptyscreen/illu-messagerie.svg';
import illuNoContent from '@images/emptyscreen/illu-no-content-in-folder.svg';
import illuSearch from '@images/emptyscreen/illu-search.svg';
import illuTrash from '@images/emptyscreen/illu-trash.svg';
import { useMemo } from 'react';

export function FolderEmpty() {
  const { folderId } = useParams();
  const [searchParams] = useSearchParams();
  const { appCode } = useEdificeClient();
  const { t } = useTranslation(appCode);

  const emptyScreenData: {
    illu: string;
    title: string;
    text: string;
    withNewMessage: boolean;
  } = useMemo(() => {
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
  }, [folderId, searchParams]);

  return (
    <div className="d-flex flex-column gap-24 align-items-center justify-content-center">
      <EmptyScreen
        imageSrc={emptyScreenData.illu}
        imageAlt={t(emptyScreenData.title)}
        title={t(emptyScreenData.title)}
        text={t(emptyScreenData.text)}
      />
      {emptyScreenData.withNewMessage && (
        <div>
          <Button>
            <IconEdit />
            {t('new.message')}
          </Button>
        </div>
      )}
    </div>
  );
}
