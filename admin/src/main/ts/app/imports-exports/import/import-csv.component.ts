import { Component, 
    OnDestroy, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, OnChanges,
    Input, Output, ViewChild } from '@angular/core'
import { BundlesService } from 'sijil'
import { Mix } from 'entcore-toolkit'
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { SpinnerService,routing } from '../../core/services'
import { ImportCSVService } from './import-csv.service'
import { User, Error, Profile } from './user.model'
import { WizardComponent } from '../../shared/ux/components'

@Component({
    selector: 'import-csv',
    template : `
        <light-box [show]="confirmCancel" (onClose)="confirmCancel = false">
            <div class="container">
                <h2>{{'import.cancel.header' | translate}}</h2>
                <article class="message is-warning">
                    {{'import.cancel.message' | translate}}
                </article>
                    <button 
                        (click)="confirmCancel = false"
                        [title]="'import.backToImport'  | translate">
                        {{'import.cancel.backToImport' | translate}}
                    </button>
                    <button 
                        (click)="cancel();confirmCancel = false;"
                        [title]="'import.cancel.confirm' | translate">
                        {{'import.cancel.confirm'  | translate}}
                    </button>
            </div>
        </light-box>
        <wizard
            (cancel)="confirmCancel=true"
            (finish)="finish()"
            (nextStep)="nextStep($event)"
            (previousStep)="previousStep($event)"
        >
        <step #step1 name="{{ 'import.files.deposit' | translate }}" [isActived]="true" [class.active]="step1.isActived">
            <h2>{{ 'import.files.deposit' | translate }}</h2>
            <article *ngIf="stepErrors[0]" class="message is-danger">{{stepErrors[0]}}</article>
            <h3>{{ 'import.files.deposit' | translate }}</h3>
            <form-field *ngFor="let p of profiles.asArray(true)" label="{{p}}">
                <input type="checkbox" name="{{p + 'CB'}}" [(ngModel)]="profiles[p]">
                <input type="file" name="{{p}}" (change)="loadFile($event)" 
                    [hidden]="!isLoaded(p)" placeholder="{{ 'import.uplaod' + p | translate }}">
            </form-field>
            <h3>{{ 'import.parameters' | translate }}</h3>
            <form-field label="import.step1.preDeleteOption">
                <input type="checkbox"  name="predeleteOption"  [(ngModel)]="importInfos.predelete" >
            </form-field>
            <form-field label="import.step1.transitionOption">
                <input type="checkbox"  name="transitionOption" [(ngModel)]="importInfos.transition" >
            </form-field>
        </step>
        <step #step2 name="{{ 'import.fields.checking' | translate }}" [class.active]="step2.isActived">
            <h2 class="panel-header">{{ 'import.fields.checking' | translate }}</h2>
            <article *ngIf="stepErrors[1]" class="message is-danger">{{stepErrors[1]}}</article>
            <panel-section *ngFor="let profile of columns.profiles" section-title="{{'import.'+profile+'File'}}" [folded]="true">
                <mappings-table 
                    [headers]="['import.fieldFromFile','import.fieldToMap']"
                    [mappings]="columns.mappings[profile]"
                    [availables]="columns.availableFields[profile]"
                >
                </mappings-table>
            </panel-section>
        </step>
        <step #step3 name="{{ 'import.classCheckingBetweenFiles' | translate }}" [class.active]="step3.isActived">
            <h2 class="panel-header">{{ 'import.class.checking' | translate }}</h2>
            <article *ngIf="stepErrors[2]" class="message is-danger">{{stepErrors[2]}}</article>
            <panel-section *ngFor="let profile of classes.profiles" section-title="{{'import.'+profile+'File'}}" [folded]="true"> 
                <mappings-table 
                    [headers]="['import.classFromFile','import.classToMap']"
                    [mappings]="classes.mappings[profile]"
                    [availables]="classes.availableClasses[profile]"
                >
                </mappings-table>
            </panel-section>
        </step>
        <step #step4 name="{{ 'import.report' | translate }}" [class.active]="step4.isActived">
            <h2 class="panel-header">{{ 'import.report' | translate }}</h2>
            <div *ngIf="report.hasErrors()" class="container report-filter">
                <a *ngFor="let r of report.softErrors.reasons" 
                    class="button is-outline"
                    [ngClass]="{'is-danger':report.countByReason(r) > 0, 'is-success':report.countByReason(r) == 0}"
                    (click)="report.setFilter('reasons',r)"
                >
                {{ [r, report.countByReason(r)] | translate }}
                    &nbsp;
                </a>
            </div>
            <div class="container has-text-right">
                <a (click)="report.setFilter('none')">{{'view.all' | translate}}</a>
            </div>
            <table class="report">
                <thead>
                    <tr>
                        <th>{{ 'line' | translate }}</th>
                        <th>{{ 'lastname' | translate }}</th>
                        <th>{{ 'firstname' | translate }}</th>
                        <th>{{ 'birthdate' | translate }}</th>
                        <th>{{ 'login' | translate }}</th>
                        <th>{{ 'profile' | translate }}</th>
                        <th>{{ 'externalId'| translate }}</th>
                        <th>{{ 'classes' | translate }}</th>
                        <th>{{ 'operation' | translate }}</th>
                    </tr>
                </thead>
                <tbody>
                <tr *ngFor="let user of (report.users | filter: report.filter())">
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
            <h2 class="panel-header">{{ 'import.finish' | translate }}</h2>
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
        private bundles: BundlesService,
        private spinner: SpinnerService,
        private cdRef: ChangeDetectorRef){}

    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    // Subscriberts
    private structureSubscriber: Subscription;
    private routerSubscriber:Subscription;

    @ViewChild(WizardComponent) wizardEl: WizardComponent;

    stepErrors = [];
    
    profiles = { 
        Teacher:false, 
        Student:false, 
        Relative:false, 
        Personnel:false, 
        Guest:false ,
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
        availableFields : {},
        mappings : {},
        profiles : []
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
        }
    };

    report = {
        importId: '',
        users : [],
        softErrors : {
            reasons : [],
            list : []
        },
        filter : User.filter,
        setFilter : User.setFilter,
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
                }
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
        
        this.stepErrors = [];

        this.profiles.asArray().forEach(p =>{ this.importInfos[p] = null }); // Flush loaded CSV files
        
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
        let data = await ImportCSVService.getColumnsMapping(this.importInfos);
        if (data.error) {
            this.stepErrors[0] = data.error;
        } else if (!data.mappings || !data.availableFields) {
            this.stepErrors[0] = "import.error.noColumnsMappingFound" // TODO : check if possible 
        } else {
            this.columns.mappings = data.mappings;
            this.columns.availableFields = data.availableFields;
            this.columns.profiles = this.profiles.asArray();
            this.stepErrors[0] = null;
            this.wizardEl.doNextStep();
        }
        this.cdRef.markForCheck();
    
    }

    private async getClassesMapping() {
        let data = await ImportCSVService.getClassesMapping(this.importInfos, this.columns.mappings);
        if (data.errors) {
            this.stepErrors[1] = data.errors;
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
            this.stepErrors[1] = null;
            this.wizardEl.doNextStep();
        }
        this.cdRef.markForCheck();
    }

    private async validate() {
        this.report.reset();
        let data = await ImportCSVService.validate(this.importInfos, this.columns.mappings, this.classes.mappings); 
        if (data.errors) {
            this.stepErrors[3] = data.errors;
        } else if (!data.importId) {
            this.stepErrors[3] = 'import.error.importIdNotFound'
        } else { 
            this.stepErrors[3] = null;
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
        if (data.errors) {
            this.stepErrors[3] = data.errors;
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
        let data = await ImportCSVService.import(this.report.importId);
        if (data.errors) {
            this.stepErrors[3] = data.errors;
        } else {
            this.wizardEl.doNextStep();
            this.cdRef.markForCheck();
        }
    }
}
