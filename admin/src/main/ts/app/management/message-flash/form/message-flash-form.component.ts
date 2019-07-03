import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, Input } from '@angular/core'
import { Subscription } from 'rxjs'
import { ActivatedRoute, Router, Data, NavigationEnd, Params } from '@angular/router'
import { routing } from '../../../core/services/routing.service'
import { StructureModel, FlashMessageModel } from '../../../core/store'
import { SpinnerService, NotifyService } from '../../../core/services'
import { MessageFlashService } from '../message-flash.service'
import { MessageFlashStore } from '../message-flash.store'
import { BundlesService } from 'sijil'


import 'trumbowyg/dist/trumbowyg.js'
import 'trumbowyg/dist/langs/fr.js'
import 'trumbowyg/dist/langs/es.js'
import 'trumbowyg/dist/langs/it.js'
import 'trumbowyg/dist/langs/de.js'
import 'trumbowyg/dist/langs/pt.js'
import 'trumbowyg/plugins/colors/trumbowyg.colors.js'
import 'trumbowyg/plugins/fontsize/trumbowyg.fontsize.js'
import 'trumbowyg/plugins/fontfamily/trumbowyg.fontfamily.js'
import 'trumbowyg/plugins/history/trumbowyg.history.js'


@Component({
    selector: 'message-flash-form',
    template: `
        <div class="container has-shadow">
            <h2><s5l>management.message.flash.{{action}}</s5l></h2>

            <fieldset>
                <form-field label="management.message.flash.title">
                    <input type="text" [(ngModel)]="message.title" class="is-flex-none">
                </form-field>
                <form-field label="management.message.flash.startDate">
                    <date-picker [(ngModel)]="message.startDate"></date-picker>
                </form-field>
                <form-field label="management.message.flash.endDate">
                    <date-picker [(ngModel)]="message.endDate"></date-picker>
                </form-field>
                <form-field label="management.message.flash.profiles">
                    <multi-combo style="z-index: 20;"
                        [comboModel]="comboModel"
                        [(outputModel)]="message.profiles"
                        [title]="'management.message.flash.chose.profiles' | translate">
                    </multi-combo>
                </form-field>
                <div class="multi-combo-companion">
                    <div *ngFor="let item of message.profiles"
                        (click)="deselect(item)">
                        <s5l>{{item}}</s5l>
                        <i class="fa fa-trash is-size-5"></i>
                    </div>
                </div>
                <form-field *ngIf="!!structure && !!structure.children && structure.children.length > 0"
                label="management.message.flash.selected.etab">
                    <span class="is-flex-none has-right-margin-40">{{message.subStructures.length}}</span>
                    <button class="is-flex-none" (click)="openLightbox()"><s5l>management.message.flash.manage</s5l></button>
                </form-field>
                <form-field label="management.message.flash.language">
                    <mono-select [(ngModel)]="selectedLanguage" (ngModelChange)="updateEditor($event)" [options]="languageOptions()">
                    </mono-select>
                </form-field>
                <form-field label="management.message.flash.color">
                    <span class="is-flex-none">
                        <div class="legend-square red" [ngClass]="{ outlined: message.color == 'red' }" (click)="message.color = 'red'"></div>
                        <div class="legend-square green" [ngClass]="{ outlined: message.color == 'green'}" (click)="message.color = 'green'"></div>
                        <div class="legend-square blue" [ngClass]="{ outlined: message.color == 'blue' }" (click)="message.color = 'blue'"></div>
                        <div class="legend-square orange" [ngClass]="{ outlined: message.color == 'orange' }" (click)="message.color = 'orange'"></div>
                        <input type="color" ng-model="message.customColor" [ngClass]="{ outlined: !!message.customColor }" [(ngModel)]="message.customColor">
                    </span>
                </form-field>
                <form-field label="management.message.flash.notification">
                    <span class="is-flex-none">
                        <input type="checkbox" [(ngModel)]="mailNotification" [disabled]="areSelectedChildren() || !isToday()">
                        <s5l>management.message.flash.notification.email</s5l>
                        <input class="has-left-margin-40 is-hidden" type="checkbox" [(ngModel)]="pushNotification" [disabled]="areSelectedChildren() || !isToday()">
                        <s5l class="is-hidden">management.message.flash.notification.mobile</s5l>
                    </span>
                </form-field>
                <div *ngIf="areSelectedChildren() || !isToday()">
                    <i class="fa fa-exclamation-circle"></i>
                    <s5l>management.message.flash.lightbox.warning.notification</s5l>
                </div>
            </fieldset>

            <div class="has-top-margin-40" style="width: 50%;">
                <textarea id="trumbowyg-editor">{{ message.contents[selectedLanguage] }}</textarea>
            </div>

            <div class="has-top-margin-40">
                <button (click)="goBack(false)"><s5l>management.message.flash.cancel</s5l></button>
                <button [disabled]="!isUploadable()" (click)="upload()"><s5l>management.message.flash.upload</s5l></button>
            </div>

            <lightbox *ngIf="structure"
                [show]="showLightbox" (onClose)="closeLightbox()">
                <h2><s5l>management.message.flash.lightbox.title</s5l></h2>
                <p><s5l>management.message.flash.lightbox.explanation</s5l></p>
                <div style="overflow: auto; max-height: 50%" class="has-bottom-margin-40">
                    <item-tree
                        [items]="getItems()"
                        order="name"
                        display="name"
                        [checkboxMode]="true"
                        (onCheck)="addOrRemoveChild($event)">
                    </item-tree>
                </div>
                <span><i class="fa fa-exclamation-circle"></i></span>
                <s5l>management.message.flash.lightbox.warning</s5l>
                <div><button class="is-pulled-right" (click)="saveAndClose()"><s5l>management.message.flash.lightbox.save</s5l></button></div>
            </lightbox>

        </div>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashFormComponent implements OnInit{
    
    structure: StructureModel;
    dataSubscriber: Subscription;
    routeSubscriber: Subscription;
    routerSubscriber: Subscription;
    originalMessage: FlashMessageModel;
    message: FlashMessageModel = new FlashMessageModel();
    messages: FlashMessageModel[] = [];
    loadedLanguages: string[] = [];
    selectedLanguage: string = this.bundles.currentLanguage;
    dateFormat: Intl.DateTimeFormat;
    comboModel = ['Teacher', 'Student', 'Relative', 'Personnel', 'Guest', 'AdminLocal'];
    showLightbox: boolean = false;

    mailNotification: boolean = false;
    pushNotification: boolean = false;

    @Input() action: 'create' | 'edit' | 'duplicate';
    @Input() messageId: string = 'none';
    
    constructor(
        public route: ActivatedRoute,
        public router: Router,
        public cdRef: ChangeDetectorRef,
        public bundles: BundlesService,
        private ns: NotifyService,
        public messageStore: MessageFlashStore) {}

    ngOnInit(): void {

        this.dataSubscriber = routing.observe(this.route, "data").subscribe(async (data: Data) => {
            if (data['structure']) {
                this.structure = data['structure'];
                this.message.structureId = this.structure.id;
            }
            if (this.action !== 'create' && data['messages']) {
                this.messages = data['messages'];
                this.originalMessage = this.messages.find(mess => mess.id == this.messageId);
                if (!this.originalMessage || this.originalMessage.structureId !== this.structure.id) {
                    this.router.navigate(["/admin", this.structure.id, "management", "message-flash"]);
                    return;
                }
                this.message.id = this.originalMessage.id;
                this.message.title = this.originalMessage.title;
                this.message.startDate = this.originalMessage.startDate;
                this.message.endDate = this.originalMessage.endDate;
                if (!!this.originalMessage.color) {
                    this.message.color = this.originalMessage.color;
                }
                if (!! this.originalMessage.customColor) {
                    this.message.customColor = this.originalMessage.customColor;
                }
                this.message.profiles = Object.assign([], this.originalMessage.profiles);
                this.message.contents = JSON.parse(JSON.stringify(this.originalMessage.contents));
                MessageFlashService.getSubStructuresByMessageId(this.originalMessage.id)
                .then(data => {
                    this.message.subStructures = data.map(item => item['structure_id']);
                    this.cdRef.detectChanges();
                });
            }
            this.cdRef.detectChanges();
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        })

        MessageFlashService.getLanguages()
            .then(lang => {
                this.loadedLanguages = lang;
                this.selectedLanguage = this.getSelectedLanguage();
                this.cdRef.detectChanges();
            })
            .catch(() => {
                this.loadedLanguages = [this.selectedLanguage];
                this.cdRef.detectChanges();
            });
    }

    ngAfterViewInit() {
        let jq = <any>jQuery;
        jq.trumbowyg.svgPath = '/admin/public/styles/icons.svg';
        let trumbowygEditor = jq("#trumbowyg-editor");
        trumbowygEditor.trumbowyg({
            lang: this.bundles.currentLanguage,
            removeformatPasted: true,
            semantic: false,
            btns: [['historyUndo', 'historyRedo'], ['strong', 'em', 'underline'],
                    ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
                    ['foreColor', 'fontfamily', 'fontsize'], ['link'], ['viewHTML']]
        });
        trumbowygEditor.on('tbwchange', () => {
            var transform = trumbowygEditor.trumbowyg('html')
            .replace(/<i>/g,'<em>').replace(/<\/i[^>]*>/g,'</em>');
            this.message.contents[this.selectedLanguage] = transform;
            this.cdRef.detectChanges();
        });
        this.updateEditor = function (lang: string) {
            trumbowygEditor.trumbowyg('html', this.message.contents[lang] ? this.message.contents[lang] : "");
            this.cdRef.detectChanges();
        }
    }

    updateEditor: (lang: string) => void;

    ngOnDestroy() {
        if (!!this.dataSubscriber) {
            this.dataSubscriber.unsubscribe();
        }
        if (!!this.routeSubscriber) {
            this.routeSubscriber.unsubscribe();
        }
        if (!!this.routerSubscriber) {
            this.routerSubscriber.unsubscribe();
        }
    }

    private deselect(item) {
        this.message.profiles.splice(this.message.profiles.indexOf(item), 1);
    }

    languageOptions(): {value: string, label: string}[] {
        return this.loadedLanguages.map(lang => {
            return { value: lang, label: ('management.message.flash.language.'+lang) }
        });
    }

    private getSelectedLanguage(): string {
        let defaultLanguage = this.bundles.currentLanguage;
        if (this.message.contents[defaultLanguage]) {
            return defaultLanguage;
        }
        for (var i = 0; i < this.loadedLanguages.length; i++) {
            if (this.message.contents[this.loadedLanguages[i]]) {
                return this.loadedLanguages[i];
            }
        }
        return defaultLanguage;
    }

    areSelectedChildren(): boolean {
        return this.message.subStructures.length > 0;
    }

    isToday(): boolean {
        var now: Date = new Date();
        var startDate: Date = new Date(this.message.startDate);
        var res = now.getDate() ==  startDate.getDate()
        && now.getMonth() == startDate.getMonth()
        && now.getFullYear() == startDate.getFullYear();
        if (!res) {
            this.mailNotification = false;
            this.pushNotification = false;
        }
        return res;
    }

    // Lightbox methods

    private lightboxSubStructures: string[];

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
        this.cdRef.detectChanges();
    }

    addOrRemoveChild(child: { name: string, id: string, children: any[], check: boolean}): void {
        let index = this.lightboxSubStructures.findIndex(subId => subId === child.id);
        if (index == -1) {
            this.lightboxSubStructures.push(child.id);
            this.checkAllChildren(child.children);
        } else {
            this.lightboxSubStructures = this.lightboxSubStructures.slice(0,index).concat(this.lightboxSubStructures.slice(index+1,this.lightboxSubStructures.length));
            this.uncheckAllChildren(child.children);
        }
    }

    private checkAllChildren(children: { name: string, id: string, children: any[], check: boolean}[]) {
        children.forEach(child => {
            child.check = true;
            if (this.lightboxSubStructures.findIndex(subId => subId === child.id) == -1) {
                this.lightboxSubStructures.push(child.id);
            }
            this.checkAllChildren(child.children);
        })
    }

    private uncheckAllChildren(children: { name: string, id: string, children: any[], check: boolean}[]) {
        children.forEach(child => {
            child.check = false;
            let index = this.lightboxSubStructures.findIndex(subId => subId === child.id);
            if (index != -1) {
                this.lightboxSubStructures = this.lightboxSubStructures.slice(0,index).concat(this.lightboxSubStructures.slice(index+1,this.lightboxSubStructures.length));
            }
            this.uncheckAllChildren(child.children);
        })
    }

    private getItems(): { name: string, id: string, children: any[], check: boolean}[] {
        var that = this;
        let myMap = function (child: StructureModel) {
            return {
                name: child.name,
                id: child.id,
                children: child.children && child.children.length > 0 ? child.children.map(myMap) : [],
                check: !!that.lightboxSubStructures && !!that.lightboxSubStructures.find(subId => subId === child.id)
            };
        }
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
        && Object.values(this.message.contents).findIndex(val => !!val) != -1;
    }

    upload() {
        let promise;
        let key: string;
        if (this.action === 'edit') {
            promise = MessageFlashService.editMessage(this.message);
            key = 'edit'
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
                { key: 'notify.management.'+key+'.success.content', parameters: {} },
                { key: 'notify.management.'+key+'.success.title', parameters: {} }
            );
        })
        .catch((error) => {
            this.ns.error(
                { key: 'notify.management.'+key+'.error.content', parameters: {} },
                { key: 'notify.management.'+key+'.error.title', parameters: {} }
            );
        });
    }

    goBack(forceReload: boolean): void {
        if (forceReload) {
            this.router.navigate(["/admin", this.structure.id, "management", "message-flash"], { queryParams: { forceReload: 'true' } });
        } else {
            this.router.navigate(["/admin", this.structure.id, "management", "message-flash"]);
        }
        return;
    }

}
