import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, UrlTree } from '@angular/router';
import { routing } from '../../core/services/routing.service';
import { canAccessReportedContents } from './reported-contents.access';

const CONTENTS_TAB = 'contenus';
const NOTIFICATIONS_TAB = 'signalements';

function tabUrl(router: Router, route: ActivatedRouteSnapshot, tab: string): UrlTree {
    const structureId = routing.getParam(route, 'structureId');
    return router.createUrlTree(['/admin', structureId, 'alertes', tab]);
}

/** On /alertes : opens "Contenus signalés" for users having the right, "Notifications signalées" otherwise. */
@Injectable({
    providedIn: 'root'
})
export class AlertesDefaultTabGuardService implements CanActivate {

    constructor(private router: Router) {}

    public async canActivate(route: ActivatedRouteSnapshot): Promise<UrlTree> {
        const tab = await canAccessReportedContents() ? CONTENTS_TAB : NOTIFICATIONS_TAB;
        return tabUrl(this.router, route, tab);
    }
}

/** On /alertes/contenus : falls back to "Notifications signalées" for users without the right. */
@Injectable({
    providedIn: 'root'
})
export class ReportedContentsGuardService implements CanActivate {

    constructor(private router: Router) {}

    public async canActivate(route: ActivatedRouteSnapshot): Promise<boolean | UrlTree> {
        return await canAccessReportedContents() || tabUrl(this.router, route, NOTIFICATIONS_TAB);
    }
}
