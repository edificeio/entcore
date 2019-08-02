import { Component, OnDestroy, OnInit, ChangeDetectorRef, ViewChild } from '@angular/core'
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { SpinnerService,routing } from '../../core/services'
import { BundlesService } from 'sijil'
import { ImportCSVService } from './import-csv.service'
import { User, Error, Profile, UserEditableProps } from './user.model'
import { WizardComponent } from '../../shared/ux/components'
import { NotifyService } from '../../core/services/notify.service'
import { Messages } from './messages.model'
import { ObjectURLDirective } from '../../shared/ux/directives/object-url.directive'

@Component({
    selector: 'import-csv',
    template : `
        <lightbox-confirm
            [show]="confirmCancel"
            [lightboxTitle]="'warning'"
            (onConfirm)="cancel(); confirmCancel = false;"
            (onCancel)="confirmCancel = false">
            <p>{{ 'import.cancel.message' | translate }}</p>
        </lightbox-confirm>
        <wizard
            (cancel)="confirmCancel=true"
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
                <label class="step1-file__label">{{'import.file.'+p | translate}}</label>
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
                <span panel-section-header-icons>
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
            <message-box *ngIf="classes.noClassesMapping()" [type]="'success'" [messages]="['import.classesChecking.success.noClasses']"></message-box>
            <message-box *ngIf="!classes.hasWarning() && !classes.noClassesMapping()" [type]="'success'" [messages]="['import.classesChecking.success']"></message-box>
            <message-box *ngIf="globalError.message" [type]="'danger'" [messages]="[globalError.message]"></message-box>
            <message-box *ngIf="classes.hasWarning()" [type]="'warning'" [messages]="['import.file.warning']"></message-box>
            <panel-section *ngFor="let profile of classes.profiles" section-title="{{'import.file.'+ profile}}" [folded]="true"> 
                <span panel-section-header-icons>
                    <message-sticker *ngIf="classes.hasWarning(profile)" [type]="'warning'" 
                        [messages]="['importClassesChecking.'+ (profile == 'Student' ? 'student' : 'generic') + '.warning']"></message-sticker>
                </span>

                <mappings-table
                    [headers]="['import.classFromFile','import.classToMap']"
                    [mappings]="classes.mappings[profile]"
                    [availables]="classes.availableClasses[profile]"
                    [emptyLabel]="'import.classesChecking.warning.create'"
                    [emptyWarning]="'import.classesChecking.warning.create.1'"
                    [mappingsKeySort]="true"
                >
                </mappings-table>
            </panel-section>
        </step>
        <step #step4 name="{{ 'import.report' | translate }}" [class.active]="step4.isActived">
            <h2>{{ 'import.report' | translate }}</h2>
            <message-box *ngIf="!report.hasErrors() && !report.hasToDelete() && !globalError.message" [type]="'success'" [messages]="['import.report.success']">
            </message-box>
            <message-box *ngIf="report.hasToDelete()" [type]="'warning'" [messages]="['import.report.warning.hasToDelete']">
                <strong><a (click)="report.setFilter('state','Supprimé'); report.page.offset=0">
                    {{'import.report.displayUsersToDelete' | translate}}
                </a></strong>
            </message-box>
            <message-box *ngIf="report.hasErrors() && !report.hasSoftErrorsWithHardError()" [type]="'warning'" [messages]="['import.report.warning.hasErrors']">
            </message-box>
            <message-box *ngIf="report.hasSoftErrorsWithHardError()" [type]="'danger'" [messages]="['import.report.softError.hardError']">
            </message-box>
            
            <div *ngIf="report.hasErrors()" class="report-filter">
                <a *ngFor="let r of report.softErrors.reasons" 
                    class="button"
                    [ngClass]="{'is-danger':report.hasErrorType(r,'danger'),
                                'is-warning':report.hasErrorType(r,'warning'),
                                'is-success':report.countByReason(r) == 0,
                                'is-outlined':!report.hasFilter('reasons',r)}"
                    (click)="toggleReportFilter(r); report.page.offset=0">
                {{ r | translate }}
                </a>
            </div>
            
            <message-box *ngIf="!!report.filter().reasons" [type]="report.errorType[report.filter().reasons]" 
                [messages]="report.errorReasonMessage(report.filter().reasons)">
            </message-box>

            <message-box *ngIf="globalError.message" [type]="'danger'" [messages]="[globalError.message]"></message-box>
            
            <panel-section *ngFor="let key of globalError.profile | keys" section-title="{{'import.file.'+ key}}" [folded]="true"> 
                <message-box [type]="'danger'" [messages]="globalError.profile[key]"></message-box>
            </panel-section>

            <div *ngIf="!globalError.message">
                <div class="pager has-text-right">
                    <a (click)="report.setFilter('none')">{{'pager.displayAll' | translate}}</a>
                    <pager 
                        [(offset)]="report.page.offset"
                        [limit]="report.page.limit"
                        [total]="report.users | filter: report.filter() | filter: report.columnFilter | length">
                    </pager>
                </div>
                <table class="report">
                    <thead>
                        <tr>
                            <th>{{ 'line' | translate }}</th>
                            <th>{{ 'operation' | translate }}</th>
                            <th>{{ 'lastName' | translate }}</th>
                            <th>{{ 'firstName' | translate }}</th>
                            <th>{{ 'birthDate' | translate }}</th>
                            <th>{{ 'login' | translate }}</th>
                            <th>{{ 'profile' | translate }}</th>
                            <th>{{ 'externalId.short'| translate }}</th>
                            <th>{{ 'classes' | translate }}</th>
                        </tr>
                        <tr>
                            <th></th>
                            <th></th>
                            <th>
                                <input type="text" [(ngModel)]="report.columnFilter.lastName" [attr.placeholder]="'search' | translate"/>
                            </th>
                            <th>
                                <input type="text" [(ngModel)]="report.columnFilter.firstName" [attr.placeholder]="'search' | translate"/>
                            </th>
                            <th></th>
                            <th></th>
                            <th>
                                <select [(ngModel)]="report.columnFilter.profiles">
                                    <option [value]=""></option>
                                    <option *ngFor="let p of columns.profiles" [value]="p">
                                        {{ p | translate }}
                                    </option>
                                </select>
                            </th>
                            <th></th>
                            <th>
                                <select [(ngModel)]="report.columnFilter.classesStr">
                                    <option [value]=""></option>
                                    <option *ngFor="let c of report.getAvailableClasses()" [value]="c">
                                        {{ c }}
                                    </option>
                                </select>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                    <tr *ngFor="let user of report.users | filter: report.filter() | filter: report.columnFilter | slice: report.page.offset:report.page.offset + report.page.limit, index as i"
                        [ngClass]="{'state-delete':user.state == 'Supprimé', 'is-danger': user.hasErrorsNotCorrected(), 'is-warning': user.hasWarnings()}"
                    >
                            <td>{{user.line}}</td>
                            <td>
                                <select (change)="report.changeState($event, user)">
                                    <option *ngFor="let state of report.possibleState(user.state)" [value]="state" [selected]="state === user.state">
                                        {{state}}
                                    </option>
                                </select>
                            </td>
                            <td [ngClass]="{'is-success':user.isCorrected('lastName'), 'is-danger': user.isWrong('lastName'), 'clickable':true}">
                                <input [(ngModel)]="user.lastName" 
                                    placeholder="{{'empty.lastName' | translate}}" 
                                    type="text" 
                                    (keyup.enter)="report.update(user, 'lastName'); lastNameValidateIcon.show = false; editLastName.disabled = true; lastNameEditIcon.show = true;"
                                    disabled="true"
                                    #editLastName />
                                <i #lastNameEditIcon 
                                    class="fa fa-pencil" 
                                    [ngStyle]="{'display': lastNameValidateIcon.show == true ? 'none' : 'inline'}"
                                    (click)="editLastName.disabled = undefined; lastNameEditIcon.show = false; lastNameValidateIcon.show = true"></i>
                                <i #lastNameValidateIcon
                                    class="fa fa-check"
                                    [ngStyle]="{'display': lastNameValidateIcon.show == undefined || lastNameValidateIcon.show == false ? 'none' : 'inline'}"
                                    (click)="report.update(user, 'lastName'); lastNameValidateIcon.show = false; editLastName.disabled = true; lastNameEditIcon.show = true;"></i>
                            </td>
                            <td [ngClass]="{'is-success':user.isCorrected('firstName'), 'is-danger': user.isWrong('firstName'), 'clickable':true}">
                                <input [(ngModel)]="user.firstName" 
                                    placeholder="{{'empty.firstName' | translate}}" 
                                    type="text" 
                                    (keyup.enter)="report.update(user, 'firstName'); firstNameValidateIcon.show = false; editFirstName.disabled = true; firstNameEditIcon.show = true;"
                                    disabled="true"
                                    #editFirstName />
                                <i #firstNameEditIcon 
                                    class="fa fa-pencil" 
                                    [ngStyle]="{'display': firstNameValidateIcon.show == true ? 'none': 'inline'}"
                                    (click)="editFirstName.disabled = undefined; firstNameEditIcon.show = false; firstNameValidateIcon.show = true;"></i>
                                <i #firstNameValidateIcon
                                    class="fa fa-check"
                                    [ngStyle]="{'display': firstNameValidateIcon.show ? 'inline': 'none'}"
                                    (click)="report.update(user, 'firstName'); firstNameValidateIcon.show = false; editFirstName.disabled = true; firstNameEditIcon.show = true;"></i>
                            </td>
                            <td [ngClass]="{'is-success':user.isCorrected('birthDate'), 'is-danger': user.isWrong('birthDate'), 'clickable':true}">
                                <input [(ngModel)]="user.birthDate"
                                    class="report_birthday--input" 
                                    placeholder="{{'empty.birthDate' | translate}}" 
                                    type="text" 
                                    (keyup.enter)="report.update(user, 'birthDate'); birthDateValidateIcon.show = false; editBirthDate.disabled = true; birthDateEditIcon.show = true;"
                                    disabled="true"
                                    #editBirthDate />
                                <i #birthDateEditIcon 
                                    class="fa fa-pencil" 
                                    [ngStyle]="{'display': birthDateValidateIcon.show ? 'none' : 'inline'}"
                                    (click)="editBirthDate.disabled = undefined; birthDateEditIcon.show = false; birthDateValidateIcon.show = true"></i>
                                <i #birthDateValidateIcon
                                    class="fa fa-check"
                                    [ngStyle]="{'display': birthDateValidateIcon.show == undefined || birthDateValidateIcon.show == false ? 'none' : 'inline'}"
                                    (click)="report.update(user, 'birthDate'); birthDateValidateIcon.show = false; editBirthDate.disabled = true; birthDateEditIcon.show = true;"></i>
                            </td>
                            <td><span>{{user.login}}</span></td>
                            <td>{{ report.getTranslatedProfiles(user.profiles, translate) }}</td>
                            <td><span>{{user.externalId}}</span></td>
                            <td><span>{{user.classesStr}}</span></td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </step>
        <step #step5 name="{{ 'import.finish' | translate }}" [class.active]="step5.isActived">
            <div *ngIf="globalError.message else noGlobalError">
                <h2>{{ 'import.finish.error' | translate }}</h2>            
                <message-box [type]="'danger'" [messages]="[globalError.message]"></message-box>
                <button 
                    (click)="cancel()"
                    [title]="'import.finish.otherImport' | translate">
                    {{ 'import.finish.otherImport' | translate }}
                </button>
            </div>
            <ng-template #noGlobalError>
                <h2>{{ 'import.finish' | translate }}</h2>            
                <message-box [type]="'success'" [messages]="['import.finish.success']">
                </message-box>
                <button 
                    (click)="cancel()"
                    [title]="'import.finish.otherImport' | translate">
                    {{ 'import.finish.otherImport' | translate }}
                </button>
                <button
                    [routerLink]="'/admin/' + importInfos.structureId + '/users'"
                    [queryParams]="{sync: true}"
                    [title]="'import.finish.usersList' | translate">
                    {{ 'import.finish.usersList' | translate }}
                </button>
                <button
                    (click)="downloadReport()">
                    {{ 'import.finish.report' | translate }}
                </button>
            </ng-template>
        </step>
    </wizard>
    `,
    styles : [`
        div.pager { padding: 1em 0 }
        a:hover { cursor: pointer; }
        .report-filter .button { margin-right: .5rem; }
        table.report { table-layout: auto; }
        table.report tr.state-delete { background: #fdd; }
        table.report tr.is-danger { background: #f7e5e5; }
        table.report tr.is-warning { background: #fff7dc; }
        table.report td.clickable:hover { border: 2px dashed #ff8352; cursor:pointer; }
        table.report td.is-danger { border: 2px dashed indianred; }
        table.report td.is-success { border: 2px dashed mediumseagreen; }
        table.report td input[disabled] { background : transparent; border:0; cursor:pointer; opacity: 1;}
        table.report td i.fa { padding: 5px; }
        table.report td i.fa-check { font-size: 1.2em; }
        .step1-file__label {min-width: 200px; display: inline-block;}
        .report_birthday--input {width: 100px;}
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

    @ViewChild(WizardComponent) 
    public wizardEl: WizardComponent;
    
    enableButtonNextStep = () => {
        this.wizardEl.canDoNext = true;
    }

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

    downloadAnchor = null;
    downloadObjectUrl: string = '';

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
    * - If the uploaded profile's file don't have any "classes" field class mapping is ignore for it
    * Finaly we populate @availableClasses, for each profile, by concat mappings[profile] and dbClasses
    */
    classes = {
        mappings: {},
        availableClasses : {},
        profiles : [],
        init(classesMapping:ClassesMapping) : void {
            // No classes found in DB and in files
            if (classesMapping == null) {
                return;
            }
            this.mappings = {};
            this.availableClasses = {};
            this.profiles = [];    
            for (let p of Object.keys(classesMapping)) {
                let availables = [''];
                if (p != 'dbClasses') {
                    this.profiles.push(p);
                    this.mappings[p] = classesMapping[p];
                    Object.keys(this.mappings[p]).forEach(el => {
                        if (el.trim().length > 0) {
                            availables.push(el);
                        }
                    });
                    if (classesMapping.dbClasses) {
                        classesMapping.dbClasses.forEach(dbClass => {
                            if (availables.indexOf(dbClass) == -1) {
                                availables.push(dbClass);
                            }    
                        });
                    }
                }
                this.availableClasses[p] = availables;
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
        },
        // Use for Guest or Personnel profiles
        noClassesMapping() {
            return !this.hasWarning() && this.profiles.length === 0;
        }
    };


    report = {
        importId: '',
        users : [],
        softErrors : {
            reasons : [],
            list :  [],
            hardError: false
        },
        page : {offset: 0, limit: 30, total: 0},
        filter : User.filter,
        columnFilter : { lastName: '', firstName: '', profiles : '', classesStr : '' },
        setFilter : User.setFilter,
        hasFilter : User.hasFilter,
        possibleState : User.possibleState,
        ns : this.ns,
        cdRef: this.cdRef,
        enableButtonNextStep: this.enableButtonNextStep,
        init(data:{importId:string, softErrors:any}, profiles):void {
            this.importId = data.importId
            for (let p of profiles.asArray()) {
                // merge profile's users 
                if (data[p]) {
                    this.users.push(...Array.from(data[p], u => new User(u)));
                }
                // merge profile's softErrors list
                if (data.softErrors && data.softErrors[p]) {
                    // FIXME ignore displayName & login error => better to implement in serrver-side ?
                    let errors = data.softErrors[p].filter(err => !['login','displayName'].includes(err.attribute));
                    this.softErrors.list.push(...errors);
                    this.markUserErrors(errors, p);
                    this.softErrors.hardError = data.softErrors[p].some(softError => softError['hardError'] == true);
                }
            }
            // Set report total user
            this.page.total = this.users.length;
            if (data.softErrors) {
                this.softErrors.reasons = data.softErrors.reasons;
            }
        },
        hasToDelete() {
            return this.users.filter(el => el.state == "Supprimé").length > 0;
        },
        hasErrors():boolean {
            return this.softErrors.list && this.softErrors.list.length > 0;
        },
        hasSoftErrorsWithHardError(): boolean {
            return this.softErrors.hardError;
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
                res.push(...this.softErrors.list.filter(el => el.reason == r).map(el =>  el.translation));
            }
            return res;
        },
        countByReason(r:string):number {
            return this.softErrors.list.reduce((count, item) => {
                return count + (item.reason === r ? 1 : 0);
            }, 0);
        },
        markUserErrors(errors:Error[], p:Profile):void {
            if (errors == undefined || errors == null)
                return;
            for (let err of errors) {
                let user:User = this.users.find(el => { 
                    return (el.line && el.line.toString() == err.line) && el.hasProfile(p)
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
            this.setFilter('none');
        },
        async update(user:User, property:UserEditableProps) {
            try {
                await user.update(this.importId, property)
                // Update error report 
                // TODO : Control (if possible) with Feeder's validator
                if (user.errors.get(property) && user[property].length > 0) {
                    this.softErrors.list.splice(
                        this.softErrors.list.indexOf(user.errors.get(property)),
                        1
                    );
                    user.errors.get(property).corrected = true;

                    if (this.softErrors.list.length == 0) {
                        this.softErrors.hardError = false;
                        this.enableButtonNextStep();
                    }
                }
                this.ns.success('import.report.line.edit.notify.success.content'
                , 'import.report.line.edit.notify.success.title');
                this.cdRef.markForCheck();
            } catch (error) {
                this.ns.error('import.report.line.edit.notify.error.content'
                    , 'import.report.line.edit.notify.error.title'
                    , error);
            }
        },
        async changeState(event, user:User) {
            let newState = event.target.value;
            try {
                switch (newState) {
                    case 'Crée': await user.keep(this.importId); break;
                    case 'Modifié': await user.keep(this.importId); break;
                    case 'Supprimé': await user.delete(this.importId); break; 
                }
                this.ns.success('import.report.notifySuccessEdit');
            } catch (error) {
                this.ns.error('import.report.notifyErrorEdit');
            }
        },
        getTranslatedProfiles(profiles: Array<string>, translate: Function) {
            return profiles.map((p) => {
                return translate(p)
            }).join(',');
        },
        getAvailableClasses() {
            let res: string[] = [];
            this.users.forEach(user => {
                if (user.classesStr 
                    && user.classesStr.length > 0 
                    && res.indexOf(user.classesStr) == -1) {
                    res.push(user.classesStr);
                }
            });
            return res;
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
        this.globalError.reset();
        this.wizardEl.doPreviousStep();
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

    public toggleReportFilter(r) {
        if(this.report.hasFilter('reasons', r)) {
            this.report.setFilter('none');
        } else {
            this.report.setFilter('reasons', r);
        }
    }

    private async validate() {
        this.report.reset();
        this.globalError.reset();
        let data = await this.spinner.perform('portal-content', ImportCSVService.validate(this.importInfos, this.columns.mappings, this.classes.mappings)); 
        if (data.errors) {
            this.globalError.message = 'import.global.error'
            this.globalError.profile = data.errors;
        } else if (!data.importId) {
            this.globalError.message = 'import.error.importIdNotFound'
        } else { 
            this.report.init(data, this.profiles);
        }
        if (this.globalError.message || this.report.hasSoftErrorsWithHardError()) {
            this.wizardEl.doNextStep(true);
        } else {
            this.wizardEl.doNextStep();
        }
        this.cdRef.markForCheck();
    }
    
    @ViewChild(ObjectURLDirective) objectURLEl: ObjectURLDirective;
    
    private async import() {
        this.globalError.reset();
        let data = await this.spinner.perform('portal-content', ImportCSVService.import(this.report.importId));
        if (data.errors && data.errors.errors) {
            this.globalError.message = data.errors.errors['error.global'];
        }
        this.wizardEl.doNextStep();
        this.cdRef.markForCheck();
    }

    public downloadReport(): void {
		const bom: string = '\ufeff';
        const headers: string = ['lastName', 'firstName', 'profile', 'import.report.export.newclasses', 'operation']
            .map(h => this.translate(h))
            .join(';');
        const content: string = this.report.users
            .map(u => `\r\n${u.lastName};${u.firstName};${u.profiles.map(p => this.translate(p)).join("-")};${u.classesStr};${u.state}`)
            .join('');
		this.ajaxDownload(
            new Blob([`${bom}${headers}${content}`]), 
            `${this.translate('import.finish.report.filename')}-${this.importInfos.structureName}.csv`);
    };
    
    private createDownloadAnchor(): void {
        this.downloadAnchor = document.createElement('a');
        this.downloadAnchor.style = "display: none";
        document.body.appendChild(this.downloadAnchor);
    }

    private ajaxDownload(blob, filename): void {
        if (window.navigator.msSaveOrOpenBlob) {
            //IE specific
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            //Other browsers
            if (this.downloadAnchor === null)
                this.createDownloadAnchor()
            if (this.downloadObjectUrl !== null)
                window.URL.revokeObjectURL(this.downloadObjectUrl);
            this.downloadObjectUrl = window.URL.createObjectURL(blob)
            let anchor = this.downloadAnchor
            anchor.href = this.downloadObjectUrl
            anchor.download = filename
            anchor.click()
        }
    }
}

type GlobalError = { message:string, profile:{}, reset:Function }
type ClassesMapping = {Student?:{}, Teacher?:{}, Relatives?:{}, Personnel?:{},Guest?:{}, dbClasses:string[]}
