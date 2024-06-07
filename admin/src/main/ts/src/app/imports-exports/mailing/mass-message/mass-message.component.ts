import { ChangeDetectionStrategy, Component, ElementRef, Injector, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Data, NavigationEnd } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { BundlesService } from 'ngx-ode-sijil';
import { FilterPipe, SelectOption, SpinnerService } from 'ngx-ode-ui';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { UserModel } from 'src/app/core/store/models/user.model';
import { NotifyService } from '../../../core/services/notify.service';
import { routing } from '../../../core/services/routing.service';
import { UserlistFiltersService } from '../../../core/services/userlist.filters.service';
import { MassMessageService } from '../mass-message.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'ode-mass-mail',
    templateUrl: './mass-message.component.html',
    host: {
        '(document:click)': 'onClick($event)',
    },
    styleUrls: ['./mass-message.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush


})
export class MassMessageComponent extends OdeComponent implements OnInit, OnDestroy {

    @ViewChild('filtersDiv') filtersDivRef: ElementRef;
    @ViewChild('filtersToggle') filtersToggleRef;
    users: UserModel[];
    htmlTemplate: string;
    tempHtmlTemplate: string;
    inputValue:string = "Objet du message";
    importedData:{ headers?: FieldAndToken[], rows?: DynamicMessageReceiver [], template?: String, messageSubject?: String};
    countUsers = 0;
    messagesSent = false;
    sendFailed = false;
    userOrder: string;
    structureId: string;
    show = false;
    private deselectItem = false;
    dateFilter: string;
    dateFormat: Intl.DateTimeFormat;
    showConfirmation = false;
    isButtonVisible = false;
    downloadAnchor = null;
    downloadObjectUrl = null;

    showImportedFileInTable = false;

    private structureSubscriber: Subscription;
    isSourceAutomatic:boolean = false;

    

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
        requiredFields: ['login'],
        availableFields : {},
        mappings : [],
        checkErrors(globalError: GlobalError, translate: Function): boolean {
            const res = {};

            if (Object.entries(res).length > 0) {
                globalError.message = 'import.error.requieredFieldNotFound.global';
            }
            return Object.entries(res).length > 0;
        },
    };

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

    translate = (...args) => {
        return (this.bundles.translate as any)(...args);
    }

    showTemplateEditor = false;

    constructor(
        injector: Injector,
        public userlistFiltersService: UserlistFiltersService,
        public bundles: BundlesService,
        private ns: NotifyService,
        private spinner: SpinnerService
    ) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.getDefaultTemplate();

        this.structureSubscriber = routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                console.log(data.structure);
                this.importInfos.structureId = data.structure.id;
                this.importInfos.structureExternalId = data.structure.externalId;
                this.importInfos.structureName = data.structure.name;
                this.importInfos.UAI = data.structure.UAI;
                this.isSourceAutomatic = data.structure.source && StructureModel.AUTOMATIC_SOURCES_REGEX.test(data.structure.source);
                this.changeDetector.markForCheck();
            }
        });
    }

    async csvImportFile() {
        await this.getColumsMapping();
        await this.getAsmReceivers();
        this.changeDetector.detectChanges();
    }
    /*
    * Fire when (change) on input[file]. Update profile's filelist to import
    * TODO : wrap into a component
    */
    loadFile(event) {   
        this.importInfos[event.target.name] = event.target.files[0];
        this.isButtonVisible = true;
    }

    async getAsmReceivers(): Promise<void> {
        try {
            const data = await MassMessageService.populateImportedInfos(this.columns.mappings, this.columns.requiredFields);
    
            if (data.error) {
                this.importedData = null;
                this.countUsers = 0;
                this.showImportedFileInTable = false;
                this.globalError.message = data.error;  
            } else if (data.errors) {
                this.importedData = null;
                this.countUsers = 0;
                this.showImportedFileInTable = false;
                this.globalError.message = 'import.error.malformedFiles';
                this.globalError.profile = data.errors.errors;
            } else {
                const headers: { field: string, token: string }[] = data.csvHeaders.map((h: string) => ({
                    field: h,
                    token: `[${h.toLowerCase()}]`
                }));
    
                const rows: DynamicMessageReceiver[] = this.columns.mappings.slice(1).map((row: string[]) => {
                    const cells = row[0].split(';').map(cell => cell.trim());
                    const messageReceiver: DynamicMessageReceiver = {};
                    cells.forEach((value, index) => {
                        const header = data.csvHeaders[index] || null;
                        if(header !== null) {
                            messageReceiver[header] = { field: `[${header.toLowerCase()}]`, value: value };
                        }
                    });
                    return messageReceiver;
                }).filter(row => Object.keys(row).length > 0);
    
                this.countUsers = rows.length;
                this.importedData = { headers, rows};
                this.showImportedFileInTable = true;
            }
        } catch (error) {
            this.globalError.message = 'An error occurred while processing the data. Please try again.';
            console.error('Error in getAsmReceivers:', error);
        } finally {
            this.changeDetector.detectChanges();
        }
    }

    sendAsmPublipostage(): void {
        this.importedData.template = this.htmlTemplate;
        this.importedData.messageSubject = this.inputValue;
        try {
            MassMessageService.sendEmail(this.importedData);
            this.messagesSent = true;
        } catch (error) {
            this.sendFailed = true;
            return error.response.data;
        }
        
      }

    private async getColumsMapping(): Promise<void> {
        //map data from current excel to columns we need
        this.globalError.reset();

        const data = await this.spinner.perform('portal-content', MassMessageService.getColumnsMapping(this.importInfos));
        console.log(data);
        if (data.error) {
            this.globalError.message = data.error;
        } else if (data.errors) {
            this.globalError.message = 'import.error.malformedFiles';
            this.globalError.profile = data.errors.errors; // TODO Fix server API. serve only {errors:{...}}
        } else {
            this.columns.availableFields = data.availableFields;
            this.columns.mappings = data.asmRecords;
            if (this.columns.checkErrors(this.globalError, this.translate)) {
                //this.wizardEl.doNextStep(true);
                console.log("has error if");
            } else {
                console.log("no error else");
            }
        }
        this.changeDetector.markForCheck();
    }

    async getDefaultTemplate(): Promise<void> {
        this.globalError.reset();

        const data = await this.spinner.perform('portal-content', MassMessageService.getDefaultTemplate());
      
        if (data.error) {
            this.globalError.message = data.error;
        } else if (data.errors) {
            this.globalError.message = 'import.error.malformedFiles';
            this.globalError.profile = data.errors.errors;
        } else {
            this.htmlTemplate = data;
        }
    }

    async saveModifiedTemplate() {
        this.showTemplateEditor = false;
    }

    async cancelTemplateEdit() {
        console.log("cancel")
        this.htmlTemplate = this.tempHtmlTemplate;
        console.log(this.htmlTemplate)
        this.showTemplateEditor = false;
    }
    openTemplateEditot() {
        this.tempHtmlTemplate = this.htmlTemplate;
        this.showTemplateEditor = true;
    }
    

    private createDownloadAnchor(): void {
        this.downloadAnchor = document.createElement('a');
        this.downloadAnchor.style = 'display: none';
        document.body.appendChild(this.downloadAnchor);
    }

    onClick(event) {
        if (this.show
            && event.target != this.filtersToggleRef.nativeElement
            && !this.filtersToggleRef.nativeElement.contains(event.target)
            && !this.filtersDivRef.nativeElement.contains(event.target)
            && !this.deselectItem
            && !event.target.className.toString().startsWith('flatpickr')
            && !event.target.parentElement.className.toString().startsWith('flatpickr')) {
            this.toggleVisibility();
        }
        this.deselectItem = false;
        return true;
    }

    toggleVisibility(): void {
        this.show = !this.show;
    }

}

interface DynamicMessageReceiver {
    [key: string]: FieldAndValue; // Allows for dynamic keys
}
export interface FieldAndToken {
    field: string;
    token: string;
}
export interface FieldAndValue {
    field: string;
    value: string;
}
interface GlobalError { message: string; profile: {}; reset: Function; }