import { Injectable } from '@angular/core';
import { Screeb } from '@screeb/sdk-angular';
import http from 'axios';
import { Session } from '../store/mappings/session';

type ScreebPublicConf = {
    'screeb-app-id'?: string;
    'screeb-allowed-profiles'?: string[];
};

/**
 * Bridge to the Screeb SDK (user feedback and surveys).
 * Never call the Screeb SDK directly from components: always go through this service.
 *
 * Screeb is enabled per platform via the `screeb-app-id` key of the admin
 * module `publicConf` (served by GET /admin/conf/public). When the key is
 * absent or empty, no Screeb script is loaded and no network call is made to Screeb.
 */
@Injectable()
export class ScreebService {

    constructor(private screeb: Screeb) {
    }

    /**
     * Reads the platform configuration and initializes Screeb when enabled
     * for this platform and allowed for the user's profile.
     */
    public async initFromPlatformConf(session: Session): Promise<void> {
        const conf = (await http.get<ScreebPublicConf>('/admin/conf/public')).data;
        const appId = conf?.['screeb-app-id'];
        if (!appId) {
            return;
        }
        const allowedProfiles = conf['screeb-allowed-profiles'];
        if (allowedProfiles && allowedProfiles.length > 0 && !allowedProfiles.includes(session.type)) {
            return;
        }
        await this.init(appId, session);
    }

    /**
     * Loads the Screeb tag and identifies the user in a single init call
     * (no anonymous entry created). The user identifier sent to Screeb is
     * hashed: the real userId is never transmitted.
     */
    public async init(appId: string, session: Session): Promise<void> {
        await this.screeb.load();
        const hashedId = await this.hashUserId(session.userId);
        await this.screeb.init(appId, hashedId, {profile: session.type});
    }

    /** SHA-256 hash truncated to 16 hexadecimal characters (privacy rule shared with the React apps). */
    private async hashUserId(userId: string): Promise<string> {
        const hashBuffer = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(userId));
        return Array.from(new Uint8Array(hashBuffer))
            .map(b => b.toString(16).padStart(2, '0'))
            .join('')
            .slice(0, 16);
    }
}
