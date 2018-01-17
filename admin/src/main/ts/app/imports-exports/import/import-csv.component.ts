import { Component, 
    OnDestroy, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, OnChanges,
    Input, Output, ViewChild } from '@angular/core'
import { Mix } from 'entcore-toolkit'
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { SpinnerService,routing } from '../../core/services'
import { BundlesService } from 'sijil'
import { ImportCSVService } from './import-csv.service'
import { User, Error, Profile } from './user.model'
import { WizardComponent } from '../../shared/ux/components'

import { Messages } from './messages.model'


@Component({
    selector: 'import-csv',
    template : `
        <lightbox [show]="confirmCancel" (onClose)="confirmCancel = false">
            <div class="container">
                <h2>{{'import.cancel.header' | translate}}</h2>
                <article class="message is-warning">
                    {{'import.cancel.message' | translate}}
                </article>
                    <button 
                        (click)="confirmCancel = false"
                        [title]="'import.cancel.backToImport'  | translate">
                        {{'import.cancel.backToImport' | translate}}
                    </button>
                    <button 
                        (click)="cancel();confirmCancel = false;"
                        [title]="'import.cancel.confirm' | translate">
                        {{'import.cancel.confirm'  | translate}}
                    </button>
            </div>
        </lightbox>
        <wizard
            (cancel)="confirmCancel=true"
            (finish)="finish()"
            (nextStep)="nextStep($event)"
            (previousStep)="previousStep($event)"
        >
        <step #step1 name="{{ 'import.filesDeposit' | translate }}" [isActived]="true" [class.active]="step1.isActived">
            <h2>{{ 'import.filesDeposit' | translate }}
            <message-sticker [type]="'info'" [header]="'import.info.file.0'" 
                [messages]="messages.get('import.info.file')">
            </message-sticker>
            </h2>
            <message-box *ngIf="globalError.message" [type]="'danger'" [messages]="[globalError.message]"></message-box>
            <h3>{{ 'import.header.selectFile' | translate }}</h3>
            <p *ngFor="let p of profiles.asArray(true)">
                <input type="checkbox" name="{{p + 'CB'}}" [(ngModel)]="profiles[p]" />
                <label>{{'import.file.'+p | translate}}</label>
                <input type="file" name="{{p}}" (change)="loadFile($event)" [hidden]="!isLoaded(p)" />
                <message-box *ngIf="globalError.profile['error.'+p]" [type]="'danger'" 
                    [messages]="globalError.profile['error.'+p]"></message-box>
            </p>
            <h3>{{ 'import.header.parameters' | translate }}</h3>
            <p>
                <input type="checkbox"  name="predeleteOption"  [(ngModel)]="importInfos.predelete" />
                <label>{{'import.deleteAccountOption' | translate}}</label>
                <message-sticker [type]="'info'" [messages]="messages.get('import.info.deleteAccount')"></message-sticker>
            </p>
            <p>
                <input type="checkbox" name="transitionOption" [(ngModel)]="importInfos.transition" />
                <label>{{'import.transitionOption' | translate}}</label>
                <message-sticker [type]="'info'" [messages]="messages.get('import.info.transition')"></message-sticker>
            </p>
        </step>
        <step #step2 name="{{ 'import.fieldsChecking' | translate }}" [class.active]="step2.isActived">
            <h2>{{ 'import.fieldsChecking' | translate }}</h2>
            <message-box *ngIf="!globalError.message && !columns.hasWarning()" [type]="'success'" 
                [messages]="['import.fieldsChecking.success']"></message-box>
            <message-box *ngIf="globalError.message" [type]="'danger'" [messages]="[globalError.message]"></message-box>
            <message-box *ngIf="columns.hasWarning()" [type]="'warning'" [messages]="['import.global.warning']"></message-box>
            <panel-section *ngFor="let p of columns.profiles" section-title="{{'import.file.'+ p}}" [folded]="true">
                <span other-actions>
                    <message-sticker [type]="'info'" [messages]="['import.info.columns.'+p]"></message-sticker>            
                    <message-sticker *ngIf="globalError.profile[p]" [type]="'danger'" 
                        [messages]="[['import.error.requieredFieldNotFound',{fields : globalError.profile[p]}]]"></message-sticker>
                    <message-sticker *ngIf="columns.hasWarning(p)" [type]="'warning'" 
                        [messages]="['import.file.warning']"></message-sticker>
                </span>
                <message-box *ngIf="globalError.profile[p]" [type]="'danger'" 
                    [messages]="[['import.error.requieredFieldNotFound',{fields : globalError.profile[p]}]]"></message-box>
                <mappings-table 
                    [headers]="['import.fieldFromFile','import.fieldToMap']"
                    [mappings]="columns.mappings[p]"
                    [availables]="columns.availableFields[p]"
                    [emptyLabel]="'import.fieldsChecking.warning.ignore'"
                    [emptyWarning]="'import.fieldsChecking.warning.ignore.1'"
                >
                </mappings-table>
            </panel-section>
        </step>
        <step #step3 name="{{'import.classesChecking' | translate }}" [class.active]="step3.isActived">
            <h2>{{ 'import.classesChecking' | translate }}
                <message-sticker [type]="'info'" [header]="'import.classesChecking.info.0'" 
                    [messages]="messages.get('import.classesChecking.info')">
                </message-sticker>
            </h2>
            <message-box *ngIf="!classes.hasWarning()" [type]="'success'" [messages]="['import.classesChecking.success']"></message-box>
            <message-box *ngIf="globalError.message" [type]="'danger'" [messages]="[globalError.message]"></message-box>
            <message-box *ngIf="classes.hasWarning()" [type]="'warning'" [messages]="['import.file.warning']"></message-box>
            <panel-section *ngFor="let profile of classes.profiles" section-title="{{'import.file.'+ profile}}" [folded]="true"> 
                <span other-actions>
                    <message-sticker *ngIf="classes.hasWarning(p)" [type]="'warning'" 
                        [messages]="['importClassesChecking.'+ (profile == 'Student()' ? 'student' : 'generic') + '.warning']"></message-sticker>
                </span>

                <mappings-table 
                    [headers]="['import.classFromFile','import.classToMap']"
                    [mappings]="classes.mappings[profile]"
                    [availables]="classes.availableClasses[profile]"
                    [emptyLabel]="'import.classesChecking.warning.create'"
                    [emptyWarning]="'import.classesChecking.warning.create.1'"
                >
                </mappings-table>
            </panel-section>
        </step>
        <step #step4 name="{{ 'import.report' | translate }}" [class.active]="step4.isActived">
            <h2>{{ 'import.report' | translate }}</h2>
            <div *ngIf="report.hasErrors()" class="container report-filter">
                <a *ngFor="let r of report.softErrors.reasons" 
                    class="button"
                    [ngClass]="{'is-danger':report.countByReason(r) > 0, 
                                'is-success':report.countByReason(r) == 0,
                                'is-outlined':!report.hasFilter('reasons',r)}"
                    (click)="report.setFilter('reasons',r); report.page.offset=0"
                >
                {{ [r, report.countByReason(r)] | translate }}
                    &nbsp;
                </a>
            </div>
            <div class="container has-text-right">
                <a (click)="report.setFilter('none')">{{'view.all' | translate}}</a>
                <pager 
                    [(offset)]="report.page.offset"
                    [limit]="report.page.limit"
                    [total]="report.users | filter: report.filter() | length">
                </pager>
            </div>
            <table class="report">
                <thead>
                    <tr>
                        <th>{{ 'line' | translate }}</th>
                        <th>{{ 'lastName' | translate }}</th>
                        <th>{{ 'firstName' | translate }}</th>
                        <th>{{ 'birthDate' | translate }}</th>
                        <th>{{ 'login' | translate }}</th>
                        <th>{{ 'profile' | translate }}</th>
                        <th>{{ 'externalId'| translate }}</th>
                        <th>{{ 'classes' | translate }}</th>
                        <th>{{ 'operation' | translate }}</th>
                    </tr>
                </thead>
                <tbody>
                <tr *ngFor="let user of report.users | filter: report.filter() | slice: report.page.offset:report.page.offset + report.page.limit">
                        <td>{{user.line}}</td>
                        <td [ngClass]="{'is-success':user.isCorrected('lastName'), 'is-danger': user.isWrong('lastName'), 'clickable':true}">
                            <span  contenteditable="true" 
                                (blur)="updateReport($event)"
                                field="lastName">
                                {{user.lastName?.length > 0 ? user.lastName : 'empty.lastName' | translate}}
                            </span>
                        </td>
                        <td [ngClass]="{'is-success':user.isCorrected('firstName'), 'is-danger': user.isWrong('firstName'), 'clickable':true}">
                            <span  contenteditable="true" 
                                (blur)="updateReport($event)"
                                field="firstName">
                                {{user.firstName?.length > 0 ? user.firstName : 'empty.firstName' | translate}}
                            </span>
                        </td>
                        <td [ngClass]="{'is-success':user.isCorrected('birthDate'), 'is-danger': user.isWrong('birthDate'), 'clickable':true}">
                            <span  contenteditable="true" 
                                (blur)="updateReport($event)"
                                field="birthDate">
                                {{user.birthDate?.length > 0 ? user.birthDate : 'empty.birthDate' | translate}}
                            </span>
                        </td>
                        <td class="clickable"><span ellipsis="expand">{{user.login}}</span></td>
                        <td>{{user.profiles.join(',')}}</td>
                        <td><span ellipsis>{{user.externalId}}</span></td>
                        <td class="clickable"><span ellipsis="expand">{{user.classesStr}}</span></td>
                        <td>{{user.state}}</td>
                    </tr>
                </tbody>
            </table>
        </step>
        <step #step5 name="{{ 'import.finish' | translate }}" [class.active]="step5.isActived">
            <h2>{{ 'import.finish' | translate }}</h2>
            <message-box *ngIf="globalError.message" [type]="'danger'" [messages]="[globalError.message]"></message-box>
        </step>
    </wizard>
    `,
    styles : [`
        .report-filter .button { margin-right: .5rem; }
        table.report { display: block; max-height : 500px; overflow: scroll; }
        table.report td.clickable:hover { border: 2px dashed orange; cursor:pointer; }
        table.report td.is-danger { border: 2px dashed red; }
        table.report td.is-success { border: 2px dashed green; }
        table.report td span[contenteditable]:focus { background : yellow; }
    `]
})
export class ImportCSV implements OnInit, OnDestroy {

    constructor(
        private route: ActivatedRoute,
        private router:Router,
        private spinner: SpinnerService,
        private bundles: BundlesService,
        private cdRef: ChangeDetectorRef){}

    private translate = (...args) => { return (<any> this.bundles.translate)(...args) }
        
    messages:Messages = new Messages();

    // Subscriberts
    private structureSubscriber: Subscription;
    private routerSubscriber:Subscription;

    @ViewChild(WizardComponent) wizardEl: WizardComponent;

    globalError:{ message:string,profile:{},reset:Function } = {
        message:undefined,
        profile:{},
        reset(){
            this.message = undefined;
            this.profile={};
        }
    };

    confirmCancel : boolean;

    profiles = { 
        Student:false, 
        Relative:false,
        Teacher:false,  
        Personnel:false, 
        Guest:false,
        inputFiles : {}, // Use to keep reference of profile's inputFile to clean FileList's attribute when inputFile is hidden
        asArray(all = false) {
            let arr = [];
            for (let p in this) {
                if (typeof this[p] == 'boolean') {
                    if (all) arr.push(p);
                    else if (this[p]) arr.push(p);
                }
            }
            return arr; 
        },
        cleanInputFile() {
            for (let p in this) {
                if (typeof this[p] == 'boolean' && !!this.inputFiles[p] ) {
                    this.inputFiles[p].value = null; // Set value to null empty the FileList. 
                }
            }
        }
    };

    isLoaded(p) {
        if (typeof this.profiles[p] == 'boolean' && !this.profiles[p]) {
            this.importInfos[p] = undefined;
            if (this.profiles.inputFiles[p]) {
                this.profiles.inputFiles[p].value = null; // Set value to null empty the FileList 
            }
        }
        return this.profiles[p]
    };


    importInfos = {
        type:'CSV', // type property must be alaways set to 'CSV' to match server API contract 
        structureId:'',
        structureExternalId:'',
        structureName:'',
        UAI:'',
        predelete:false,
        transition:false,
    };

    /* 
    * Fire when (change) on input[file]. Update profile's filelist to import
    * TODO : wrap into a component
    */
    loadFile(event) {
        let files : FileList = event.target.files;
        this.profiles.inputFiles[event.target.name] = event.target;
        if (files.length == 1) {
            this.importInfos[event.target.name] = event.target.files[0];
        }
    }

    columns = {
        // TODO : Move server-side
        requieredFields : {
            Teacher:['firstName','lastName'], 
            Student:['firstName','lastName','birthDate','classes'], 
            Relative:['firstName','lastName'],
            Personnel:['firstName','lastName'],
            Guest:['firstName','lastName'],
        },
        availableFields : {},
        mappings : {},
        profiles : [],
        hasErrors(){
            for (let p of this.profiles) {
                for (let requiered of this.requieredFields[p]) {
                    if (!Object.values(this.mappings[p]).includes(requiered)) { 
                        return true; 
                    }
                }
            }
            return false;
        },
        errors(){
            let res = {};
            for (let p of this.profiles) {
                for (let requiered of this.requieredFields[p]) {
                    if (!Object.values(this.mappings[p]).includes(requiered)) { 
                        if (res[p] == undefined ) res[p] = [];
                        res[p].push(requiered);
                    }
                }
            } 
            return res;
        },
        hasWarning(profile?:Profile) { 
            if (profile) {
                return Object.values(this.mappings[profile]).includes('ignore') ||
                    Object.values(this.mappings[profile]).includes('');
            }
            for (let p of this.profiles) {
                if (Object.values(this.mappings[p]).includes('ignore') || 
                    Object.values(this.mappings[p]).includes('')) { 
                    return true; 
                } 
            }
            return false;
        }
    };
 
    /*
    * In @mappings you wiil find for each profile a mapping of profile's classes and stutend's classes.
    * Notes that student's classes are always map to dbClasses. If you upload only one profileFile, 
    * it will be directly map against dbClasses.
    * Finaly we populate @availableClasses, for each profile, with Object.value(mappings[profile]) 
    * to be compatible with  mapping-table.component input
    */
    classes = {
        mappings: {},
        availableClasses : {},
        profiles : [],
        initAvailableClasses(profile:string ,dbClasses:Array<string>) : void {
            let availables = [''];
            Object.values(this.mappings[profile]).forEach(el => {
                if (el.trim().length > 0) {
                    availables.push(el);
                }
            });
            if (dbClasses != null) {
                dbClasses.forEach(el => {
                    if (availables.indexOf(el) == -1) {
                        availables.push(el);
                    }
                });
            }
            this.availableClasses[profile] = availables;
        },
        hasWarning(profile?:Profile) {
            if (profile) {
                return Object.values(this.mappings[profile]).includes('');
            }
            for (let p of this.profiles) {
                if (Object.values(this.mappings[p]).includes('')) { 
                    return true; 
                } 
            }
            return false;
        }
    };


    report = {
        importId: '',
        users : [],
        softErrors : {
            reasons : [],
            list : []
        },
        page : {offset: 0, limit: 30, total: 0},
        filter : User.filter,
        setFilter : User.setFilter,
        hasFilter : User.hasFilter,
        hasErrors():boolean {
            return this.softErrors.reasons.length > 0;
        },
        countByReason(r:string):number {
            return this.softErrors.list.reduce((count, item) => {
                return count + (item.reason == r ? 1 : 0);
            }, 0);
        },
        markUserErrors(errors:Error[], p:Profile):void {
            if (errors == undefined || errors == null)
                return;
            for (let err of errors) {
                let user:User = this.users.find(el => { 
                    return (el.line.toString() == err.line) && el.hasProfile(p)
                });
                user.errors.set(err.attribute, err);
                user.errors.get(err.attribute).corrected = false;
                user.reasons.push(err.reason);
            }
        },
        reset():void {
            Object.assign(this, {
                importId:'', 
                users:[], 
                softErrors : {
                    reasons:[],
                    list: []
                },
                page : {offset: 0, limit: 30, total: 0},
            });
        }
    }

    ngOnInit(): void {
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
                this.cancel();
                this.importInfos.structureId = data['structure'].id;
                this.importInfos.structureExternalId = data['structure'].externalId;
                this.importInfos.structureName = data['structure'].name;
                this.importInfos.UAI = data['structure'].UAI;
                this.cdRef.markForCheck()
            }
        })

        this.routerSubscriber = this.router.events.subscribe(e => {
            if(e instanceof NavigationEnd)
                this.cdRef.markForCheck()
        })
    }

    ngOnDestroy(): void {
    }

    /* 
    * Reset component's state. 
    * WARN : we maintain structure's informations (id, externalId, name, UAI) 
    *        because they are set by observing the route  
    */
    cancel() {
        // Hack to pass the first onInit() call
        if ('' != this.importInfos.structureId) 
            this.wizardEl.doCancel();
        
        this.globalError.reset();

        this.profiles.asArray().forEach(
            p =>
            { this.importInfos[p] = null }); // Flush loaded CSV files
        
        this.profiles.cleanInputFile();
        Object.assign(this.profiles,
            { Teacher:false, Student:false, Relative:false, Personnel:false, Guest:false, inputFiles : {}}
        );
        Object.assign(this.importInfos, {predelete:false, transition:false});
        Object.assign(this.columns, {mappings:{},availableFields:{},profiles:[]});
        Object.assign(this.classes, {mappings:{},availableClasses:{},profiles:[]});
        this.report.reset();
    }

    nextStep(activeStep: Number) {
        switch(activeStep) {
            case 0 : this.getColumsMapping(); break;
            case 1 : this.getClassesMapping(); break;
            case 2 : this.validate(); break;
            case 3 : this.import(); break;
            case 4 : break;
            default : break;
        }
    }

    previousStep (activeStep: Number) {
        this.wizardEl.doPreviousStep();
    }

    
    finish() {
        // TODO : close Wizard
    }
    
    /*
    * Next Step operations
    */
    private async getColumsMapping() {
        this.globalError.reset();
        for (let p of this.profiles.asArray()) {
            if (!this.profiles.inputFiles[p]) {
                this.globalError.message = 'missing.csv.files';
                return;
            }
        }
        let data = await ImportCSVService.getColumnsMapping(this.importInfos);
        if (data.error) {
            this.globalError.message = data.error;
        } else if (data.errors) {
            this.globalError.message = 'import.error.malformedFiles';
            this.globalError.profile = data.errors.errors; // TODO Fix server API. serve only {errors:{...}}
        } else {
            this.columns.mappings = data.mappings;
            this.columns.availableFields = data.availableFields;
            this.columns.profiles = this.profiles.asArray();
            if (this.columns.hasErrors()) {
                let errors = this.columns.errors();
                this.globalError.message = 'import.error.requieredFieldNotFound.global';
                for(let p of this.columns.profiles) {
                    if (errors[p]) {
                        this.globalError.profile[p] = 
                            errors[p].map(field => { return this.translate(field); }, this);
                    }
                }
            }
            this.wizardEl.doNextStep();
        }
        this.cdRef.markForCheck();
    
    }

    private async getClassesMapping() {
        this.globalError.reset();
        let data = await ImportCSVService.getClassesMapping(this.importInfos, this.columns.mappings);
        if (data.errors) {
            this.globalError.message = data.errors;
        } else {
            this.classes.profiles = this.profiles.asArray();
            for (let profile of this.classes.profiles) {
                if (data.classesMapping[profile] == null) {
                    // NOTE: another option is to ignore the mapping classes step
                    this.classes.mappings[profile] = data.classesMapping['dbClasses'];
                } else {
                    this.classes.mappings[profile] = data.classesMapping[profile];
                    this.classes.initAvailableClasses(profile, data.classesMapping['dbClasses']);
                }
            }
            this.wizardEl.doNextStep();
        }
        this.cdRef.markForCheck();
    }

    private async validate() {
        this.report.reset();
        this.globalError.reset();
        let data = await ImportCSVService.validate(this.importInfos, this.columns.mappings, this.classes.mappings); 
        if (data.errors) {
            this.globalError.message = 'import.error.validationGlobalError'
            this.globalError.profile = data.errors;
        } else if (!data.importId) {
            this.globalError.message = 'import.error.importIdNotFound'
        } else { 
            this.report.importId = data.importId
            for (let p of this.profiles.asArray()) {
                // merge profile's users 
                if (data[p]) {
                    this.report.users.push(...Array.from(data[p], u => new User(u)));
                }
                // merge profile's softErrors list
                if (data.softErrors && data.softErrors[p]) {
                    this.report.softErrors.list.push(...data.softErrors[p]);
                    this.report.markUserErrors(data.softErrors[p], p);
                }
            }
            // Set report total user
            this.report.page.total = this.report.users.length;
            if (data.softErrors) {
                this.report.softErrors.reasons = data.softErrors.reasons;
                this.report.setFilter('errors');
            }
        }
        this.wizardEl.doNextStep();
        this.cdRef.markForCheck();
    }
    
    private async updateReport(event) {
        // TODO replace implementation by a "contenteditable" directive that manage binding
        let tdEls = event.target.parentElement.parentElement.children
        let profile = tdEls[4].innerText
        let body = {
            line : Number.parseInt(tdEls[0].innerText)
        };
        body[event.target.getAttribute('field')] = event.target.innerText;

        let data = await ImportCSVService.updateReport('put', this.report.importId,profile, body);
        if (data.error) {
            this.globalError.message = data.error; // use an other variable to manage inline error check
        } else {
            let user:User = this.report.users.find(el => { return el.line == body.line});
            for (let p in body) {
                if ('line' != p) {
                    // Synchronize model with view.  
                    user[p] = body[p];

                    // Update error report 
                    // TODO : Control (if possible) with Feeder's validator
                    if (user.errors.get(p) && body[p].length > 0) {
                        this.report.softErrors.list.splice(
                            this.report.softErrors.list.indexOf(user.errors.get(p)),
                            1
                        );
                        user.errors.get(p).corrected = true;
                    }
                    this.cdRef.markForCheck();
                }
            }
        }
    }

    private async import() {
        this.globalError.reset();
        let data = await ImportCSVService.import(this.report.importId);
        if (data.error) {
            this.globalError.message = data.error;
        } else {
            this.wizardEl.doNextStep();
            this.cdRef.markForCheck();
        }
    }
}
