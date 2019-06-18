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
        <div class="container has-shadow">
            <div class="has-vertical-padding">
                <div class="has-vertical-padding is-display-flex has-space-between">
                    <div class="checkbox__item">
                        <input id="all" type="checkbox" name="filter" value="all" [(ngModel)]="filter" (change)="updateData()">
                        <label for="all"><s5l>management.message.flash.all</s5l></label>
                    </div>
                    <div class="checkbox__item">
                        <input id="in-progress" type="checkbox" name="filter" value="current" [(ngModel)]="filter" (change)="updateData()">
                        <label for="in-progress"><s5l>management.message.flash.current</s5l></label>
                    </div>
                    <div class="checkbox__item">
                        <input id="incoming" type="checkbox" name="filter" value="future" [(ngModel)]="filter" (change)="updateData()">
                        <label for="incoming"><s5l>management.message.flash.future</s5l></label>
                    </div>
                    <div class="checkbox__item">
                      <input id="obsolete" type="checkbox" name="filter" value="obsolete" [(ngModel)]="filter" (change)="updateData()">
                      <label for="obsolete"><s5l>management.message.flash.obsolete</s5l></label>
                    </div>
                    <div>
                      <button *ngIf="isSelectionCopyable()" (click)="duplicateMessage()"><s5l>management.message.flash.copy</s5l></button>
                      <button *ngIf="isSelectionRemovable()" (click)="showConfirmation = true"><s5l>management.message.flash.delete</s5l></button>
                      <lightbox-confirm
                          [show]="showConfirmation"
                          [lightboxTitle]="'warning'"
                          (onConfirm)="removeSelection()"
                          (onCancel)="showConfirmation = false">
                          <s5l>management.message.flash.confirm.delete</s5l>
                      </lightbox-confirm>
                      <button class="is-pulled-right" (click)="createMessage()"><s5l>management.message.flash.create.message</s5l></button>
                    </div>
                </div>
                <table class="table">
                    <thead>
                        <tr>
                            <th class="table__checkbox checkbox__item"><input type="checkbox" [checked]="areAllChecked()" (click)="checkAll()" [disabled]="displayedMessages.length == 0"><label></label></th>
                            <th><s5l>management.message.flash.title</s5l></th>
                            <th><s5l>management.message.flash.message</s5l></th>
                            <th><s5l>management.message.flash.startDate</s5l></th>
                            <th><s5l>management.message.flash.title</s5l></th>
                            <th><s5l>management.message.flash.viewCount</s5l></th>
                            <th><s5l>management.message.flash.author</s5l></th>
                            <th><s5l>management.message.flash.lastModified</s5l></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngFor="let message of displayedMessages" [ngClass]="{'disabled': isMessageFromParentStructure(message)}" (click)="editMessage(message.id)">
                            <td class="checkbox__item"><input type="checkbox" [(ngModel)]="checkboxes[message.id]" (click)="$event.stopPropagation()"></td>
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

    public filter: "all" | "current" | "future" | "obsolete" = 'all';
    
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
            this.filter = 'all';
            this.cdRef.detectChanges();
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        })
    }

    updateData(): void {
        var now = Date.now();
        var res = this.messages ? this.messages.concat(this.messagesFromParentStructure) : [];
        switch(this.filter) {
            case "all":
                break;
            case "current":
                res = res.filter(mess => new Date(mess.startDate).getTime() < now && new Date(mess.endDate).getTime() > now);
                break;
            case "future":
                res = res.filter(mess => new Date(mess.startDate).getTime() > now);
                break;
            case "obsolete":
                res = res.filter(mess => new Date(mess.endDate).getTime() < now);
                break;
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

    getContent(contents: string): string {
        return contents[this.currentLanguage];
    }

    displayDate(date: string): string {
        return this.dateFormat.format(new Date(date));
    }

    removeSelection(): void {
        let ids: string[] = this.displayedMessages.filter(mess => this.checkboxes[mess.id]).map(mess => mess.id);
        MessageFlashService.deleteMessages(ids)
        .then(() => {
            this.messages = this.messages.filter(mess => !ids.includes(mess.id));
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
        this.router.navigate(["/admin", this.structure.id, "management", "message-flash-create"]);
    }

    editMessage(messageId: string): void {
        let message: FlashMessageModel = this.displayedMessages.find(mess => mess.id === messageId);
        if (!!message) {
            this.router.navigate(["/admin", this.structure.id, "management", "message-flash-edit", message.id]);
        }
    }

    duplicateMessage(): void {
        let message: FlashMessageModel = this.displayedMessages.find(mess => this.checkboxes[mess.id]);
        if (!!message) {
            this.router.navigate(["/admin", this.structure.id, "management", "message-flash-duplicate", message.id]);
        }
    }

}