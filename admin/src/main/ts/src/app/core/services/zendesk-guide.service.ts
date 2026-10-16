import { JsonObject } from '@angular/compiler-cli/ngcc/src/utils';
import { Injectable, NgZone } from '@angular/core';
import { http } from 'entcore-toolkit';
import { BehaviorSubject } from 'rxjs';
import { Session } from '../store/mappings/session';
import { SessionModel } from '../store/models/session.model';
import { UiOverrideService } from './ui-override.service';

// Feature flag (uiOverrides in /assets/theme-conf.js) selecting the help zone launcher:
// `uiOverrides['layout.helpzone'] = 'edifice-in-product'` renders the Edifice-branded
// launcher (HelpZoneComponent), any other value keeps the native Zendesk launcher.
export const UI_OVERRIDE_HELPZONE_KEY = 'layout.helpzone';
export const HELPZONE_EDIFICE_IN_PRODUCT = 'edifice-in-product';

// Widget panel color forced by the Edifice help zone, regardless of the backend one
//TODO #IMPULS-6352 to delete with "edifice in product" generalization
const EDIFICE_HELPZONE_FORCED_COLOR = '#3030D1';

// Hides the native Zendesk launcher (see admin.scss)
const HELPZONE_ACTIVE_BODY_CLASS = 'help-zone-active';

/**
 * Loads and configures the Zendesk Guide Widget.
 * The widget is the same whatever the launcher: only the launcher button differs.
 */
@Injectable()
export class ZendeskGuideService {
    /** True when the Edifice-branded launcher must be rendered instead of the native one. */
    public readonly isEdificeHelpZone$ = new BehaviorSubject<boolean>(false);
    /** True while the widget panel is open. */
    public readonly isOpen$ = new BehaviorSubject<boolean>(false);

    constructor(private readonly uiOverrideService: UiOverrideService,
                private readonly zone: NgZone) {
    }

    public toggle() {
        (window as any).zE('webWidget', 'toggle');
    }

    // Add Zendesk Guide Widget
    public init(session: Session) {
        // Get Zendesk Guide Widget configuration
        http.get<any>('/zendeskGuide/config?module=admin').then(async (response) => {
            const data = response.data as JsonObject;
            // Add Zendesk Guide Widget script if configuration is available and key is provided
            if (data && data.key) {
                // TODO #IMPULS-6352 : inverser le feature flag pour que par defaut il ait ce look "edifice-in-product" mais que l'override { 'layout.helpzone': 'hide-edifice-logo' } puisse etre possible
                const isEdificeHelpZone = (await this.uiOverrideService.getUiOverride(UI_OVERRIDE_HELPZONE_KEY)) === HELPZONE_EDIFICE_IN_PRODUCT;
                if (isEdificeHelpZone) {
                    document.body.classList.add(HELPZONE_ACTIVE_BODY_CLASS);
                }

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
                            color: {
                                //TODO #IMPULS-6352 to update with "edifice in product" generalization
                                // theme: data.color || '#3030D1',
                                theme: (isEdificeHelpZone ? EDIFICE_HELPZONE_FORCED_COLOR : undefined) || data.color || '#3030D1',
                            },
                            ...(isEdificeHelpZone ? {} : {
                                launcher: {
                                    mobile: {
                                        labelVisible: true
                                    }
                                }
                            }),
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

                    if (isEdificeHelpZone) {
                        // Render the Edifice launcher (HelpZoneComponent)
                        this.zone.run(() => this.isEdificeHelpZone$.next(true));
                    } else {
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
                    }

                    // Re-display the support button if user has the right and change label if the user has change his view
                    (window as any).zE('webWidget:on', 'open', () => {
                        this.zone.run(() => this.isOpen$.next(true));
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
                    // Notify the help zone when the user closes the widget
                    (window as any).zE('webWidget:on', 'close', () => {
                        this.zone.run(() => this.isOpen$.next(false));
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
                            window.open('/support', '_blank');
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
