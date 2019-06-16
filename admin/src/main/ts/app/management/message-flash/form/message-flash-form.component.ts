import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, Input } from '@angular/core'
import { Subscription } from 'rxjs'
import { ActivatedRoute, Router, Data, NavigationEnd, Params } from '@angular/router'
import { routing } from '../../../core/services/routing.service'
import { StructureModel, FlashMessageModel } from '../../../core/store'
import { SpinnerService, NotifyService } from '../../../core/services'
import { MessageFlashService } from '../message-flash.service'
import { MessageFlashStore } from '../message-flash.store'
import { BundlesService } from 'sijil'


@Component({
    selector: 'message-flash-form',
    template: `
        <div class="container has-shadow">
            <h2><s5l>management.message.flash.{{action}}</s5l></h2>

            <div>
                <s5l>management.message.flash.title</s5l>*
                <input type="text" [(ngModel)]="message.title">
            </div>
            <div>
                <s5l>management.message.flash.startDate</s5l>*
                <date-picker [(ngModel)]="message.startDate"></date-picker>
            </div>
            <div>
                <s5l>management.message.flash.endDate</s5l>*
                <date-picker [(ngModel)]="message.endDate"></date-picker>
            </div>
            <div>
                <s5l>management.message.flash.profiles</s5l>*
                <multi-combo
                    [comboModel]="comboModel"
                    [(outputModel)]="message.profiles"
                    [title]="'management.message.flash.chose.profiles' | translate">
                </multi-combo>
            </div>

            <div>
                <s5l>management.message.flash.language</s5l>
                <mono-select [(ngModel)]="selectedLanguage" [options]="languageOptions()">
                </mono-select>
            </div>

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
    loadedLanguages: string[] = [];
    selectedLanguage: string;
    dateFormat: Intl.DateTimeFormat;
    comboModel = ['Teacher', 'Student', 'Relative', 'Personnel', 'Guest', 'AdminLocal'];
    showConfirmation: boolean = false;

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
            }
            if (this.action !== 'create' && data['messages']) {
                this.originalMessage = data['messages'].find(mess => mess.id == this.messageId);
                if (!!this.originalMessage) {
                    this.message.title = this.originalMessage.title;
                    this.message.startDate = this.originalMessage.startDate;
                    this.message.endDate = this.originalMessage.endDate;
                    this.message.color = this.originalMessage.color;
                    this.message.customColor = this.originalMessage.customColor;
                    this.message.profiles = Object.assign([], this.originalMessage.profiles);
                    this.message.contents = this.originalMessage.contents;
                } else {
                    this.router.navigate(["../.."], { relativeTo: this.route });
                }
            }
            this.selectedLanguage = this.bundles.currentLanguage;
            MessageFlashService.getLanguages()
            .then(lang => this.loadedLanguages = lang)
            .catch(() => this.loadedLanguages = [this.selectedLanguage]);
            this.cdRef.detectChanges();
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd) {
                this.cdRef.markForCheck();
            }
        })
    }

    languageOptions(): {value: string, label: string}[] {
        return this.loadedLanguages.map(lang => {
            return { value: lang, label: ('management.message.flash.language.'+lang) }
        });
    }

}
