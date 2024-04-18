import { Component, EventEmitter, Injector, Input, OnChanges, Output, SimpleChanges, ViewChild, OnInit } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from '../../../../core/services/routing.service';
import { SelectOption } from 'ngx-ode-ui';
import { ConnectorModel, MappingModel } from '../../../../core/store/models/connector.model';
import { CasType } from '../CasType';
import { MappingCollection } from 'src/app/core/store/collections/connector.collection';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Structure } from 'src/app/services/_shared/services-types';

@Component({
    selector: 'ode-connector-properties',
    templateUrl: './connector.properties.component.html',
    styleUrls: ['./connector.properties.component.scss'],
})
export class ConnectorPropertiesComponent extends OdeComponent implements OnInit,OnChanges {
    @Input()
    connector: ConnectorModel;
    @Input()
    casTypes: CasType[];
    @Input()
    structureChildren: boolean;
    @Input()
    creationMode: boolean;
    @Input()
    disabled: boolean;
    @Input()
    admc: boolean;

    @Output()
    create: EventEmitter<string> = new EventEmitter<string>();
    @Output()
    iconFileChanged: EventEmitter<File[]> = new EventEmitter();
    @Output()
    iconFileInvalid: EventEmitter<string> = new EventEmitter();

    @ViewChild('propertiesForm')
    propertiesFormRef: NgForm;

    LINKPARAMS_TARGET_PORTAL = '';
    LINKPARAMS_TARGET_NEWPAGE = '_blank';
    LINKPARAMS_TARGET_ADAPTOR = 'adapter';
    CAS_DEFAULT_CAS_TYPE_ID = 'UidRegisteredService';


    targetOptions: SelectOption<string>[] = [
        {value: this.LINKPARAMS_TARGET_PORTAL, label: 'services.connector.properties.target.portal'},
        {value: this.LINKPARAMS_TARGET_NEWPAGE, label: 'services.connector.properties.target.newPage'},
        {value: this.LINKPARAMS_TARGET_ADAPTOR, label: 'services.connector.properties.target.adaptor'}
    ];

    OAUTH_GRANTTYPE_AUTHORIZATION_CODE = 'authorization_code';
    OAUTH_GRANTTYPE_JWT_BEARER = 'urn:ietf:params:oauth:grant-type:jwt-bearer';
    OAUTH_GRANTTYPE_CLIENT_CREDENTIALS = 'client_credentials';
    OAUTH_GRANTTYPE_PASSWORD = 'password';
    OAUTH_GRANTTYPE_BASIC = 'Basic';

    grantTypeOptions: SelectOption<string>[] = [
        {value: this.OAUTH_GRANTTYPE_AUTHORIZATION_CODE, label: 'services.connector.oauth.grantType.authorizationCode'},
        { value: this.OAUTH_GRANTTYPE_JWT_BEARER, label: 'services.connector.oauth.grantType.jwtBearer' },
        {value: this.OAUTH_GRANTTYPE_CLIENT_CREDENTIALS, label: 'services.connector.oauth.grantType.clientCredentials'},
        {value: this.OAUTH_GRANTTYPE_PASSWORD, label: 'services.connector.oauth.grantType.password'},
        {value: this.OAUTH_GRANTTYPE_BASIC, label: 'services.connector.oauth.grantType.basic'}
    ];

    casTypesOptions: SelectOption<string>[];
    casMappings: MappingModel[] = [];
    casMappingCollection: MappingCollection;
    isOpenCasType = false;
    isOpenCasRemove = false;
    casTypeToRemove: MappingModel = null;
    casTypeToRemoveId: string = null;
    newCasType = new MappingModel;
    

    structure:Structure = null;
    
    async ngOnInit(){
        super.ngOnInit();
        this.casMappingCollection = await MappingCollection.getInstance();
        this.casMappings = this.casMappingCollection.data;
        // Watch selected structure
        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.structure = data.structure;
                this.changeDetector.markForCheck();
            }
        }));
    }

    private setMapping(mapping: MappingModel){
        if(mapping){
            this.connector.casPattern = mapping.pattern;
            this.connector.casTypeId = mapping.casType;
        } else{
            this.connector.casTypeId = undefined;
            this.connector.casPattern = undefined;
        }
    }

    public onCasMappingTypeChange(value:string){
        const removeAccents = (strAccents) => {
            strAccents = strAccents.split('');
            let strAccentsOut :any = new Array();
            let strAccentsLen = strAccents.length;
            var accents = 'ÀÁÂÃÄÅàáâãäåÒÓÔÕÕÖØòóôõöøÈÉÊËèéêëðÇçÐÌÍÎÏìíîïÙÚÛÜùúûüÑñŠšŸÿýŽž';
            var accentsOut = ['A','A','A','A','A','A','a','a','a','a','a','a','O','O','O','O','O','O','O','o','o','o','o','o','o','E','E','E','E','e','e','e','e','e','C','c','D','I','I','I','I','i','i','i','i','U','U','U','U','u','u','u','u','N','n','S','s','Y','y','y','Z','z'];
            for (var y = 0; y < strAccentsLen; y++) {
                if (accents.indexOf(strAccents[y]) != -1) {
                    strAccentsOut[y] = accentsOut[accents.indexOf(strAccents[y])];
                }
                else
                    strAccentsOut[y] = strAccents[y];
            }
            strAccentsOut = strAccentsOut.join('');
            return strAccentsOut;
        }
        const slug = (s: string) => {
            s = removeAccents(s);
            const res = s.replace(/[^\w\s]/gi, " ").replace(/\s\s+/g, " ").trim().replace(/\s/g, "-");
            return (res);
        }
        this.newCasType.type = slug(value);
    }

    public onCasMappingChange(statCasType:string){
        this.connector.statCasType = statCasType;
        const mapping = this.casMappings.find(e=>e.type==statCasType);
        this.setMapping(mapping);
    }

    public toggleCasType(): void {
        if (!this.connector.hasCas) {
            this.connector.statCasType = null;
            this.connector.casTypeId = null;
            this.connector.casPattern = null;
        }
    }

    public getCasTypeDescription(): string {
        if (this.connector.casTypeId && this.casTypes) {
            return this.casTypes.find(casType => casType.id === this.connector.casTypeId).description;
        }
        return '';
    }

    public openCasType(){
        this.newCasType = new MappingModel;
        this.isOpenCasType = true;
    }

    public async closeCasType(confirm: boolean){
        try {
            if(confirm){
                await this.casMappingCollection.createMapping(this.newCasType);
                this.casMappings = this.casMappingCollection.data;
                this.changeDetector.markForCheck();
            }
            this.isOpenCasType = false;
            this.newCasType =  new MappingModel;
        } catch(e) {
            console.error(e)
            this.notifyService.error(e.response.data.error)
        }
    }

    public removeCasType()
    {
        this.isOpenCasRemove = true;
    }

    public async onCasTypeToRemoveChange(statCasType:string)
    {
        let stats = await this.casMappingCollection.getUsage(statCasType, this.structure.id);
        this.casTypeToRemove = this.casMappings.find(e=>e.type==statCasType);
        this.casTypeToRemove.connectorsInStruct = stats.data["connectorsInThisStruct"] == null ? [] : stats.data["connectorsInThisStruct"];
        this.casTypeToRemove.connectorsOutsideStruct = stats.data["usesInOtherStructs"];
        this.changeDetector.markForCheck();
    }

    public isRemoveDisabled()
    {
        return this.casTypeToRemove == null
        || (this.casTypeToRemove.connectorsInStruct == null || this.casTypeToRemove.connectorsInStruct.length != 0)
        || (this.casTypeToRemove.connectorsOutsideStruct == null || this.casTypeToRemove.connectorsOutsideStruct != 0);
    }

    public async closeCasRemove(confirm: boolean)
    {
        try {
            if(confirm){
                await this.casMappingCollection.removeMapping(this.casTypeToRemove);
                this.casMappings = this.casMappingCollection.data;
                this.changeDetector.markForCheck();
            }
            this.isOpenCasRemove = false;
            this.casTypeToRemove = null;
        } catch(e) {
            console.error(e)
            this.notifyService.error(e.response.data.error)
        }
    }

    public setUserinfoInOAuthScope() {
        if (this.connector.oauthTransferSession
            && (!this.connector.oauthScope || this.connector.oauthScope.indexOf('userinfo') === -1)) {
            this.connector.oauthScope = 'userinfo' + (this.connector.oauthScope || '');
        }
        if (!this.connector.oauthTransferSession
            && this.connector.oauthScope && this.connector.oauthScope.indexOf('userinfo') !== -1) {
            this.connector.oauthScope = this.connector.oauthScope.replace('userinfo', '');
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);
        this.casTypesOptions = this.casTypes ? this.casTypes.map(c => ({value: c.id, label: c.name})) : [];
        if(this.casMappingCollection && this.connector && !this.connector.statCasType){//compute it only if not exists
            this.connector.statCasType = this.casMappingCollection.getStatCasType(this.connector.casTypeId, this.connector.casPattern);
        }
    }

    public onUpload($event: File[]): void {
        this.iconFileChanged.emit($event);
    }

    public onInvalidUpload($event: string): void {
        this.iconFileInvalid.emit($event);
    }
    constructor(injector: Injector, private notifyService: NotifyService) {
        super(injector);
        this.casTypesOptions = this.casTypes ? this.casTypes.map(c => ({
            value: c.id,
            label: c.name
        })) : [];
    }
}
