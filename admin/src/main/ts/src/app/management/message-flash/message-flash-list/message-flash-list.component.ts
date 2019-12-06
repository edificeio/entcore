import { ChangeDetectionStrategy, Component, Injector, OnInit } from '@angular/core';
import { Data, NavigationEnd } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { BundlesService } from 'ngx-ode-sijil';
import { FlashMessageModel } from 'src/app/core/store/models/flashmessage.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { NotifyService } from '../../../core/services/notify.service';
import { routing } from '../../../core/services/routing.service';
import { MessageFlashService } from '../message-flash.service';
import { MessageFlashStore } from '../message-flash.store';


@Component({
    selector: 'ode-message-flash-list',
    templateUrl: './message-flash-list.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashListComponent extends OdeComponent implements OnInit {

    structure: StructureModel;
    messages: FlashMessageModel[];
    messagesFromParentStructure: FlashMessageModel[];
    displayedMessages: FlashMessageModel[] = [];
    checkboxes: boolean[] = [];
    dateFormat: Intl.DateTimeFormat;
    currentLanguage: string;

    showConfirmation = false;
    showEditFrom = false;

    public filter = {
        current: true,
        future: true,
        obsolete: true
    };

    constructor(
        injector: Injector,
        public bundles: BundlesService,
        private ns: NotifyService,
        public messageStore: MessageFlashStore) {
            super(injector);
        }

    ngOnInit(): void {
        super.ngOnInit();
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe(async (data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
            }
            if (data.messages) {
                this.messages = data.messages.filter(mess => !this.isMessageFromParentStructure(mess));
                this.messages.forEach(mess => this.checkboxes[mess.id] = false);
                this.messagesFromParentStructure = data.messages.filter(mess => this.isMessageFromParentStructure(mess));
                this.displayedMessages = this.messages.concat(this.messagesFromParentStructure);
            }
            this.checkboxes = [];
            this.currentLanguage = this.bundles.currentLanguage;
            this.dateFormat = Intl.DateTimeFormat(this.currentLanguage);
            this.showConfirmation = false;
            this.filter.current = true;
            this.filter.future = true;
            this.filter.obsolete = true;
            this.changeDetector.detectChanges();
        }));

        this.subscriptions.add(this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.changeDetector.markForCheck();
            }
        }));

    }

    updateData(): void {
        const now = Date.now();
        let res = this.messages ? this.messages.concat(this.messagesFromParentStructure) : [];
        if (!this.filter.current) {
            res = res.filter(mess => new Date(mess.startDate.split(' ')[0]).getTime() > now || new Date(mess.endDate.split(' ')[0]).getTime() < now);
        }
        if (!this.filter.future) {
            res = res.filter(mess => new Date(mess.startDate.split(' ')[0]).getTime() < now);
        }
        if (!this.filter.obsolete) {
            res = res.filter(mess => new Date(mess.endDate.split(' ')[0]).getTime() > now);
        }
        this.displayedMessages = res;
    }

    isMessageFromParentStructure(message: FlashMessageModel): boolean {
        return !!message && !!this.structure && message.structureId !== this.structure._id;
    }

    private numberOfCheckedMessages(): number {
        return this.displayedMessages.filter(mess => this.checkboxes[mess.id]).length;
    }

    isSelectionRemovable(): boolean {
        return this.numberOfCheckedMessages() > 0;
    }

    isSelectionCopyable(): boolean {
        return this.numberOfCheckedMessages() === 1;
    }

    areAllChecked(): boolean {
        return this.displayedMessages.length > 0 && this.displayedMessages.every(mess => this.isMessageFromParentStructure(mess) || this.checkboxes[mess.id]);
    }

    checkAll(): void {
        const allChecked = this.areAllChecked();
        this.displayedMessages.forEach(mess => this.checkboxes[mess.id] = !allChecked && !this.isMessageFromParentStructure(mess));
    }

    checkAllCategories(): void {
        const allChecked = this.filter.current && this.filter.future && this.filter.obsolete;
        this.filter.current = !allChecked;
        this.filter.future = !allChecked;
        this.filter.obsolete = !allChecked;
        this.updateData();
    }

    getContent(contents: Object): string {
        let res: string;
        if (contents[this.currentLanguage]) {
            res = contents[this.currentLanguage];
        } else {
            const keys: string[] = Object.keys(contents);
            for (let i = 0; i < keys.length; i++ ) {
                if (contents[keys[i]]) {
                    res = contents[keys[i]];
                    break;
                }
            }
        }
        return !!res && res.replace(/<\/p>/g, ' ').replace(/<[^>]*?>/g, '');
    }

    displayDate(date: string): string {
        const _date: Date = new Date(date);
        return new Date(_date.getTime() - (_date.getTimezoneOffset() * 60000 )).toLocaleDateString(this.currentLanguage);
    }

    removeSelection(): void {
        const ids: string[] = this.displayedMessages.filter(mess => this.checkboxes[mess.id]).map(mess => mess.id);
        MessageFlashService.deleteMessages(ids)
        .then(() => {
            this.messages = this.messages.filter(mess => !ids.includes(mess.id));
            this.messageStore.messages = this.messageStore.messages.filter(mess => !ids.includes(mess.id));
            this.showConfirmation = false;
            this.updateData();
            this.changeDetector.detectChanges();
            this.ns.success(
                { key: 'notify.management.remove.success.content', parameters: {} },
                { key: 'notify.management.remove.success.title', parameters: {} }
            );
        })
        .catch(() => {
            this.showConfirmation = false;
            this.ns.error(
                { key: 'notify.management.remove.error.content', parameters: {} },
                { key: 'notify.management.remove.error.title', parameters: {} }
            );
        });
    }

    createMessage(): void {
        this.router.navigate(['/admin', this.structure.id, 'management', 'message-flash', 'create']);
    }

    editMessage(messageId?: string): void {
        let message: FlashMessageModel;
        if (!!messageId) {
            message = this.displayedMessages.find(mess => mess.id === messageId);
        } else {
            message = this.displayedMessages.find(mess => this.checkboxes[mess.id]);
        }
        if (!!message) {
            this.router.navigate(['/admin', this.structure.id, 'management', 'message-flash', 'edit', message.id]);
        }
    }

    duplicateMessage(): void {
        const message: FlashMessageModel = this.displayedMessages.find(mess => this.checkboxes[mess.id]);
        if (!!message) {
            this.router.navigate(['/admin', this.structure.id, 'management', 'message-flash', 'duplicate', message.id]);
        }
    }

}
