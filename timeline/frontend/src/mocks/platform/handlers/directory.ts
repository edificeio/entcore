import { HttpResponse, http } from 'msw';
import { mockStructureChildren } from '../data/children';

export const handlers = [
  http.get('/directory/userbook/:userId', ({ params }) =>
    HttpResponse.json({
      mood: 'happy',
      health: 'good',
      alertSize: false,
      storage: 12345678,
      type: 'USERBOOK',
      userid: params.userId,
      picture: `/userbook/avatar/${params.userId}`,
      quota: 104857600,
      motto: 'Always Learning',
      theme: 'default',
      hobbies: ['reading', 'coding'],
    }),
  ),

  http.get('/directory/user/:userId/children', () =>
    HttpResponse.json(mockStructureChildren),
  ),
];
