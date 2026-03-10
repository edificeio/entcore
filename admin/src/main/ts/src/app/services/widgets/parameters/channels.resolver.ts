import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { SpinnerService } from 'ngx-ode-ui';
import { routing } from 'src/app/core/services/routing.service';
import http from 'axios';
import { Channel } from './bibliocollege-channels.types';

@Injectable()
export class ChannelsResolver implements Resolve<Channel[]> {

    constructor(private spinner: SpinnerService) {}

    resolve(route: ActivatedRouteSnapshot, _state: RouterStateSnapshot): Promise<Channel[]> {
        const structureId = routing.getParam(route, 'structureId');
        if (!structureId) {
            return Promise.resolve([]);
        }
        const type = (route.queryParams && route.queryParams['type']) || 'bibliocollege';
        const typeQ = encodeURIComponent(type);
        return this.spinner.perform(
            'portal-content',
            http.get<Channel[]>(`/rss/channels/structure/${structureId}?type=${typeQ}`)
                .then(res => {
                    const list = (res.data && Array.isArray(res.data)) ? res.data : [];
                    if (list.length > 0) return list;
                    return http.post<Channel>('/rss/channel', { feeds: [], structureID: structureId, type })
                        .then(createRes => (createRes?.data ? [createRes.data] : []))
                        .catch(() => []);
                })
                .catch(() =>
                    http.post<Channel>('/rss/channel', { feeds: [], structureID: structureId, type })
                        .then(createRes => (createRes?.data ? [createRes.data] : []))
                        .catch(() => [])
                )
        );
    }
}
