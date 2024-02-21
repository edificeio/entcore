export const SessionMode = {
    COOKIE: 0,
    OAUTH2: 1
}
export class Session {
    constructor(token, mode, expiresIn, cookies) {
        this.token = token;
        this.mode = mode;
        this.cookies = cookies;
        this.expiresAt = Date.now() + (expiresIn * 1000) - 3000;
    }
    isExpired() {
        return this.expiresAt <= Date.now();
    }
    getCookie(cookieName) {
        return this.cookies ? this.cookies.filter(cookie => cookie.name === cookieName).map(cookie => cookie.value)[0] : null;
    }
}