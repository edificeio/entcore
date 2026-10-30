import { HttpResponse, http } from 'msw';
import { WIDGETS_ORDER_URL } from '~/services';

/** Project order of the homepage widgets (`assets/widgets` of the platform). */
export const handlers = [
  http.get(WIDGETS_ORDER_URL, () =>
    HttpResponse.json([
      { name: 'communities-widget', fullWidth: true },
      { name: 'agenda-widget' },
      { name: 'bookmark-widget' },
      { name: 'briefme-widget' },
      { name: 'qwant' },
      { name: 'universalis-widget' },
      { name: 'cursus-widget' },
      { name: 'cantine-widget' },
      { name: 'rss-widget' },
    ]),
  ),
];
