import { Component, EventEmitter, Injector, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { OdeComponent } from 'ngx-ode-core';
import { SelectOption } from 'ngx-ode-ui';
import { ConnectorModel } from '../../../../core/store/models/connector.model';
import { CasType } from '../CasType';

@Component({
    selector: 'ode-connector-properties',
    templateUrl: './connector.properties.component.html',
    styles: [`
        .connector-properties__action {
            display: flex;
            justify-content: flex-end;
            padding: 20px 10px 10px 10px;
        }
    `, `
        .connector-properties__action--cancel {
            min-width: 80px;
            text-align: center;
        }
    `, `
        .connector-properties__action--submit {
            margin-left: 5px;
            min-width: 80px;
            text-align: center;
        }
    `, `
        .connector-properties-warning {
            padding: 0px 15px;
        }
    `, `
        .connector-properties-warning__header {
            color: #ff8352;
            font-weight: bold;
        }
    `, `
        .connector-properties-warning__content {
            font-style: italic;
            font-size: 0.8em;
        }
    `, `
        .connector-properties-cas-casType__description {
            padding-top: 20px;
            font-weight: bold;
        }
    `]
})
export class ConnectorPropertiesComponent extends OdeComponent implements OnChanges {
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

    @ViewChild('propertiesForm', { static: false })
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
    OAUTH_GRANTTYPE_CLIENT_CREDENTIALS = 'client_credentials';
    OAUTH_GRANTTYPE_PASSWORD = 'password';
    OAUTH_GRANTTYPE_BASIC = 'Basic';

    grantTypeOptions: SelectOption<string>[] = [
        {value: this.OAUTH_GRANTTYPE_AUTHORIZATION_CODE, label: 'services.connector.oauth.grantType.authorizationCode'},
        {value: this.OAUTH_GRANTTYPE_CLIENT_CREDENTIALS, label: 'services.connector.oauth.grantType.clientCredentials'},
        {value: this.OAUTH_GRANTTYPE_PASSWORD, label: 'services.connector.oauth.grantType.password'},
        {value: this.OAUTH_GRANTTYPE_BASIC, label: 'services.connector.oauth.grantType.basic'}
    ];

    casTypesOptions: SelectOption<string>[] = this.casTypes ? this.casTypes.map(c => ({
        value: c.id,
        label: c.name
    })) : [];

    public toggleCasType(): void {
        if (this.connector.hasCas) {
            this.connector.casTypeId = this.CAS_DEFAULT_CAS_TYPE_ID;
        } else {
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
    }

    public onUpload($event: File[]): void {
        this.iconFileChanged.emit($event);
    }

    public onInvalidUpload($event: string): void {
        this.iconFileInvalid.emit($event);
    }
    constructor(injector: Injector) {
        super(injector);
    }
}
