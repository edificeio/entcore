import { Component, 
    OnDestroy, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, OnChanges,
    Input, Output, ViewChild } from '@angular/core'
import { BundlesService } from 'sijil'
import { ActivatedRoute, Data, Router, NavigationEnd } from '@angular/router'
import { Subscription } from 'rxjs/Subscription'
import { routing } from '../../core/services/routing.service'
import { ImportCSVService } from './import-csv.service'
import { WizardComponent } from '../../shared/ux/components'

@Component({
    selector: 'import-csv',
    template : `
        <wizard
        (cancel)="cancel()"
        (finish)="finish()"
        (nextStep)="nextStep($event)"
        (previousStep)="previousStep($event)"
    >
        <step #step1 name="{{ 'import.files.deposit' | translate }}" [isActived]="true" [class.active]="step1.isActived">
            <h2 class="panel-header">{{ 'import.files.deposit' | translate }}</h2>
            <p *ngIf="stepErrors[0]" class="error">{{stepErrors[0]}}</p>
            <form #step1Form="ngForm">
                <h3>{{ 'import.files.deposit' | translate }}</h3>
                <form-field label="Teacher">
                <input type="checkbox"  name="teacherCB" [(ngModel)]="profiles.isLoaded.Teacher">
                <input type="file" name="Teacher" (change)="loadFile($event)" 
                        [hidden]="!profiles.isLoaded.Teacher" placeholder="{{ 'import.uplaodTeachers' | translate }}">                        
                </form-field>
                <form-field label="Student">
                <input type="checkbox" name="studentCB" [(ngModel)]="profiles.isLoaded.Student">
                    <input type="file" name="Student" (change)="loadFile($event)"
                        [hidden]="!profiles.isLoaded.Student" placeholder="{{ 'import.uplaodStudents' | translate }}">
                </form-field>
                <form-field label="Relative">
                <input type="checkbox" name="relativeCB" [(ngModel)]="profiles.isLoaded.Relative">
                    <input type="file"  name="Relative" (change)="loadFile($event)"
                    [hidden]="!profiles.isLoaded.Relative" placeholder="{{ 'import.uplaodRelatives' | translate }}">
                </form-field>
                <form-field label="Personnel">
                <input type="checkbox" name="personnelCB" [(ngModel)]="profiles.isLoaded.Personnel">
                    <input type="file"  name="Personnel" (change)="loadFile($event)"
                        [hidden]="!profiles.isLoaded.Personnel" placeholder="{{ 'import.uplaodPersonnels' | translate }}">
                </form-field>
                <form-field label="Guest">
                <input type="checkbox" name="guestCB" [(ngModel)]="profiles.isLoaded.Guest">
                    <input type="file" name="Guest" (change)="loadFile($event)" 
                        [hidden]="!profiles.isLoaded.Guest" placeholder="{{ 'import.uplaodGuests' | translate }}">
                </form-field>

                <h3>{{ 'import.parameters' | translate }}</h3>
                <form-field label="import.step1.preDeleteOption">
                    <input type="checkbox"  name="predeleteOption"  [(ngModel)]="importInfos.predelete" >
                </form-field>
                <form-field label="import.step1.transitionOption">
                    <input type="checkbox"  name="transitionOption" [(ngModel)]="importInfos.transition" >
                </form-field>
            </form>
        </step>
        <step #step2 name="{{ 'import.fields.checking' | translate }}" [class.active]="step2.isActived">
            <h2 class="panel-header">{{ 'import.fields.checking' | translate }}</h2>
            <p *ngIf="stepErrors[1]" class="error">{{stepErrors[1]}}</p>
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
            <p *ngIf="stepErrors[2]" class="error">{{stepErrors[2]}}</p>
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
            <table>
                <tr>
                    <th>{{ 'line' | translate }}</th>
                    <th>{{ 'lastname' | translate }}</th>
                    <th>{{ 'firstname' | translate }}</th>
                    <th>{{ 'birthdate' | translate }}</th>
                    <th>{{ 'profile' | translate }}</th>
                    <th>{{ 'id' | translate }}</th>
                    <th>{{ 'classes' | translate }}</th>
                    <th>{{ 'operation' | translate }}</th>
                </tr>
                <tr *ngFor="let user of report.users">
                    <td>{{user.line}}</td>
                    <td>
                        <span  contenteditable="true" 
                            (blur)="updateReport($event)"
                            field="lastName">
                            {{user.lastName}}
                        </span>
                    </td>
                    <td>
                        <span  contenteditable="true" 
                        (blur)="updateReport($event)"
                        field="firstName">
                        {{user.firstName}}
                        </span>
                    </td>
                    <td>{{user.birthDate}}</td>
                    <td>{{user.profiles.join(',')}}</td>
                    <td>{{user.id}}</td>
                    <td>{{user.classesStr}}</td>
                    <td>{{user.state}}</td>
                </tr>
            </table>
        </step>
        <step #step5 name="{{ 'import.finish' | translate }}" [class.active]="step5.isActived">
            <h2 class="panel-header">{{ 'import.finish' | translate }}</h2>
        </step>
    </wizard>
    `,
    styles : [`
        .error { color : red; font-weigth: bold; }
        td { border: 2px dashed transparent; }
        td:hover { border: 2px dashed gray; }
        td span[contenteditable]:focus {background : yellow; }
    `]
})

export class ImportCSV implements OnInit, OnDestroy {

    constructor(
        private route: ActivatedRoute,
        private router:Router,
        private bundles: BundlesService,
        private cdRef: ChangeDetectorRef){}

    translate = (...args) => { return (<any> this.bundles.translate)(...args) }

    // Subscriberts
    private structureSubscriber: Subscription;
    private routerSubscriber:Subscription;

    @ViewChild(WizardComponent) wizardEl: WizardComponent;

    stepErrors = [];
    
    profiles = { 
        isLoaded : { Teacher:false, Student:false, Relative:false, Personnel:false, Guest:false },
        asArray() { 
            let arr = [];
            for (let p in this.isLoaded) {
                if (this.isLoaded[p]) arr.push(p);
            }
            return arr; 
        }
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
        users : []
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
    * Fire when (change) on input[file]. Update profile's filelist to import
    */
    loadFile(event) {
        let files : FileList = event.target.files;  
        if (files.length == 1) {
            this.importInfos[event.target.name] = event.target.files[0];
        }
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
        this.profiles.isLoaded = { Teacher:false, Student:false, Relative:false, Personnel:false, Guest:false};

        this.importInfos.predelete = false;
        this.importInfos.transition = false;

        this.columns.mappings = {};
        this.columns.availableFields = {};
        this.classes.profiles = [];

        this.classes.mappings = {};
        this.classes.availableClasses = {};
        this.classes.profiles = [];

        this.report.importId = '';
        this.report.users = [];
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
            this.columns.profiles = Object.keys(data.mappings);
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
        let data = await ImportCSVService.validate(this.importInfos, this.columns.mappings, this.classes.mappings); 
        if (data.errors) {
            this.stepErrors[3] = data.errors;
        } else if (!data.importId) {
            this.stepErrors[3] = 'import.error.importIdNotFound'
        } else { 
            this.stepErrors[3] = null;
            this.report.importId = data.importId
            for (let p of this.columns.profiles) {
                if (!data[p]) break; // useless
                this.report.users.push(...data[p]); 
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
            let user = this.report.users.find(el => { return el.line == body.line});
            for (let p in body) {
                if ('line' != p) {
                    user[p] = body[p];
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
