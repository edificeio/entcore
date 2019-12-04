import { OdeComponent } from './../../../core/ode/OdeComponent';
import { Component, OnDestroy, OnInit, ViewChild, Injector } from '@angular/core';
import { Data, NavigationEnd } from '@angular/router';
import {Subscription} from 'rxjs';
import {BundlesService} from 'ngx-ode-sijil';
import {ImportCSVService} from '../import-csv.service';
import {Error, Profile, User, UserEditableProps} from '../user.model';
import {WizardComponent} from 'ngx-ode-ui';
import {NotifyService} from '../../../core/services/notify.service';
import {Messages} from '../messages.model';
import {ObjectURLDirective} from 'ngx-ode-ui';
import { SpinnerService } from 'ngx-ode-ui';
import { routing } from 'src/app/core/services/routing.service';

@Component({
    selector: 'ode-import-csv',
    templateUrl: './import-csv.component.html',
    styleUrls: ['./import-csv.component.scss']
})
export class ImportCSVComponent extends OdeComponent implements OnInit, OnDestroy {

    constructor(
        injector: Injector,
        private spinner: SpinnerService,
        private bundles: BundlesService,
        private ns: NotifyService) {
            super(injector);
        }



    messages: Messages = new Messages();

    // Subscriberts
    private structureSubscriber: Subscription;
    private routerSubscriber: Subscription;

    @ViewChild(WizardComponent, { static: false })
    public wizardEl: WizardComponent;

    globalError: { message: string, param: string, profile: {}, reset: () => void } = {
        message: undefined,
        param: undefined,
        profile: {},
        reset() {
            this.message = undefined;
            this.param = undefined;
            this.profile = {};
        }
    };

    // Control displaying of cancel confirmation lightbox
    confirmCancel: boolean;

    downloadAnchor = null;
    downloadObjectUrl = '';

    profiles = {
        Student: false,
        Relative: false,
        Teacher: false,
        Personnel: false,
        Guest: false,
        inputFiles : {}, // Use to keep reference of profile's inputFile to clean FileList's attribute when inputFile is hidden
        asArray(all = false) {
            const arr = [];
            for (const p in this) {
                if (typeof this[p] == 'boolean') {
                    if (all) { arr.push(p); } else if (this[p]) { arr.push(p); }
                }
            }
            return arr;
        },
        cleanInputFile() {
            for (const p in this) {
                if (typeof this[p] == 'boolean' && !!this.inputFiles[p] ) {
                    this.inputFiles[p].value = null; // Set value to null empty the FileList.
                }
            }
        }
    };


    importInfos = {
        type: 'CSV', // type property must be alaways set to 'CSV' to match server API contract
        structureId: '',
        structureExternalId: '',
        structureName: '',
        UAI: '',
        predelete: false,
        transition: false,
    };

    columns = {
        // TODO : Move server-side
        requieredFields : {
            Teacher: ['firstName', 'lastName'],
            Student: ['firstName', 'lastName', 'birthDate', 'classes'],
            Relative: ['firstName', 'lastName'],
            Personnel: ['firstName', 'lastName'],
            Guest: ['firstName', 'lastName'],
        },
        availableFields : {},
        mappings : {},
        profiles : [],
        // @ts-ignore: this.prop is really assigned before being used
        enableButtonNextStep: this.enableButtonNextStep,
        checkErrors(globalError: GlobalError, translate: Function): boolean {
            const res = {};
            for (const p of this.profiles) {
                for (const requiered of this.requieredFields[p]) {
                    if (!Object.values(this.mappings[p]).includes(requiered)) {
                        if (res[p] === undefined ) { res[p] = []; }
                        res[p].push(requiered);
                    }
                }
                if (res[p]) {
                    globalError.profile[p] =
                        res[p].map(field => translate(field), this);
                }
            }
            if (Object.entries(res).length > 0) {
                globalError.message = 'import.error.requieredFieldNotFound.global';
            }
            return Object.entries(res).length > 0;
        },
        hasWarning(profile?: Profile) {
            if (profile) {
                return Object.values(this.mappings[profile]).includes('ignore') ||
                    Object.values(this.mappings[profile]).includes('');
            }
            for (const p of this.profiles) {
                if (Object.values(this.mappings[p]).includes('ignore') ||
                    Object.values(this.mappings[p]).includes('')) {
                    return true;
                }
            }
            return false;
        },
        selectChange(globalError: GlobalError , profile: string, value: string) {
            if (this.requieredFields[profile].every(requiered => Object.values(this.mappings[profile]).includes(requiered))) {
                if (globalError.message === 'import.error.requieredFieldNotFound.global') {
                    globalError.message = '';
                    globalError.profile[profile] = null;
                    this.enableButtonNextStep();
                }
            }
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
        init(classesMapping: ClassesMapping): void {
            // No classes found in DB and in files
            if (classesMapping == null) {
                return;
            }
            this.mappings = {};
            this.availableClasses = {};
            this.profiles = [];
            for (const p of Object.keys(classesMapping)) {
                const availables = [];
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
                            if (availables.indexOf(dbClass) === -1) {
                                availables.push(dbClass);
                            }
                        });
                    }
                }
                this.availableClasses[p] = availables;
            }
        },
        hasWarning(profile?: Profile) {
            if (profile && this.mappings[profile] != null) {
                return Object.values(this.mappings[profile]).includes('');
            }
            for (const p of this.profiles) {
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
        userOrder: '',
        softErrors : {
            reasons : [],
            list :  [],
            hardError: false
        },
        page : {offset: 0, limit: 10, total: 0},
        filter : User.filter,
        columnFilter : { lastName: '', firstName: '', profiles : '', classesStr : '' },
        setFilter : User.setFilter,
        hasFilter : User.hasFilter,
        possibleState : User.possibleState,
        ns : this.ns,
        cdRef: this.changeDetector,
        // @ts-ignore: this.prop is really assigned before being used
        enableButtonNextStep: this.enableButtonNextStep,
        init(data: {importId: string, softErrors: any}, profiles): void {
            this.importId = data.importId;
            for (const p of profiles.asArray()) {
                // merge profile's users
                if (data[p]) {
                    this.users.push(...Array.from(data[p], u => new User(u)));
                }
                // merge profile's softErrors list
                if (data.softErrors && data.softErrors[p]) {
                    // FIXME ignore displayName & login error => better to implement in serrver-side ?
                    const errors = data.softErrors[p].filter(err => !['login', 'displayName'].includes(err.attribute));
                    this.softErrors.list.push(...errors);
                    this.markUserErrors(errors, p);
                }
            }
            // Set report total user
            this.page.total = this.users.length;
            if (data.softErrors) {
                this.softErrors.reasons = data.softErrors.reasons;
            }
        },
        hasToDelete() {
            return this.users.filter(el => el.state == 'Supprimé').length > 0;
        },
        hasSoftErrors(): boolean {
            return this.softErrors.list && this.softErrors.list.length > 0;
        },
        hasSoftErrorsWithHardError(): boolean {
            return this.softErrors.list.some(softError => softError.hardError == true);
        },
        errorType : {
            'missing.student.soft' : 'warning',
            'invalid.value' : 'warning',
            'missing.attribute' : 'danger'
        },
        hasError(r: string) {
            return this.countByReason(r) > 0;
        },
        hasErrorType(r: string, type: 'warning' | 'danger') {
            return this.errorType[r] == type && this.countByReason(r) > 0;
        },
        errorReasonMessage(r: string): (string | [string, Object])[] {
            const res: (string | [string, Object])[] = [];
            if (this.softErrors.list.some(softError => softError.reason == r)) {
                // Main message
                res.push([r + '.message', { errorNumber: this.countByReason(r)}]);
                // Add server-side translations just for warning
                // because some informations can't be gracefully display in report'table
                res.push(...this.softErrors.list.filter(el => el.reason == r).map(el =>  el.translation));
            }
            return res;
        },
        countByReason(r: string): number {
            return this.softErrors.list.reduce((count, item) => {
                return count + (item.reason === r ? 1 : 0);
            }, 0);
        },
        markUserErrors(errors: Error[], p: Profile): void {
            if (errors == undefined || errors == null) {
                return;
            }
            for (const err of errors) {
                const user: User = this.users.find(el => {
                    return (el.line && el.line.toString() == err.line) && el.hasProfile(p);
                });
                user.errors.set(err.attribute, err);
                user.errors.get(err.attribute).corrected = false;
                user.reasons.push(err.reason);
            }
        },
        reset(): void {
            Object.assign(this, {
                importId: '',
                users: [],
                softErrors : {
                    reasons: [],
                    list: []
                },
                page : {offset: 0, limit: 10, total: 0},
            });
            this.setFilter('none');
        },
        async update(user: User, property: UserEditableProps) {
            try {
                await user.update(this.importId, property);
                // Update error report
                // TODO : Control (if possible) with Feeder's validator
                if (user.errors.get(property) && user[property].length > 0) {
                    this.softErrors.list.splice(
                        this.softErrors.list.indexOf(user.errors.get(property)),
                        1
                    );
                    user.errors.get(property).corrected = true;

                    if (!this.hasSoftErrorsWithHardError()) {
                        this.setFilter('none');
                        this.enableButtonNextStep();
                    }
                }
                this.ns.success('import.report.line.edit.notify.success.content'
                , 'import.report.line.edit.notify.success.title');
                this.cdRef.markForCheck();
            } catch (error) {
                this.ns.error('import.report.line.edit.notify.error.content'
                    , 'import.report.line.edit.notify.error.title');
                console.error(error);
            }
        },
        async changeState(event, user: User) {
            const newState = event.target.value;
            try {
                switch (newState) {
                    case 'Crée': await user.keep(this.importId, newState); break;
                    case 'Modifié': await user.keep(this.importId, newState); break;
                    case 'Supprimé': await user.delete(this.importId, newState); break;
                }
                this.ns.success('import.report.line.operation.edit.notify.success.content'
                    , 'import.report.line.operation.edit.notify.success.title');
            } catch (error) {
                this.ns.error('import.report.line.operation.edit.notify.error.content'
                    , 'import.report.line.operation.edit.notify.error.title');
            }
        },
        getTranslatedProfiles(profiles: Array<string>, translate: Function) {
            return profiles.map((p) => {
                return translate(p);
            }).join(',');
        },
        getAvailableClasses() {
            const res: {name: string}[] = [];
            this.users.forEach(user => {
                if (user.classesStr
                    && user.classesStr.length > 0
                    && res.filter(classe => classe.name === user.classesStr).length === 0) {
                    res.push({name: user.classesStr});
                }
            });
            return res;
        },
        setUserOrder(order: string): void {
            this.userOrder = this.userOrder === order ? '-' + order : order;
        }
    };

    @ViewChild(ObjectURLDirective, { static: false }) objectURLEl: ObjectURLDirective;

    public translate = (...args) => (this.bundles.translate as any)(...args);

    enableButtonNextStep = () => {
        this.wizardEl.canDoNext = true;
    }

    isLoaded(p) {
        if (typeof this.profiles[p] == 'boolean' && !this.profiles[p]) {
            this.importInfos[p] = undefined;
            if (this.profiles.inputFiles[p]) {
                this.profiles.inputFiles[p].value = null; // Set value to null empty the FileList
            }
        }
        return this.profiles[p];
    }
    /*
    * Fire when (change) on input[file]. Update profile's filelist to import
    * TODO : wrap into a component
    */
    loadFile(event) {
        const files: FileList = event.target.files;
        this.profiles.inputFiles[event.target.name] = event.target;
        if (files.length === 1) {
            this.importInfos[event.target.name] = event.target.files[0];
        }
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.cancel();
                this.importInfos.structureId = data.structure.id;
                this.importInfos.structureExternalId = data.structure.externalId;
                this.importInfos.structureName = data.structure.name;
                this.importInfos.UAI = data.structure.UAI;
                this.changeDetector.markForCheck();
            }
        });

        this.routerSubscriber = this.router.events.subscribe(e => {
            if (e instanceof NavigationEnd) {
                this.changeDetector.markForCheck();
            }
        });
    }


    /*
    * Reset component's state.
    * WARN : we maintain structure's informations (id, externalId, name, UAI)
    *        because they are set by observing the route
    */
    cancel() {
        // Hack to pass the first onInit() call
        if ('' != this.importInfos.structureId) {
            this.wizardEl.doCancel();
        }

        this.globalError.reset();

        this.profiles.asArray().forEach(
            p => { this.importInfos[p] = null; }); // Flush loaded CSV files

        this.profiles.cleanInputFile();
        Object.assign(this.profiles,
            { Teacher: false, Student: false, Relative: false, Personnel: false, Guest: false, inputFiles : {}}
        );
        Object.assign(this.importInfos, {predelete: false, transition: false});
        Object.assign(this.columns, {mappings: {}, availableFields: {}, profiles: []});
        Object.assign(this.classes, {mappings: {}, availableClasses: {}, profiles: []});
        this.report.reset();
    }

    nextStep(activeStep: Number) {
        switch (activeStep) {
            case 0 : this.getColumsMapping(); break;
            case 1 : this.getClassesMapping(); break;
            case 2 : this.validate(); break;
            case 3 : this.import(); break;
            case 4 : break;
            default : break;
        }
    }

    previousStep(activeStep: Number) {
        this.globalError.reset();
        this.wizardEl.doPreviousStep();
    }

    /*
    * Next Step operations
    */
    private async getColumsMapping() {
        this.globalError.reset();
        for (const p of this.profiles.asArray()) {
            if (!this.profiles.inputFiles[p]) {
                this.globalError.message = 'missing.csv.files';
                return;
            }
        }

        const data = await this.spinner.perform('portal-content', ImportCSVService.getColumnsMapping(this.importInfos));
        if (data.error) {
            this.globalError.message = data.error;
        } else if (data.errors) {
            this.globalError.message = 'import.error.malformedFiles';
            this.globalError.profile = data.errors.errors; // TODO Fix server API. serve only {errors:{...}}
        } else {
            this.columns.mappings = data.mappings;
            this.columns.availableFields = data.availableFields;
            this.columns.profiles = this.profiles.asArray();
            if (this.columns.checkErrors(this.globalError, this.translate)) {
                this.wizardEl.doNextStep(true);
            } else {
                this.wizardEl.doNextStep();
            }
        }
        this.changeDetector.markForCheck();

    }

    private async getClassesMapping() {
        if (this.columns.checkErrors(this.globalError, this.translate)) {
            return;
        }
        this.globalError.reset();
        const data: { classesMapping: ClassesMapping, errors: string } =
                await this.spinner.perform('portal-content', ImportCSVService.getClassesMapping(this.importInfos, this.columns.mappings));

        if (data.errors) {
            this.globalError.message = data.errors;
        } else {
            this.classes.init(data.classesMapping);
            this.wizardEl.doNextStep();
        }
        this.changeDetector.markForCheck();
    }

    public toggleReportFilter(r) {
        if (this.report.hasFilter('reasons', r)) {
            this.report.setFilter('none');
        } else {
            this.report.setFilter('reasons', r);
        }
    }

    private async validate() {
        this.report.reset();
        this.globalError.reset();
        const data = await this.spinner.perform('portal-content', ImportCSVService.validate(this.importInfos, this.columns.mappings, this.classes.mappings));
        if (data.errors) {
            this.globalError.message = 'import.global.error';
            this.globalError.profile = data.errors;
        } else if (!data.importId) {
            this.globalError.message = 'import.error.importIdNotFound';
        } else {
            this.report.init(data, this.profiles);
        }
        if (this.globalError.message || this.report.hasSoftErrorsWithHardError()) {
            this.wizardEl.doNextStep(true);
        } else {
            this.wizardEl.doNextStep();
        }
        this.changeDetector.markForCheck();
    }

    private async import() {
        this.globalError.reset();
        let data;
        try {
            data = await this.spinner.perform('portal-content', ImportCSVService.import(this.report.importId));
        } catch (err) {
            this.globalError.message = 'import.technical.error';
            this.globalError.param = err.message;
        }
        if (data && data.errors && data.errors.errors) {
            if (data.errors.errors['error.global']) {
                this.globalError.message = data.errors.errors['error.global'];
            } else {
                this.globalError.message = 'import.global.error';
                this.globalError.profile = data.errors.errors;
            }
        }
        this.wizardEl.doNextStep();
        this.changeDetector.markForCheck();
    }

    public downloadReport(): void {
		const bom = '\ufeff';
        const headers: string = ['lastName', 'firstName', 'profile', 'import.report.export.newclasses', 'operation']
            .map(h => this.translate(h))
            .join(';');
        const content: string = this.report.users
            .map(u => `\r\n${u.lastName};${u.firstName};${u.profiles.map(p => this.translate(p)).join('-')};${u.classesStr};${u.state}`)
            .join('');
		this.ajaxDownload(
            new Blob([`${bom}${headers}${content}`]),
            `${this.translate('import.finish.report.filename')}-${this.importInfos.structureName}.csv`);
    }
    private createDownloadAnchor(): void {
        this.downloadAnchor = document.createElement('a');
        this.downloadAnchor.style = 'display: none';
        document.body.appendChild(this.downloadAnchor);
    }

    private ajaxDownload(blob, filename): void {
        if (window.navigator.msSaveOrOpenBlob) {
            // IE specific
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            // Other browsers
            if (this.downloadAnchor === null) {
                this.createDownloadAnchor();
            }
            if (this.downloadObjectUrl !== null) {
                window.URL.revokeObjectURL(this.downloadObjectUrl);
            }
            this.downloadObjectUrl = window.URL.createObjectURL(blob);
            const anchor = this.downloadAnchor;
            anchor.href = this.downloadObjectUrl;
            anchor.download = filename;
            anchor.click();
        }
    }

    public showAlias(): boolean {
        return this.report.users.some(user => user.loginAlias);
    }
}

interface GlobalError { message: string; profile: {}; reset: Function; }
interface ClassesMapping {Student?: {}; Teacher?: {}; Relatives?: {}; Personnel?: {}; Guest?: {}; dbClasses: string[]; }