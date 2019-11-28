import http from 'axios';
import { Session } from '../mappings/session';

export class SessionModel {

    private static session: Session;

    public static getSession(): Promise<Session> {
        if (!SessionModel.session) {

            return new Promise((resolve, reject) => {
                http.get('/auth/oauth2/userinfo')
                .then(result => {
                    SessionModel.session = result.data as Session;
                    resolve(SessionModel.session);
                }, e => {
                    console.error(e);
                    resolve(new Session());
                });
            });
        } else {
            return new Promise(res => res(SessionModel.session));
        }
    }
}
