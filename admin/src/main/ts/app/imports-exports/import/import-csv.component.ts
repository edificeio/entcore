import { Component, 
    OnDestroy, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, OnChanges,
    Input, Output, ViewChild, ViewChildren, QueryList } from '@angular/core'
import { Mix } from 'entcore-toolkit'
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { SpinnerService,routing } from '../../core/services'
import { BundlesService } from 'sijil'
import { ImportCSVService } from './import-csv.service'
import { User, Error, Profile, UserEditableProps } from './user.model'
import { WizardComponent } from '../../shared/ux/components'
import { NotifyService } from '../../core/services/notify.service'
import { Messages } from './messages.model'

type GlobalError = { message:string, profile:{}, reset:Function }
type ClassesMapping = {Student?:{}, Teacher?:{}, Relatives?:{}, Personnel?:{},Guest?:{}, dbClasses:string[]}

@Component({
    selector: 'import-csv',
    template : `
        <lightbox-confirm
            [show]="confirmCancel"
            [title]="'warning'"
            (onConfirm)="cancel(); confirmCancel = false;"
            (onCancel)="confirmCancel = false">
            <p>{{ 'import.cancel.message' | translate }}</p>
        </lightbox-confirm>
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
                    <message-sticker *ngIf="classes.hasWarning(profile)" [type]="'warning'" 
                        [messages]="['importClassesChecking.'+ (profile == 'Student' ? 'student' : 'generic') + '.warning']"></message-sticker>
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
            <message-box *ngIf="!report.hasErrors() && !report.hasToDelete()" [type]="'success'" [messages]="['import.report.success']"></message-box>
            <message-box *ngIf="report.hasToDelete()" [type]="'warning'" [messages]="['import.report.warning.hasToDelete']">
                <strong><a (click)="report.setFilter('state','Supprimé'); report.page.offset=0">
                    {{'import.report.displayUsersToDelete' | translate}}
                </a></strong>
            </message-box>
            <message-box *ngIf="report.hasErrors()" [type]="'warning'" [messages]="['import.report.warning.hasErrors']"></message-box>
            <div *ngIf="report.hasErrors()" class="report-filter">
                <a *ngFor="let r of report.softErrors.reasons" 
                    class="button"
                    [ngClass]="{'is-danger':report.hasErrorType(r,'danger'),
                                'is-warning':report.hasErrorType(r,'warning'),
                                'is-success':report.countByReason(r) == 0,
                                'is-outlined':!report.hasFilter('reasons',r)}"
                    (click)="report.setFilter('reasons',r); report.page.offset=0"
                >
                {{ r | translate }}
                </a>
            </div>
            <message-box *ngIf="!!report.filter().reasons" [type]="report.errorType[report.filter().reasons]" 
                [messages]="report.errorReasonMessage(report.filter().reasons)">
            </message-box>
            <div class="pager has-text-right">
                <a (click)="report.setFilter('none')">{{'pager.displayAll' | translate}}</a>
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
                    <tr>
                        <th></th>
                        <th>
                        <input type="text" [(ngModel)]="report.columnFilter.lastName" [attr.placeholder]="'search' | translate"/>
                        </th>
                        <th>
                        <input type="text" [(ngModel)]="report.columnFilter.firstName" [attr.placeholder]="'search' | translate"/>
                        </th>
                        <th colspan="5"></th>
                    </tr>
                </thead>
                <tbody>
                <tr *ngFor="let user of report.users | filter: report.filter() | filter: report.columnFilter | slice: report.page.offset:report.page.offset + report.page.limit, index as i"
                    [ngClass]="{'state-delete':user.state == 'Supprimé'}"
                >
                        <td>{{user.line}}</td>
                        <td (dblclick)="editLastName.disabled = !editLastName.disabled"
                            [ngClass]="{'is-success':user.isCorrected('lastName'), 'is-danger': user.isWrong('lastName'), 'clickable':true}">
                            <input 
                                [(ngModel)]="user.lastName" placeholder="{{'empty.lastName' | translate}}" type="text" 
                                (keyup.enter)="report.update(user, 'lastName')"
                                (blur)="report.update(user, 'lastName')"
                                disabled="true"
                                #editLastName
                            />
                        </td>
                        <td (dblclick)="editFirstName.disabled = !editFirstName.disabled"
                            [ngClass]="{'is-success':user.isCorrected('firstName'), 'is-danger': user.isWrong('firstName'), 'clickable':true}">
                            <input [(ngModel)]="user.firstName" placeholder="{{'empty.firstName' | translate}}" type="text" 
                                (keyup.enter)="report.update(user, 'firstName')"
                                (blur)="report.update(user, 'firstName')"
                                disabled="true"
                                #editFirstName
                            />            
                        </td>
                        <td (dblclick)="editBirthDate.disabled = !editBirthDate.disabled"
                            [ngClass]="{'is-success':user.isCorrected('birthDate'), 'is-danger': user.isWrong('birthDate'), 'clickable':true}">
                            <input [(ngModel)]="user.birthDate" placeholder="{{'empty.birthDate' | translate}}" type="text"
                                (keyup.enter)="report.update(user, 'birthDate')"
                                (blur)="report.update(user, 'birthDate')"
                                disabled="true"
                                #editBirthDate
                            />
                        </td>
                        <td class="clickable"><span ellipsis="expand">{{user.login}}</span></td>
                        <td>{{user.profiles?.join(',')}}</td>
                        <td><span ellipsis="expand">{{user.externalId}}</span></td>
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
        div.pager { padding: 1em 0 }
        a:hover { cursor: pointer; }
        .report-filter .button { margin-right: .5rem; }
        table.report { display: block; max-height : 500px; overflow: scroll; }
        table.report tr.state-delete { background: #fdd; }
        table.report td.clickable:hover { border: 2px dashed orange; cursor:pointer; }
        table.report td.is-danger { border: 2px dashed red; }
        table.report td.is-success { border: 2px dashed green; }
        table.report td input[disabled] { background : transparent; border:0; cursor:pointer; }
    `]
})
export class ImportCSV implements OnInit, OnDestroy {

    constructor(
        private route: ActivatedRoute,
        private router:Router,
        private spinner: SpinnerService,
        private bundles: BundlesService,
        private ns: NotifyService,
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
            this.profile = {};
        }
    };

    // Control displaying of cancel confirmation lightbox
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
        checkErrors(globalError:GlobalError, translate:Function){
            let res = {};
            for (let p of this.profiles) {
                for (let requiered of this.requieredFields[p]) {
                    if (!Object.values(this.mappings[p]).includes(requiered)) { 
                        if (res[p] == undefined ) res[p] = [];
                        res[p].push(requiered);
                    }
                }
                if (res[p]) {
                    globalError.profile[p] = 
                        res[p].map(field => { return translate(field); }, this);
                }
            } 
            if (Object.entries(res).length > 0) {
                globalError.message = 'import.error.requieredFieldNotFound.global';
            }
            return Object.entries(res).length > 0;
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
    * In @mappings you wiil find for each profile classe's mapping'.
    * Notes that student's classes are always map to dbClasses. 
    * - If you upload just 1 profile's file, it will be directly map against dbClasses.
    * - If the uploaded profile's file don't have any "classes" field class mappind is ignore for it
    * Finaly we populate @availableClasses, for each profile, by concat mappings[profile] and dbClasses
    */
    classes = {
        mappings: {},
        availableClasses : {},
        profiles : [],
        init(classesMapping:ClassesMapping) : void {
            this.mappings = {};
            this.availableClasses = {};
            this.profiles = [];    
            for (let p of Object.keys(classesMapping)) {
                if (p != 'dbClasses') {
                    this.profiles.push(p);
                    this.mappings[p] = classesMapping[p];
                    let availables = [''];
                    Object.values(this.mappings[p]).forEach(el => {
                        if (el.trim().length > 0) {
                            availables.push(el);
                        }
                    });
                    if (classesMapping.dbClasses != null) {
                        classesMapping.dbClasses.forEach(el => {
                            if (availables.indexOf(el) == -1) {
                                availables.push(el);
                            }
                        });
                    }
                    this.availableClasses[p] = availables;
                }
            }
        },
        hasWarning(profile?:Profile) {
            if (profile && this.mappings[profile] != null) {
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
            list :  []
        },
        page : {offset: 0, limit: 30, total: 0},
        filter : User.filter,
        columnFilter : { lastName: '', firstName: '' },
        setFilter : User.setFilter,
        hasFilter : User.hasFilter,
        ns : this.ns,
        init(data:{importId:string, softErrors:any}, profiles):void {
            this.importId = data.importId
            for (let p of profiles.asArray()) {
                // merge profile's users 
                if (data[p]) {
                    this.users.push(...Array.from(data[p], u => new User(u)));
                }
                // merge profile's softErrors list
                if (data.softErrors && data.softErrors[p]) {
                    this.softErrors.list.push(...data.softErrors[p]);
                    this.markUserErrors(data.softErrors[p], p);
                }
            }
            // Set report total user
            this.page.total = this.users.length;
            if (data.softErrors) {
                this.softErrors.reasons = data.softErrors.reasons;
                this.setFilter('errors');
            }
        },
        hasToDelete() {
            return this.users.filter(el => el.state == "Supprimé").length > 0;
        },
        hasErrors():boolean {
            return this.softErrors.reasons.length > 0;
        },
        errorType : {
            'missing.student.soft' : 'warning',
            'missing.attribute' : 'danger',
            'invalid.value' : 'danger'
        },
        hasErrorType (r:string, type:'warning' | 'danger') {
            return this.errorType[r] == type && this.countByReason(r) > 0;
        },
        errorReasonMessage(r:string):(string | [string,Object])[] {
            let res:(string | [string,Object])[] = [];
            // Main message
            res.push([r + '.message', { errorNumber:this.countByReason(r)}])
            // Add server-side translations just for warning 
            // because some informations can't be gracefully display in report'table
            if (this.errorType[r] == 'warning') {
                res.push(...this.softErrors.list
                    .filter(el => el.reason == r).map(el =>  el.translation));
            }
            return res;
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
        },
        update(user:User, property:UserEditableProps) {
            try {
                user.update(this.importId, property)
                // Update error report 
                // TODO : Control (if possible) with Feeder's validator
                if (user.errors.get(property) && user[property].length > 0) {
                    this.softErrors.list.splice(
                        this.softErrors.list.indexOf(user.errors.get(property)),
                        1
                    );
                    user.errors.get(property).corrected = true;
                } 
                this.ns.success('import.report.notifySuccessEdit');
            } catch (error) {
                this.ns.error('import.report.notifyErrorEdit');
            }
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

        let data = await this.spinner.perform('portal-content', ImportCSVService.getColumnsMapping(this.importInfos));
        if (data.error) {
            this.globalError.message = data.error;
        } else if (data.errors) {
            this.globalError.message = 'import.error.malformedFiles';
            this.globalError.profile = data.errors.errors; // TODO Fix server API. serve only {errors:{...}}
        } else {
            this.columns.mappings = data.mappings;
            this.columns.availableFields = data.availableFields;
            this.columns.profiles = this.profiles.asArray();
            this.columns.checkErrors(this.globalError, this.translate)
            this.wizardEl.doNextStep();
        }
        this.cdRef.markForCheck();
    
    }

    private async getClassesMapping() {
        if (this.columns.checkErrors(this.globalError, this.translate)) {
            return;
        }
        this.globalError.reset();
        let data:{ classesMapping:ClassesMapping, errors:string } = 
                await this.spinner.perform('portal-content', ImportCSVService.getClassesMapping(this.importInfos, this.columns.mappings));

        if (data.errors) {
            this.globalError.message = data.errors;
        } else {
            this.classes.init(data.classesMapping);
            this.wizardEl.doNextStep();
        }
        this.cdRef.markForCheck();
    }

    private async validate() {
        this.report.reset();
        this.globalError.reset();
        let data = await this.spinner.perform('portal-content', ImportCSVService.validate(this.importInfos, this.columns.mappings, this.classes.mappings)); 
        if (data.errors) {
            this.globalError.message = 'import.error.validationGlobalError'
            this.globalError.profile = data.errors;
        } else if (!data.importId) {
            this.globalError.message = 'import.error.importIdNotFound'
        } else { 
            this.report.init(data, this.profiles);
        }
        this.wizardEl.doNextStep();
        this.cdRef.markForCheck();
    }
    
    private async import() {
        this.globalError.reset();
        let data = await this.spinner.perform('portal-content', ImportCSVService.import(this.report.importId));
        if (data.error) {
            this.globalError.message = data.error;
        } else {
            this.wizardEl.doNextStep();
            this.cdRef.markForCheck();
        }
    }
}
