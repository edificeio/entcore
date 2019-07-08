import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { ConnectorModel } from '../../../../core/store';
import { CasType } from '../CasType';
import { NgForm } from '@angular/forms';
import { SelectOption } from '../../../../shared/ux/components/multi-select.component';

@Component({
    selector: 'connector-properties',
    template: `
        <form #propertiesForm="ngForm">
            <panel-section section-title="services.connector.icon.title">
                <upload-files [fileSrc]="connector.icon"
                              [allowedExtensions]="['jpeg', 'jpg', 'bmp', 'png']"
                              [maxFilesNumber]="1"
                              [disabled]="disabled"
                              (upload)="onUpload($event)"
                              (invalidUpload)="onInvalidUpload($event)">
                </upload-files>

                <fieldset>
                    <form-field label="services.connector.icon.url"
                                help="services.connector.icon.url.help">
                        <input type="text"
                               [(ngModel)]="connector.icon"
                               name="icon"
                               (change)="connector.iconFile = null"
                               [disabled]="disabled">
                    </form-field>
                </fieldset>
            </panel-section>

            <panel-section section-title="services.connector.properties.title">
                <fieldset [disabled]="disabled">
                    <div *ngIf="structureChildren"
                         class="has-vertical-padding-5">
                        <input type="checkbox"
                               [(ngModel)]="connector.inherits"
                               name="inherits"
                               class="has-no-margin">
                        <s5l>services.connector.properties.inherits</s5l>
                    </div>

                    <form-field label="services.connector.properties.id">
                        <input type="text"
                               [(ngModel)]="connector.name"
                               name="name"
                               required
                               #nameInput="ngModel">
                        <form-errors [control]="nameInput"></form-errors>
                    </form-field>

                    <form-field label="services.connector.properties.displayName">
                        <input type="text"
                               [(ngModel)]="connector.displayName"
                               name="displayName"
                               required
                               #displayNameInput="ngModel">
                        <form-errors [control]="displayNameInput"></form-errors>
                    </form-field>

                    <form-field label="services.connector.properties.url">
                        <input type="text"
                               [(ngModel)]="connector.url"
                               name="url"
                               required
                               #urlInput="ngModel">
                        <form-errors [control]="urlInput"></form-errors>
                    </form-field>

                    <form-field label="services.connector.properties.target">
                        <mono-select [(ngModel)]="connector.target" 
                                    name="target" 
                                    class="is-flex-none"
                                    [options]="targetOptions"
                                    [disabled]="disabled">
                            <option [value]="LINKPARAMS_TARGET_PORTAL">
                                {{ 'services.connector.properties.target.portal' | translate }}
                            </option>
                            <option [value]="LINKPARAMS_TARGET_NEWPAGE">
                                {{ 'services.connector.properties.target.newPage' | translate }}
                            </option>
                            <option [value]="LINKPARAMS_TARGET_ADAPTOR">
                                {{ 'services.connector.properties.target.adaptor' | translate }}
                            </option>
                        </mono-select>
                    </form-field>
                </fieldset>
            </panel-section>

            <hr>
            <div class="connector-properties-warning">
                <div class="connector-properties-warning__header">
                    <s5l>services.connector.properties.warning.header</s5l>
                </div>
                <div class="connector-properties-warning__content">
                    <s5l>services.connector.properties.warning.content</s5l>
                </div>
            </div>

            <panel-section section-title="services.connector.cas.title" [folded]="true">
                <fieldset [disabled]="disabled">
                    <div>
                        <input type="checkbox"
                               [(ngModel)]="connector.hasCas"
                               name="hasCas"
                               class="has-no-margin"
                               (change)="toggleCasType()">
                        <s5l>services.connector.cas.hasCas</s5l>
                    </div>

                    <div *ngIf="connector.hasCas && connector.casTypeId"
                         class="connector-properties-cas-casType__description"
                         [innerHtml]="getCasTypeDescription()">
                    </div>

                    <form-field label="services.connector.cas.type">
                        <mono-select [(ngModel)]="connector.casTypeId"
                                     name="casTypeId"
                                     [disabled]="!connector.hasCas || disabled"
                                     class="is-flex-none has-min-width" [options]="casTypesOptions">
                            <option *ngFor="let casType of casTypes" [value]="casType.id">
                                {{ casType.name }}
                            </option>
                        </mono-select>
                    </form-field>

                    <form-field label="services.connector.cas.pattern">
                        <input type="text"
                               [(ngModel)]="connector.casPattern"
                               name="casPattern"
                               [placeholder]="'form.optional' | translate">
                    </form-field>
                </fieldset>
            </panel-section>

            <panel-section section-title="services.connector.oauth.title" [folded]="true">
                <fieldset [disabled]="disabled">
                    <div>
                        <input type="checkbox"
                               [(ngModel)]="connector.oauthTransferSession"
                               name="transferSession"
                               (change)="setUserinfoInOAuthScope()"
                               class="has-no-margin">
                        <s5l>services.connector.oauth.transferSession</s5l>
                    </div>

                    <form-field label="services.connector.oauth.clientId">
                        <span>{{ connector.name }}</span>
                    </form-field>

                    <form-field label="services.connector.oauth.scope">
                        <input type="text" [(ngModel)]="connector.oauthScope" name="scope">
                    </form-field>

                    <form-field label="services.connector.oauth.secret">
                        <input type="text" [(ngModel)]="connector.oauthSecret" name="secret">
                    </form-field>

                    <form-field label="services.connector.oauth.grantType">
                        <mono-select [(ngModel)]="connector.oauthGrantType"
                                     name="grantType"
                                     class="is-flex-none" 
                                     [options]="grantTypeOptions"
                                     [disabled]="disabled">
                        </mono-select>
                    </form-field>
                </fieldset>
            </panel-section>

            <div *ngIf="creationMode"
                 class="connector-properties__action">
                <button type="button"
                        class="connector-properties__action--cancel"
                        (click)="create.emit('cancel')">
                    <s5l>services.connector.create.button.cancel</s5l>
                </button>
                <button type="button"
                        class="connector-properties__action--submit confirm"
                        (click)="create.emit('submit')"
                        [disabled]="propertiesForm.pristine || propertiesForm.invalid">
                    <s5l>services.connector.create.button.submit</s5l>
                </button>
            </div>
        </form>
    `,
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
export class ConnectorPropertiesComponent implements OnChanges {
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
            return this.casTypes.find(casType => casType.id === this.connector.casTypeId).description
        }
        return '';
    }

    public setUserinfoInOAuthScope() {
        if (this.connector.oauthTransferSession
            && (!this.connector.oauthScope || this.connector.oauthScope.indexOf('userinfo') === -1)) {
            this.connector.oauthScope = 'userinfo' + (this.connector.oauthScope || '')
        }
        if (!this.connector.oauthTransferSession
            && this.connector.oauthScope && this.connector.oauthScope.indexOf('userinfo') !== -1) {
            this.connector.oauthScope = this.connector.oauthScope.replace('userinfo', '')
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        this.casTypesOptions = this.casTypes ? this.casTypes.map(c => ({value: c.id, label: c.name})) : [];
    }

    public onUpload($event: File[]): void {
        this.iconFileChanged.emit($event);
    }

    public onInvalidUpload($event: string): void {
        this.iconFileInvalid.emit($event);
    }
}