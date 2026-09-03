import { HttpResponse, http } from 'msw';
import { mockUserInfo } from '../data/userinfo';

export const handlers = [
  http.get('/auth/oauth2/userinfo', () => HttpResponse.json(mockUserInfo)),
];
