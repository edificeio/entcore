import { Component, Injector, OnInit } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from '../../core/services/routing.service';

/** "Contenus signalés" tab : embeds the moderation React app served by GET /admin/reports. */
@Component({
    selector: 'ode-reported-contents',
    templateUrl: './reported-contents.component.html',
    styleUrls: ['./reported-contents.component.scss']
})
export class ReportedContentsComponent extends OdeComponent implements OnInit {
    iframeUrl: SafeResourceUrl;
    private structureId: string;

    constructor(injector: Injector, private sanitizer: DomSanitizer) {
        super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        // The component may be reused when the selected structure changes : follow the route params.
        this.subscriptions.add(
            routing.observe(this.route, 'params').subscribe(() => {
                const structureId = routing.getParam(this.route.snapshot, 'structureId');
                if (structureId && structureId !== this.structureId) {
                    this.structureId = structureId;
                    this.iframeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
                        `/admin/reports/${encodeURIComponent(structureId)}`);
                }
            })
        );
    }
}
