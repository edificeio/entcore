import { OdeComponent } from './../../../../core/ode/OdeComponent';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, AfterViewInit, OnDestroy, Injector } from '@angular/core';
import {DomSanitizer} from '@angular/platform-browser';
import {Subscription} from 'rxjs';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {routing} from '../../../../core/services/routing.service';
import {NotifyService} from '../../../../core/services/notify.service';
import {MessageFlashService} from '../../message-flash.service';
import {MessageFlashStore} from '../../message-flash.store';
import {BundlesService} from 'sijil';
import * as $ from 'jquery';


import 'trumbowyg/dist/trumbowyg.js';
import 'trumbowyg/dist/langs/fr.js';
import 'trumbowyg/dist/langs/es.js';
import 'trumbowyg/dist/langs/it.js';
import 'trumbowyg/dist/langs/de.js';
import 'trumbowyg/dist/langs/pt.js';
import 'trumbowyg/plugins/colors/trumbowyg.colors.js';
import 'trumbowyg/plugins/fontsize/trumbowyg.fontsize.js';
import 'trumbowyg/plugins/fontfamily/trumbowyg.fontfamily.js';
import 'trumbowyg/plugins/history/trumbowyg.history.js';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { FlashMessageModel } from 'src/app/core/store/models/flashmessage.model';


@Component({
    selector: 'ode-message-flash-form',
    templateUrl: './message-flash-form.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashFormComponent extends OdeComponent implements OnInit, OnDestroy, AfterViewInit {

    constructor(
        injector: Injector,
        public bundles: BundlesService,
        public ns: NotifyService,
        public messageStore: MessageFlashStore,
        public sanitized: DomSanitizer) { 
            super(injector);
        }

    structure: StructureModel;
    originalMessage: FlashMessageModel;
    message: FlashMessageModel = new FlashMessageModel();
    messages: FlashMessageModel[] = [];
    loadedLanguages: string[] = [];
    selectedLanguage: string = this.bundles.currentLanguage;
    dateFormat: Intl.DateTimeFormat;
    comboModel = ['Teacher', 'Student', 'Relative', 'Personnel', 'Guest', 'AdminLocal'];
    showLightbox = false;

    mailNotification = false;
    pushNotification = false;

    @Input() action: 'create' | 'edit' | 'duplicate';
    @Input() messageId = 'none';

    updateEditor: (lang: string) => void;

    // Lightbox methods

    private lightboxSubStructures: string[];

    ngOnInit(): void {
        super.ngOnInit();
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe(async (data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.message.structureId = this.structure.id;
            }
            if (this.action !== 'create' && data.messages) {
                this.messages = data.messages;
                this.originalMessage = this.messages.find(mess => mess.id === this.messageId);
                if (!this.originalMessage || this.originalMessage.structureId !== this.structure.id) {
                    this.router.navigate(['/admin', this.structure.id, 'management', 'message-flash', 'list']);
                    return;
                }
                this.message.id = this.originalMessage.id;
                this.message.title = this.originalMessage.title;
                const _startDate: Date = new Date(this.originalMessage.startDate);
                this.message.startDate = new Date(_startDate.getTime() - (_startDate.getTimezoneOffset() * 60000)).toISOString();
                const _endDate: Date = new Date(this.originalMessage.endDate);
                this.message.endDate = new Date(_endDate.getTime() - (_endDate.getTimezoneOffset() * 60000)).toISOString();
                if (!!this.originalMessage.color) {
                    this.message.color = this.originalMessage.color;
                }
                if (!!this.originalMessage.customColor) {
                    this.message.customColor = this.originalMessage.customColor;
                }
                this.message.profiles = Object.assign([], this.originalMessage.profiles);
                this.message.contents = JSON.parse(JSON.stringify(this.originalMessage.contents));
                MessageFlashService.getSubStructuresByMessageId(this.originalMessage.id)
                    .then(sdata => {
                        this.message.subStructures = sdata.map((item: any) => item.structure_id);
                        this.changeDetector.detectChanges();
                    });
            }
            if (this.action === 'create') {
                this.message.color = 'red';
            }
            this.changeDetector.detectChanges();
        }));

        this.subscriptions.add(this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.changeDetector.markForCheck();
            }
        }));

        MessageFlashService.getLanguages()
            .then(lang => {
                this.loadedLanguages = lang;
                this.selectedLanguage = this.getSelectedLanguage();
                this.changeDetector.detectChanges();
            })
            .catch(() => {
                this.loadedLanguages = [this.selectedLanguage];
                this.changeDetector.detectChanges();
            });
    }

    ngAfterViewInit() {
        super.ngAfterViewInit();
        const jq = $ as any;
        jq.trumbowyg.svgPath = '/admin/public/assets/icons.svg';
        const trumbowygEditor = jq('#trumbowyg-editor');
        trumbowygEditor.trumbowyg({
            lang: this.bundles.currentLanguage,
            removeformatPasted: true,
            semantic: false,
            btns: [['historyUndo', 'historyRedo'], ['strong', 'em', 'underline'],
            ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
            ['foreColor', 'fontfamily', 'fontsize'], ['link'], ['viewHTML']]
        });
        trumbowygEditor.on('tbwchange', () => {
            const transform = trumbowygEditor.trumbowyg('html')
                .replace(/<i>/g, '<em>').replace(/<\/i[^>]*>/g, '</em>');
            this.message.contents[this.selectedLanguage] = transform;
            this.changeDetector.detectChanges();
        });
        this.updateEditor = function(lang: string) {
            trumbowygEditor.trumbowyg('html', this.message.contents[lang] ? this.message.contents[lang] : '');
            this.cdRef.detectChanges();
        };
    }


    deselect(item) {
        this.message.profiles.splice(this.message.profiles.indexOf(item), 1);
    }

    languageOptions(): { value: string, label: string }[] {
        return this.loadedLanguages.map(lang => {
            return { value: lang, label: ('management.message.flash.language.' + lang) };
        });
    }

    private getSelectedLanguage(): string {
        const defaultLanguage = this.bundles.currentLanguage;
        if (this.message.contents[defaultLanguage]) {
            return defaultLanguage;
        }
        for (const loadedLanguage of this.loadedLanguages) {
            if (this.message.contents[loadedLanguage]) {
                return loadedLanguage;
            }
        }
        return defaultLanguage;
    }

    areSelectedChildren(): boolean {
        return this.message.subStructures.length > 0;
    }

    isToday(): boolean {
        const now: Date = new Date();
        const startDate: Date = new Date(this.message.startDate);
        const res = now.getDate() === startDate.getDate()
            && now.getMonth() === startDate.getMonth()
            && now.getFullYear() === startDate.getFullYear();
        if (!res) {
            this.mailNotification = false;
            this.pushNotification = false;
        }
        return res;
    }

    openLightbox(): void {
        this.lightboxSubStructures = Object.assign([], this.message.subStructures);
        this.showLightbox = true;
    }

    saveAndClose(): void {
        this.message.subStructures = this.lightboxSubStructures;
        if (this.message.subStructures.length > 0) {
            this.mailNotification = false;
            this.pushNotification = false;
        }
        this.closeLightbox();
    }

    closeLightbox(): void {
        this.showLightbox = false;
        this.changeDetector.detectChanges();
    }

    addOrRemoveChild(child: { name: string, id: string, children: any[], check: boolean }): void {
        const index = this.lightboxSubStructures.findIndex(subId => subId === child.id);
        if (index === -1) {
            this.lightboxSubStructures.push(child.id);
            this.checkAllChildren(child.children);
        } else {
            this.lightboxSubStructures = this.lightboxSubStructures.slice(0, index).concat(this.lightboxSubStructures.slice(index + 1, this.lightboxSubStructures.length));
            this.uncheckAllChildren(child.children);
        }
    }

    private checkAllChildren(children: { name: string, id: string, children: any[], check: boolean }[]) {
        children.forEach(child => {
            child.check = true;
            if (this.lightboxSubStructures.findIndex(subId => subId === child.id) === -1) {
                this.lightboxSubStructures.push(child.id);
            }
            this.checkAllChildren(child.children);
        });
    }

    private uncheckAllChildren(children: { name: string, id: string, children: any[], check: boolean }[]) {
        children.forEach(child => {
            child.check = false;
            const index = this.lightboxSubStructures.findIndex(subId => subId === child.id);
            if (index !== -1) {
                this.lightboxSubStructures = this.lightboxSubStructures.slice(0, index).concat(this.lightboxSubStructures.slice(index + 1, this.lightboxSubStructures.length));
            }
            this.uncheckAllChildren(child.children);
        });
    }

    getItems(): { name: string, id: string, children: any[], check: boolean }[] {
        const that = this;
        const myMap = (child: StructureModel) => {
            return {
                name: child.name,
                id: child.id,
                children: child.children && child.children.length > 0 ? child.children.map(myMap) : [],
                check: !!that.lightboxSubStructures && !!that.lightboxSubStructures.find(subId => subId === child.id)
            };
        };
        if (this.structure && this.structure.children) {
            return this.structure.children.map(myMap);
        } else {
            return [];
        }
    }

    //

    isUploadable(): boolean {
        return !!this.message && !!this.message.title && !!this.message.startDate && !!this.message.endDate
            && !!this.message.profiles && this.message.profiles.length > 0 && (!!this.message.color || !!this.message.customColor)
            && !!this.message.contents && Object.keys(this.message.contents).length > 0
            && Object.values(this.message.contents).findIndex(val => !!val) !== -1;
    }

    upload() {
        let promise;
        let key: string;
        if (this.action === 'edit') {
            promise = MessageFlashService.editMessage(this.message);
            key = 'edit';
        } else {
            promise = MessageFlashService.createMessage(this.message);
            key = 'create';
        }
        promise.then(() => {
            if (this.mailNotification || this.pushNotification) {
                MessageFlashService.sendNotifications(this.message, this.structure, this.selectedLanguage, this.mailNotification, this.pushNotification);
            }
            this.goBack(true);
            this.ns.success(
                { key: 'notify.management.' + key + '.success.content', parameters: {} },
                { key: 'notify.management.' + key + '.success.title', parameters: {} }
            );
        })
            .catch((error) => {
                this.ns.error(
                    { key: 'notify.management.' + key + '.error.content', parameters: {} },
                    { key: 'notify.management.' + key + '.error.title', parameters: {} }
                );
            });
    }

    goBack(forceReload: boolean): void {
        if (forceReload) {
            this.router.navigate(['/admin', this.structure.id, 'management', 'message-flash', 'list'], { queryParams: { forceReload: 'true' } });
        } else {
            this.router.navigate(['/admin', this.structure.id, 'management', 'message-flash', 'list']);
        }
        return;
    }

}
