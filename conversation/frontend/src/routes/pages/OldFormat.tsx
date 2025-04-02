import { useEdificeTheme } from '@edifice.io/react';
import { QueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { LoaderFunctionArgs, useLoaderData } from 'react-router-dom';
import { useI18n } from '~/hooks';
import { Message as MessageData } from '~/models';

import { messageQueryOptions } from '~/services';

/** Load a message in OLD-FORMAT content */
export const loader =
  (queryClient: QueryClient) =>
  async ({ params /*, request*/ }: LoaderFunctionArgs) => {
    const { messageId } = params;
    const message = await queryClient.ensureQueryData(
      messageQueryOptions.getOriginalFormat(messageId as string),
    );
    return message;
  };

export function Component() {
  const data = useLoaderData() as MessageData;
  const { theme } = useEdificeTheme();
  const { t } = useI18n();

  useEffect(() => {
    const link = document.getElementById('theme') as HTMLAnchorElement;
    if (link) link.href = `${theme?.themeUrl}theme.css`;
  }, [theme?.themeUrl]);

  const style = {
    margin: 'auto',
    padding: '16px',
    minHeight: '100vh',
    backgroundColor: '#fff',
  };

  return data ? (
    <div className="d-flex flex-column mt-24 ms-md-24 me-md-16">
      <div
        style={style}
        contentEditable={false}
        dangerouslySetInnerHTML={{
          __html: data?.body ?? t('message.modal.notfound.or.unauthorized'),
        }}
      />
    </div>
  ) : null;
}
