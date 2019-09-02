import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef} from '@angular/core'
import { Subscription } from 'rxjs'
import { ActivatedRoute, Router, Data, NavigationEnd } from '@angular/router'
import { routing } from '../../core/services/routing.service'
import { StructureModel, FlashMessageModel } from '../../core/store'
import { NotifyService } from '../../core/services'
import { MessageFlashService } from './message-flash.service'
import { MessageFlashStore } from './message-flash.store'
import { BundlesService } from 'sijil'


@Component({
    selector: 'message-flash',
    template: `
        <div class="container has-shadow" *ngIf="router.isActive('/admin/' + structure?.id + '/management/message-flash', true)">
            <div class="has-vertical-padding">
                <div class="has-vertical-padding is-display-flex">
                    <div class="checkbox__item">
                        <input id="all" type="checkbox" [checked]="filter.current && filter.future && filter.obsolete" (change)="checkAllCategories()">
                        <label for="all"><s5l>management.message.flash.all</s5l></label>
                    </div>
                    <div class="checkbox__item">
                        <input id="in-progress" type="checkbox" name="filter" value="current" [(ngModel)]="filter.current" (change)="updateData()">
                        <label for="in-progress"><s5l>management.message.flash.current</s5l></label>
                    </div>
                    <div class="checkbox__item">
                        <input id="incoming" type="checkbox" name="filter" value="future" [(ngModel)]="filter.future" (change)="updateData()">
                        <label for="incoming"><s5l>management.message.flash.future</s5l></label>
                    </div>
                    <div class="checkbox__item">
                      <input id="obsolete" type="checkbox" name="filter" value="obsolete" [(ngModel)]="filter.obsolete" (change)="updateData()">
                      <label for="obsolete"><s5l>management.message.flash.obsolete</s5l></label>
                    </div>
                    <div class="has-left-margin-auto">
                      <button *ngIf="isSelectionCopyable()" (click)="duplicateMessage()"><s5l>management.message.flash.copy</s5l></button>
                      <button *ngIf="isSelectionRemovable()" (click)="showConfirmation = true"><s5l>management.message.flash.delete</s5l></button>
                      <lightbox-confirm
                          [show]="showConfirmation"
                          [lightboxTitle]="'warning'"
                          (onConfirm)="removeSelection()"
                          (onCancel)="showConfirmation = false">
                          <s5l>management.message.flash.confirm.delete</s5l>
                      </lightbox-confirm>
                      <button (click)="createMessage()"><s5l>management.message.flash.create.message</s5l></button>
                    </div>
                </div>
                <table class="table">
                    <thead>
                        <tr>
                            <th class="table__checkbox checkbox__item" (click)="checkAll()"><input type="checkbox" [checked]="areAllChecked()"  [disabled]="displayedMessages.length == 0"><label></label></th>
                            <th><s5l>management.message.flash.title</s5l></th>
                            <th><s5l>management.message.flash.message</s5l></th>
                            <th><s5l>management.message.flash.startDate</s5l></th>
                            <th><s5l>management.message.flash.endDate</s5l></th>
                            <th><s5l>management.message.flash.viewCount</s5l></th>
                            <th><s5l>management.message.flash.author</s5l></th>
                            <th><s5l>management.message.flash.lastModified</s5l></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngFor="let message of displayedMessages" [ngClass]="{'disabled': isMessageFromParentStructure(message)}" (click)="editMessage(message.id)">
                            <td class="checkbox__item" (click)="checkboxes[message.id] = !checkboxes[message.id]; $event.stopPropagation()"><input type="checkbox" [(ngModel)]="checkboxes[message.id]"><label></label></td>
                            <td>{{message.title}}</td>
                            <td [innerHTML]="getContent(message.contents)"></td>
                            <td>{{displayDate(message.startDate)}}</td>
                            <td>{{displayDate(message.endDate)}}</td>
                            <td>{{message.readCount}}</td>
                            <td>{{message.author}}</td>
                            <td>{{message.lastModifier}}</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <router-outlet></router-outlet>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashComponent implements OnInit{
    
    structure: StructureModel;
    dataSubscriber: Subscription;
    routerSubscriber: Subscription;
    messages: FlashMessageModel[];
    messagesFromParentStructure: FlashMessageModel[];
    displayedMessages: FlashMessageModel[] = [];
    checkboxes: boolean[] = [];
    dateFormat: Intl.DateTimeFormat;
    currentLanguage: string;

    showConfirmation: boolean = false;
    showEditFrom: boolean = false;

    public filter = {
        current: true,
        future: true,
        obsolete: true
    }
    
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
            }
            if (data['messages']) {
                this.messages = data['messages'].filter(mess => !this.isMessageFromParentStructure(mess));
                this.messages.forEach(mess => this.checkboxes[mess.id] = false);
                this.messagesFromParentStructure = data['messages'].filter(mess => this.isMessageFromParentStructure(mess));
                this.displayedMessages = this.messages.concat(this.messagesFromParentStructure); 
            }
            this.checkboxes = [];
            this.currentLanguage = this.bundles.currentLanguage;
            this.dateFormat = Intl.DateTimeFormat(this.currentLanguage);
            this.showConfirmation = false;
            this.filter.current = true;
            this.filter.future = true;
            this.filter.obsolete = true;
            this.cdRef.detectChanges();
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        })

    }

    ngOnDestroy() {
        if (!!this.dataSubscriber) {
            this.dataSubscriber.unsubscribe();
        }
        if (!!this.routerSubscriber) {
            this.routerSubscriber.unsubscribe();
        }
    }

    updateData(): void {
        var now = Date.now();
        var res = this.messages ? this.messages.concat(this.messagesFromParentStructure) : [];
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
        var allChecked = this.areAllChecked();
        this.displayedMessages.forEach(mess => this.checkboxes[mess.id] = !allChecked && !this.isMessageFromParentStructure(mess));
    }

    checkAllCategories(): void {
        var allChecked = this.filter.current && this.filter.future && this.filter.obsolete;
        this.filter.current = !allChecked;
        this.filter.future = !allChecked;
        this.filter.obsolete = !allChecked;
        this.updateData();
    }

    getContent(contents: Object): string {
        var res: string;
        if (contents[this.currentLanguage]) {
            res = contents[this.currentLanguage];
        } else {
            let keys: string[] = Object.keys(contents);
            for (var i = 0; i < keys.length; i++ ) {
                if (contents[keys[i]]) {
                    res = contents[keys[i]];
                    break;
                }
            }
        }
        return !!res && res.replace(/<\/p>/g,' ').replace(/<[^>]*?>/g,'');
    }

    displayDate(date: string): string {
        var _date: Date = new Date(date);
        return new Date(_date.getTime() - (_date.getTimezoneOffset() * 60000 )).toLocaleDateString(this.currentLanguage);
    }

    removeSelection(): void {
        let ids: string[] = this.displayedMessages.filter(mess => this.checkboxes[mess.id]).map(mess => mess.id);
        MessageFlashService.deleteMessages(ids)
        .then(() => {
            this.messages = this.messages.filter(mess => !ids.includes(mess.id));
            this.messageStore.messages = this.messageStore.messages.filter(mess => !ids.includes(mess.id));
            this.showConfirmation = false;
            this.updateData();
            this.cdRef.detectChanges();
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
        this.router.navigate(["/admin", this.structure.id, "management", "message-flash", "create"]);
    }

    editMessage(messageId: string): void {
        let message: FlashMessageModel = this.displayedMessages.find(mess => mess.id === messageId);
        if (!!message) {
            this.router.navigate(["/admin", this.structure.id, "management", "message-flash", "edit", message.id]);
        }
    }

    duplicateMessage(): void {
        let message: FlashMessageModel = this.displayedMessages.find(mess => this.checkboxes[mess.id]);
        if (!!message) {
            this.router.navigate(["/admin", this.structure.id, "management", "message-flash", "duplicate", message.id]);
        }
    }

}
