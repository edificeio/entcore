import { Component, Injector } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { globalStore } from './core/store/global.store';
import { SessionModel } from './core/store/models/session.model';
import { StructureModel } from './core/store/models/structure.model';
import http from 'axios';
import {JsonObject} from '@angular/compiler-cli/ngcc/src/utils';


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

        http.get<any>('/zendeskGuide/config').then((response) => {
            const data = response.data as JsonObject;
            if (data && data.key) {
                const scriptZendesk = document.createElement('script');
                scriptZendesk.id = 'ze-snippet';
                scriptZendesk.src = `https://static.zdassets.com/ekr/snippet.js?key=${data.key}`;
                document.body.appendChild(scriptZendesk).onload = () => {
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
                    (window as any).zE('webWidget', 'helpCenter:setSuggestions', { search: 'Console administrateur' });
                    // (window as any).zE('webWidget', 'helpCenter:setSuggestions', { labels: ['connexion', 'authentification'] } );
                    (window as any).zE('webWidget', 'updateSettings', {
                        webWidget: {
                            color: { theme: data.color || '#ffc400' },
                            launcher: {
                                mobile: {
                                    labelVisible: true
                                }
                            },
                            helpCenter: {
                                messageButton: {
                                    '*': 'Assistance ENT'
                                }
                            }
                        },
                    });
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
                    (window as any).zE('webWidget:on', 'open', () => {
                        (window as any).zE('webWidget', 'updateSettings', {
                            webWidget: {
                                contactForm: {
                                    suppress: false
                                }
                            }});
                    });
                    (window as any).zE('webWidget:on', 'userEvent', (ref: { category: any; action: any; properties: any; }) => {
                        const category = ref.category;
                        const action = ref.action;
                        const properties = ref.properties;
                        if (action === 'Contact Form Shown' && category === 'Zendesk Web Widget' && properties && properties.name === 'contact-form') {
                            (window as any).zE('webWidget', 'updateSettings', {
                                webWidget: {
                                    contactForm: {
                                        suppress: true
                                    }
                                }});
                            window.location.href = '/support';
                        }
                    });
                };
            }
        });
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
