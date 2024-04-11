import { Component, Injector } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { globalStore } from './core/store/global.store';
import { SessionModel } from './core/store/models/session.model';
import { StructureModel } from './core/store/models/structure.model';
import http from 'axios';
import {JsonObject} from '@angular/compiler-cli/ngcc/src/utils';
import { Session } from './core/store/mappings/session';


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
    private hasSubscribeChildRoute: boolean = false;

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

        this.subscriptions.add(this.route.data.subscribe((data: Data) => {
            if (data && data.config) {
                this.hideAdminV1Link = data.config['hide-adminv1-link'];
            }
        }));

        this.subscriptions.add(this.route.url.subscribe(url =>{
            if (!this.hasSubscribeChildRoute && this.route.children[0].routeConfig.path == ":structureId") {
                this.subscriptions.add(this.route.children[0].params.subscribe(params => {
                    if (params) {
                        const structureId = params.structureId;
                        if (structureId) {
                            this.currentStructure = globalStore.structures.data.find(s => s.id === structureId);
                            this.router.navigateByUrl(this.getNewPath(this.currentStructure.id));
                        }
                    }
                }));
                this.hasSubscribeChildRoute = true;
            }
        }));

        const session = await SessionModel.getSession();
        this.isAdmc = session.isADMC();

        if (this.isAdmc && this.router.url === '/admin') {
            this.router.navigateByUrl('/admin/admc/dashboard');
        }
        // Add Zendesk Guide Widget
        this.addZendeskGuideWedget(session);
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
    // Add Zendesk Guide Widget
    private addZendeskGuideWedget(session: Session) {
        // Get Zendesk Guide Widget configuration
        http.get<any>('/zendeskGuide/config?module=admin').then((response) => {
            const data = response.data as JsonObject;
            // Add Zendesk Guide Widget script if configuration is available and key is provided
            if (data && data.key) {
                const scriptZendesk = document.createElement('script');
                scriptZendesk.id = 'ze-snippet';
                scriptZendesk.src = `https://static.zdassets.com/ekr/snippet.js?key=${data.key}`;
                document.body.appendChild(scriptZendesk).onload = () => {
                    // Set Zendesk Guide Widget settings language
                    const currentLanguage = SessionModel.getCurrentLanguage();
                    currentLanguage.then((lang) => {
                        if ('fr' === lang) {
                            (window as any).zE(() => {
                                (window as any).zE.setLocale('fr');
                            });
                        } else {
                            (window as any).zE(() => {
                                (window as any).zE.setLocale('en-gb');
                            });
                        }
                    });
                    // Set Zendesk Guide Widget label from the pathname
                    let modulePathname = window.location.pathname;
                    this.setZendeskLabels(data.module as JsonObject, modulePathname);
                    // Set Zendesk Guide Widget settings color, launcher visibility, and support button visibility if user has the right
                    (window as any).zE('webWidget', 'updateSettings', {
                        webWidget: {
                            color: { theme: data.color || '#ffc400' },
                            launcher: {
                                mobile: {
                                    labelVisible: true
                                }
                            },
                            contactForm: {
                                suppress: !session.hasRight('net.atos.entng.support.controllers.DisplayController|view')
                            },
                            helpCenter: {
                                messageButton: {
                                    '*': 'Assistance ENT',
                                    'en-gb': 'ENT Support'
                                }
                            }
                        },
                    });
                    // Hide the launcher label when the user scrolls on mobile
                    window.addEventListener('scroll', () => {
                        (window as any).zE('webWidget', 'updateSettings', {
                            webWidget: {
                                launcher: {
                                    mobile: {
                                        labelVisible:  window.scrollY <= 5
                                    }
                                },
                            },
                        });
                    });

                    // Re-display the support button if user has the right and change label if the user has change his view
                    (window as any).zE('webWidget:on', 'open', () => {
                        if (window.location.pathname !==  modulePathname) {
                            this.setZendeskLabels(data.module as JsonObject, window.location.pathname);
                            modulePathname = window.location.pathname;
                        }
                        if (session.hasRight('net.atos.entng.support.controllers.DisplayController|view')) {
                            (window as any).zE('webWidget', 'updateSettings', {
                                webWidget: {
                                    contactForm: {
                                        suppress: false
                                    }
                                }});
                        }
                    });
                    // Redirect to support page if the contact form is shown and the user has the right
                    (window as any).zE('webWidget:on', 'userEvent', (ref: { category: any; action: any; properties: any; }) => {
                        const category = ref.category;
                        const action = ref.action;
                        const properties = ref.properties;
                        if (action === 'Contact Form Shown' && category === 'Zendesk Web Widget' && properties && properties.name === 'contact-form' && session.hasRight('net.atos.entng.support.controllers.DisplayController|view')) {
                            (window as any).zE('webWidget', 'updateSettings', {
                                webWidget: {
                                    contactForm: {
                                        suppress: true
                                    }
                                }});
                            (window as any).zE('webWidget', 'close');
                            window.location.href = '/support';
                        }
                    });
                };
            }
        });
    }
    // Set Zendesk Guide Widget label from the pathname
    private setZendeskLabels(dataModule, locationPathname) {
        const modulePathnameSplit = locationPathname.split('/');
        let moduleLabel = '';
        // Set Zendesk Guide Widget label from the pathname if the configuration is available else set the default label
        if (dataModule.labels && Object.keys(dataModule.labels).length > 0 && modulePathnameSplit.length > 1) {
            // Re-format the pathname with removing the id from the pathname
            for (let i = 1; i < modulePathnameSplit.length; i++) {
                if ( modulePathnameSplit[i].length > 0 && (modulePathnameSplit[i].match(/\d/) == null) ) {
                    if (moduleLabel.length  === 0) {
                        moduleLabel = modulePathnameSplit[i];
                    } else {
                        moduleLabel = moduleLabel + '/' + modulePathnameSplit[i];
                    }
                }
            }
            // Check if the label is available in the configuration if not set the default label
            if ( (dataModule.labels as JsonObject).hasOwnProperty(moduleLabel) ) {
                (window as any).zE('webWidget', 'helpCenter:setSuggestions', { labels: [(dataModule.labels as JsonObject)[moduleLabel]] });
            } else if (dataModule.default && String(dataModule.default).length > 0) {
                (window as any).zE('webWidget', 'helpCenter:setSuggestions', { labels: [dataModule.default] });
            }
        } else if (dataModule.default && String(dataModule.default).length > 0) {
            (window as any).zE('webWidget', 'helpCenter:setSuggestions', { labels: [dataModule.default] });
        }
    }
}
