import { Component, OnInit, OnDestroy } from "@angular/core";
import { Location } from "@angular/common";
import { Router, ActivatedRoute } from "@angular/router";
import { ConnectorModel, StructureModel, SessionModel, Session } from "../../../core/store";
import { ServicesStore } from "../../services.store";
import { ServicesService } from "../../services.service";
import { NotifyService, SpinnerService } from "../../../core/services";
import { CasType } from "../CasType";

import { Subscription } from "rxjs";
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/toPromise';

@Component({
    selector: 'connector-create',
    template: `
    <div class="panel-header">
        <span><s5l>services.connector.create.title</s5l></span>
    </div>

    <form #createForm="ngForm" (ngSubmit)="create()">
        <panel-section section-title="services.connector.create.section.linkParams.title">
            <div *ngIf="hasChildren(servicesStore.structure)"
                class="has-vertical-padding-5">
                <input type="checkbox" 
                    [(ngModel)]="newConnector.inherits" 
                    name="inherits" 
                    class="has-no-margin">
                <s5l>services.connector.create.section.linkParams.inherits</s5l>
            </div>

            <div *ngIf="newConnector.inherits == true && isAdmc" class="has-vertical-padding-5">
                <input type="checkbox" 
                    [(ngModel)]="newConnector.locked" 
                    name="locked" 
                    class="has-no-margin">
                <s5l>services.connector.create.section.linkParams.locked</s5l>
            </div>
                    
            <form-field label="services.connector.create.section.linkParams.id">
                <input type="text" 
                    [(ngModel)]="newConnector.name" 
                    name="name" 
                    required 
                    #nameInput="ngModel">
                <form-errors [control]="nameInput"></form-errors>
            </form-field>

            <form-field label="services.connector.create.section.linkParams.displayName">
                <input type="text" 
                    [(ngModel)]="newConnector.displayName" 
                    name="displayName" 
                    required 
                    #displayNameInput="ngModel">
                <form-errors [control]="displayNameInput"></form-errors>
            </form-field>

            <form-field label="services.connector.create.section.linkParams.icon">
                <input type="text" [(ngModel)]="newConnector.icon" name="icon">
            </form-field>

            <form-field label="services.connector.create.section.linkParams.url">
                <input type="text" 
                    [(ngModel)]="newConnector.url" 
                    name="url" 
                    required 
                    #urlInput="ngModel">
                <form-errors [control]="urlInput"></form-errors>
            </form-field>

            <form-field label="services.connector.create.section.linkParams.target">
                <select [(ngModel)]="newConnector.target" name="target" class="is-flex-none">
                    <option [value]="LINKPARAMS_TARGET_PORTAL">
                        {{ 'services.connector.create.section.linkParams.target.portal' | translate }}
                    </option>
                    <option [value]="LINKPARAMS_TARGET_NEWPAGE">
                        {{ 'services.connector.create.section.linkParams.target.newPage' | translate }}
                    </option>
                    <option [value]="LINKPARAMS_TARGET_ADAPTOR">
                        {{ 'services.connector.create.section.linkParams.target.adaptor' | translate }}
                    </option>
                </select>
            </form-field>
        </panel-section>

        <hr>

        <div class="connector-create-warning">
            <div class="connector-create-warning__header">
                <s5l>services.connector.create.section.warning.header</s5l>
            </div>
            <div class="connector-create-warning__content">
                <s5l>services.connector.create.section.warning.content</s5l>
            </div>
        </div>

        <panel-section section-title="services.connector.create.section.cas.title">
            <div>
                <input type="checkbox" 
                    [(ngModel)]="newConnector.hasCas" 
                    name="hasCas" 
                    class="has-no-margin"
                    (change)="toggleCasType()">
                <s5l>services.connector.create.section.cas.hasCas</s5l>
            </div>
            
            <div *ngIf="newConnector.hasCas && newConnector.casTypeId" 
                class="connector-create-cas-casType__description"
                [innerHtml]="getCasTypeDescription()">
            </div>

            <form-field label="services.connector.create.section.cas.type">
                <select [(ngModel)]="newConnector.casTypeId" 
                    name="casTypeId" 
                    [disabled]="!newConnector.hasCas" 
                    class="is-flex-none">
                    <option *ngFor="let casType of casTypes" [value]="casType.id">{{ casType.name }}</option>
                </select>
            </form-field>

            <form-field label="services.connector.create.section.cas.pattern"
                *ngIf="isAdmc">
                <input type="text" [(ngModel)]="newConnector.casPattern" name="casPattern" [placeholder]="'form.optional' | translate">
            </form-field>
        </panel-section>

        <panel-section section-title="services.connector.create.section.oauth.title">
            <div>
                <input type="checkbox" 
                    [(ngModel)]="newConnector.oauthTransferSession" 
                    name="transferSession" 
                    (change)="setUserinfoInOAuthScope()"
                    class="has-no-margin">
                <s5l>services.connector.create.section.oauth.transferSession</s5l>
            </div>
            
            <form-field label="services.connector.create.section.oauth.clientId">
                <span>{{ newConnector.name }}</span>
            </form-field>

            <form-field label="services.connector.create.section.oauth.scope">
                <input type="text" [(ngModel)]="newConnector.oauthScope" name="scope">
            </form-field>

            <form-field label="services.connector.create.section.oauth.secret">
                <input type="text" [(ngModel)]="newConnector.oauthSecret" name="secret">
            </form-field>

            <form-field label="services.connector.create.section.oauth.grantType">
                <select [(ngModel)]="newConnector.oauthGrantType" 
                    name="grantType" 
                    class="is-flex-none">
                    <option [value]="OAUTH_GRANTTYPE_AUTHORIZATION_CODE">
                        {{ 'services.connector.create.section.oauth.grantType.authorizationCode' | translate }}
                    </option>
                    <option [value]="OAUTH_GRANTTYPE_CLIENT_CREDENTIALS">
                        {{ 'services.connector.create.section.oauth.grantType.clientCredentials' | translate }}
                    </option>
                    <option [value]="OAUTH_GRANTTYPE_PASSWORD">
                        {{ 'services.connector.create.section.oauth.grantType.password' | translate }}
                    </option>
                    <option [value]="OAUTH_GRANTTYPE_BASIC">
                        {{ 'services.connector.create.section.oauth.grantType.basic' | translate }}
                    </option>
                </select>
            </form-field>
        </panel-section>

        <div class="connector-create__action">
            <button type="button" (click)="cancel()">
                <s5l>services.connector.create.button.cancel</s5l>
            </button>
            <button type="submit" 
                    [disabled]="createForm.pristine || createForm.invalid" 
                    class="connector-create__action--submit">
                <s5l>services.connector.create.button.submit</s5l>
            </button>
        </div>
    </form>
    `,
    styles: [`
        .connector-create__action {
            display: flex;
            justify-content: flex-end;
            padding: 0px 10px 10px 10px;
        }
    `, `
        .connector-create__action--submit {
            margin-left: 5px;
            margin-right: 10px;
        }
    `, `
        .connector-create-warning {
            padding: 0px 15px;
        }
    `, `
        .connector-create-warning__header {
            color: #ff8352;
            font-weight: bold;
        }
    `, `
        .connector-create-warning__content {
            font-style: italic;
            font-size: 0.8em;
        }
    `, `
        .connector-create-cas-casType__description {
            padding-top: 20px;
            font-weight: bold;
        }
    `]
})
export class ConnectorCreate implements OnInit, OnDestroy {

    newConnector: ConnectorModel;
    casTypes: CasType[];
    isAdmc: boolean;
    casTypesSubscription: Subscription;

    LINKPARAMS_TARGET_PORTAL = '';
    LINKPARAMS_TARGET_NEWPAGE = '_blank';
    LINKPARAMS_TARGET_ADAPTOR = 'adapter';
    OAUTH_GRANTTYPE_AUTHORIZATION_CODE = 'authorization_code';
    OAUTH_GRANTTYPE_CLIENT_CREDENTIALS = 'client_credentials';
    OAUTH_GRANTTYPE_PASSWORD = 'password';
    OAUTH_GRANTTYPE_BASIC = 'Basic';
    CAS_DEFAULT_CAS_TYPE_ID = 'UidRegisteredService';

    constructor(private location: Location,
        public servicesStore: ServicesStore,
        private notifyService: NotifyService,
        private spinnerService: SpinnerService,
        private router: Router,
        private activatedRoute: ActivatedRoute,
        private servicesService: ServicesService) {
    }

    public async ngOnInit() {
        this.newConnector = new ConnectorModel();
        this.newConnector.target = this.LINKPARAMS_TARGET_PORTAL;

        this.casTypesSubscription = this.servicesService.getCasTypes().subscribe(res => this.casTypes = res);
        
        const session: Session = await SessionModel.getSession();
        this.isAdmc = session.functions && session.functions['SUPER_ADMIN'] != null;
    }

    public ngOnDestroy(): void {
        this.casTypesSubscription.unsubscribe();
    }

    public create(): void {
        this.spinnerService.perform('portal-content'
            , this.servicesService.createConnector(this.newConnector, this.servicesStore.structure.id)
                .do(res => {
                    this.newConnector.id = res.id;
                    this.servicesStore.structure.connectors.data.push(this.newConnector);
                    this.notifyService.success({
                        key: 'services.connector.create.success.content',
                        parameters: {connector: this.newConnector.displayName}
                    }, 'services.connector.create.success.title');
    
                    this.router.navigate(['..', res.id]
                        , {relativeTo: this.activatedRoute, replaceUrl: false});
                })
                .catch(error => {
                    if (error.error && error.error.error) {
                        this.notifyService.error(error.error.error
                            , 'services.connector.create.error.title'
                            , error);
                    } else {
                        this.notifyService.error({
                            key: 'services.connector.create.error.content'
                            , parameters: {connector: this.newConnector.displayName}
                        }, 'services.connector.create.error.title'
                        , error);
                    }
                    throw error;
                })
                .toPromise()
        );
    }

    public cancel() {
        this.location.back();
    }

    public hasChildren(structure: StructureModel): boolean {
        return structure.children && structure.children.length > 0;
    }

    public toggleCasType(): void {
        if (this.newConnector.hasCas) {
            this.newConnector.casTypeId = this.CAS_DEFAULT_CAS_TYPE_ID;            
        } else {
            delete this.newConnector.casTypeId;
            delete this.newConnector.casPattern;
        }
    }

    public setUserinfoInOAuthScope() {
        if (this.newConnector.oauthTransferSession 
            && (!this.newConnector.oauthScope || this.newConnector.oauthScope.indexOf('userinfo') === -1)){
			this.newConnector.oauthScope = 'userinfo' + (this.newConnector.oauthScope || '')
		}
        if (!this.newConnector.oauthTransferSession
            && this.newConnector.oauthScope && this.newConnector.oauthScope.indexOf('userinfo') !== -1){
                this.newConnector.oauthScope = this.newConnector.oauthScope.replace('userinfo', '')
		}
	}

    public getCasTypeDescription(): string {
        if (this.newConnector.casTypeId) {
            return this.casTypes.find(casType => casType.id === this.newConnector.casTypeId).description
        }
        return '';
    }
}