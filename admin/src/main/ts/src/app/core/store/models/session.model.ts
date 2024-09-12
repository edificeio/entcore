import http from 'axios';
import { Context } from '../mappings/context';
import { Session } from '../mappings/session';
import { Theme } from '../mappings/theme';

export class SessionModel {

    private static session: Session;
    private static context: Context;
    private static currentLanguage: string;
    private static theme: Promise<Theme>;

    public static getSession(): Promise<Session> {
        if (!SessionModel.session) {

            return new Promise((resolve, reject) => {
                http.get('/auth/oauth2/userinfo')
                .then(result => {
                    SessionModel.session = result.data as Session;
                    Object.setPrototypeOf(SessionModel.session, new Session());
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

    public static getContext(): Promise<Context> {
        if (!SessionModel.context) {
            return new Promise((resolve, reject) => {
                http.get('/auth/context')
                .then(result => {
                    SessionModel.context = result.data as Context;
                    Object.setPrototypeOf(SessionModel.context, new Context());
                    resolve(SessionModel.context);
                }, e => {
                    console.error(e);
                    resolve(new Context());
                });
            });
        } else {
            return Promise.resolve( SessionModel.context );
        }
    }

    public static getCurrentLanguage(): Promise<string> {
        if (!SessionModel.currentLanguage) {
            return new Promise((resolve, reject) => {
                http.get('/userbook/preference/language')
                .then(result => {
                    SessionModel.currentLanguage = JSON.parse(result.data.preference)['default-domain'] as string;
                    resolve(SessionModel.currentLanguage);
                }, e => {
                    console.error(e);
                    resolve('fr');
                });
            });
        } else {
            return Promise.resolve( SessionModel.currentLanguage );
        }
    }

    public static getTheme(): Promise<Theme> {
        if(!SessionModel.theme) {
            SessionModel.theme = http.get('/theme')
            .then(result => {
                let theme = result.data as Theme;
                if(typeof theme.skin === "string" && 
                        theme.skin.length>0 &&
                        !theme.skin.endsWith('/')) {
                    theme.skin += '/';
                }
                Object.setPrototypeOf(theme, new Theme());
                return theme;
            })
            .catch(e => {
                console.error(e);
                return new Theme();
            });
        }
        return SessionModel.theme
    }
}
