import { ChangeDetectionStrategy, Component, Injector, Input, OnDestroy, OnInit } from '@angular/core';
import { Data, NavigationEnd } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { BundlesService } from 'ngx-ode-sijil';
import { NotifyService } from '../../../../core/services/notify.service';
import { routing } from '../../../../core/services/routing.service';
import { MessageFlashService } from '../../message-flash.service';
import { MessageFlashStore } from '../../message-flash.store';
import { StructureModel } from '../../../../core/store/models/structure.model';
import { FlashMessageModel } from '../../../../core/store/models/flashmessage.model';
import { TrumbowygOptions } from 'ngx-trumbowyg';

class StructureListItem {
    name: string;
    id: string;
    children: StructureListItem[];
    check: boolean;
}

@Component({
    selector: 'ode-message-flash-form',
    templateUrl: './message-flash-form.component.html',
    styleUrls: ['./message-flash-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashFormComponent extends OdeComponent implements OnInit, OnDestroy {
    @Input() action: 'create' | 'edit' | 'duplicate';
    @Input() messageId = 'none';

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
    private lightboxSubStructures: string[];
    trumbowygOptions: TrumbowygOptions = {lang: this.bundles.currentLanguage};
    itemList: StructureListItem[] = [];

    constructor(injector: Injector,
                public bundles: BundlesService,
                public ns: NotifyService,
                public messageStore: MessageFlashStore) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe(async (data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.message.structureId = this.structure.id;
            }
            if (this.action !== 'create' && data.messages) {
                this.messages = data.messages;
                this.originalMessage = this.messages.find(mess => mess.id == this.messageId);
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

    onSelectedLanguageChange(lang: string): void {
        this.selectedLanguage = lang;
        // prevents domSanitizer to display "undefined"
        if (!this.message.contents[this.selectedLanguage]) {
            this.message.contents[this.selectedLanguage] = '';
        }
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

    addOrRemoveChild(child: StructureListItem): void {
        const index = this.lightboxSubStructures.findIndex(subId => subId === child.id);
        if (index === -1) {
            this.lightboxSubStructures.push(child.id);
            this.checkAllChildren(child.children);
        } else {
            this.lightboxSubStructures = this.lightboxSubStructures.slice(0, index).concat(this.lightboxSubStructures.slice(index + 1, this.lightboxSubStructures.length));
            this.uncheckAllChildren(child.children);
        }
    }

    selectAll(): void {
        this.checkAllChildren(this.itemList);
    }

    unselectAll(): void {
        this.uncheckAllChildren(this.itemList);
    }

    private checkAllChildren(children: StructureListItem[]) {
        children.forEach(child => {
            child.check = true;
            if (this.lightboxSubStructures.findIndex(subId => subId === child.id) === -1) {
                this.lightboxSubStructures.push(child.id);
            }
            this.checkAllChildren(child.children);
        });
    }

    private uncheckAllChildren(children: StructureListItem[]) {
        children.forEach(child => {
            child.check = false;
            const index = this.lightboxSubStructures.findIndex(subId => subId === child.id);
            if (index !== -1) {
                this.lightboxSubStructures = this.lightboxSubStructures.slice(0, index).concat(this.lightboxSubStructures.slice(index + 1, this.lightboxSubStructures.length));
            }
            this.uncheckAllChildren(child.children);
        });
    }

    getItems(): StructureListItem[] {
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
            this.itemList = this.structure.children.map(myMap);
            return this.itemList;
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
        this.loadedLanguages.forEach(lang => this.message.contents[lang] = this.replaceItalicTags(this.message.contents[lang]));
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
            .catch(() => {
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

    private replaceItalicTags(html: string): string {
        if (html) {
            return html.replace(/<i[^>]*?>/g, '<em>').replace(/<\/i[^>]*?>/g, '</em>');
        }
        return;
    }
}
