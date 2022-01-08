import { Component, Injector } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { globalStore } from './core/store/global.store';
import { SessionModel } from './core/store/models/session.model';
import { StructureModel } from './core/store/models/structure.model';


@Component({
    selector: 'ode-admin-root',
    template: `
        <ode-navbar
            [structures]="structures"
            [currentStructure]="currentStructure"
            [hideAdminV1Link]="hideAdminV1Link"
            [isAdmc]="isAdmc"
            (selectStructure)="onSelectStructure($event)"
        >
        </ode-navbar>

        <ode-spinner-cube 
            waitingFor="portal-content" 
            class="portal-spinner"
        >
        </ode-spinner-cube>

        <section class="body">
            <router-outlet></router-outlet>
        </section>

        <section class="footer">
        </section>
    `,
    styles: [`
        .body {
            padding: 50px 5% 0 5%;
            max-width: 1600px;
            margin: 0 auto;
        }
    `],
})
export class AppRootComponent extends OdeComponent {
    public structures: Array<StructureModel>;
    public currentStructure: StructureModel;
    public hideAdminV1Link: boolean;
    public isAdmc: boolean;

    constructor(injector: Injector) {
        super(injector);
    }

    async ngOnInit() {
        super.ngOnInit();

        this.structures = globalStore.structures.asTree();

        if (this.structures.length === 1 && !this.structures[0].children) {
            this.currentStructure = this.structures[0];
            this.router.navigateByUrl(this.getNewPath(this.currentStructure.id));
        }

        this.subscriptions.add(this.route.children[0].params.subscribe(params => {
            if (params) {
                const structureId = params.structureId;
                if (structureId) {
                    this.currentStructure = globalStore.structures.data.find(s => s.id === structureId);
                    this.router.navigateByUrl(this.getNewPath(this.currentStructure.id));
                }
            }
        }));

        this.subscriptions.add(this.route.data.subscribe((data: Data) => {
            if (data && data.config) {
                this.hideAdminV1Link = data.config['hide-adminv1-link'];
            }
        }));

        const session = await SessionModel.getSession();
        this.isAdmc = session.isADMC();

        if (this.isAdmc && this.router.url === '/admin') {
            this.router.navigateByUrl('/admin/admc/dashboard');
        }
    }

    public onSelectStructure(structure: StructureModel) {
        this.router.navigateByUrl(this.getNewPath(structure.id));
    }

    private getNewPath(structureId): string {
        if (this.router.url.startsWith('/admin/admc')) {
            return `/admin/${structureId}`;
        }

        const replacerRegex = /^\/{0,1}admin(\/[^\/]+){0,1}/;
        return window.location.pathname.replace(replacerRegex, `/admin/${structureId}`);
    }
}
