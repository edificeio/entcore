import {Injectable} from '@angular/core';
import {NavigationStart, Resolve, Router, Event} from '@angular/router';


@Injectable({
    providedIn: 'root'
})
export class AppResolver implements Resolve<void> {
    constructor(private router: Router) {}

    resolve(): void {
        this.router.events.subscribe((event: Event) => {
            if (event instanceof NavigationStart) {
                // do something on start activity
                if ((window as any).zE) {
                    (window as any).zE('webWidget', 'close');
                }
            }
        });
    }
}
