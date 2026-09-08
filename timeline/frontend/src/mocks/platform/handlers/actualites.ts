import { HttpResponse, http } from 'msw';
import { mockLastInfos } from '../data/lastInfos';

export const handlers = [
  http.get('/actualites/api/v1/infos/preview/last/6', () =>
    HttpResponse.json(mockLastInfos),
  ),
];
