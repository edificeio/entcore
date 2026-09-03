import { HttpResponse, http } from 'msw';

export const handlers = [
  http.get('/workspace/quota/user/:userId', () =>
    HttpResponse.json({ quota: 104857600, storage: 12345678 }),
  ),
];
