import {Injectable} from '@angular/core';
import {Resolve} from '@angular/router';
import { SessionModel } from '../store/models/session.model';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class FavIconResolver implements Resolve<void> {

    constructor(private http: HttpClient) {}

    async resolve(): Promise<void> {
        const theme = await SessionModel.getTheme();
        const iconUrl = `${theme.skin}../../img/illustrations/favicon.ico`;
        const request = new Request(iconUrl, {method: "HEAD"});
        try {
            const response = await fetch(request);
            if(response.ok) {
                const link = document.createElement("link");
                link.rel = "icon";
                link.href = iconUrl;
                document.head.appendChild(link);
                console.log("favicon OK");
            } else {
                console.log("favicon unreachable");
            }
        } catch(e) {
            console.log("Cannot fetch favicon :" + e);
        }
    }
}
