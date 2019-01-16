import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { AbstractControl, NgForm } from '@angular/forms';
import { Subscription } from 'rxjs/Subscription';
import { Observable } from 'rxjs/Observable';
import { BundlesService } from 'sijil';

import { AbstractSection } from '../abstract.section';
import { NotifyService, PlatformInfoService, SpinnerService } from '../../../../core/services';
import { UserInfoService } from './user-info.service';
import { StructureModel, UserModel } from '../../../../core/store/models';
import { Config } from '../../Config';

@Component({
    selector: 'user-info-section',
    template: `
        <panel-section section-title="users.details.section.infos">
            <form #infoForm="ngForm">
                <fieldset>
                    <form-field label="profile">
                        <span>{{ user.type | translate }}</span>
                    </form-field>
                    <form-field label="login">
                        <span>{{ details.login }}</span>
                    </form-field>

                    <form-field label="loginAlias">
                        <div>
                            <input type="text"
                                   [(ngModel)]="details.loginAlias"
                                   name="loginAlias"
                                   [pattern]="loginAliasPattern"
                                   #loginAliasInput="ngModel">
                            <button (click)="updateLoginAlias()"
                                    [disabled]="infoForm.pristine || infoForm.invalid || spinner.isLoading('portal-content')">
                                <s5l>login.update</s5l>
                                <i class="fa fa-floppy-o"></i>
                            </button>
                            <form-errors [control]="loginAliasInput"
                                         [expectedPatternMsg]="'form.user.alias.pattern' | translate">
                            </form-errors>
                        </div>
                    </form-field>

                    <form-field label="activation.code" *ngIf="details.activationCode">
                        <span>{{ details.activationCode }}</span>
                    </form-field>
                    <form-field label="id">
                        <span>{{ user.id }}</span>
                    </form-field>
                    <form-field label="externalId">
                        <span>{{ details.externalId }}</span>
                    </form-field>
                    <form-field label="source">
                        <span>{{ details.source | translate }}</span>
                    </form-field>
                    <form-field *ngIf="details.created" label="creation">
                        <span>{{ displayDate(details.created) }}</span>
                    </form-field>
                    <form-field *ngIf="details.modified" label="modification.date">
                        <span>{{ displayDate(details.modified) }}</span>
                    </form-field>
                    <form-field label="mergeKey" *ngIf="user.type === 'Relative'">
                        <div>
                            <span *ngIf="details.mergeKey">{{ details.mergeKey }}</span>
                            <button class="noflex"
                                    *ngIf="!details.mergeKey"
                                    (click)="generateMergeKey()"
                                    [disabled]="user.deleteDate != null">
                                <s5l>generate</s5l>
                                <i class="fa fa-cog"></i>
                            </button>
                        </div>
                    </form-field>
                    <form-field label="functions" *ngIf="!user.deleteDate">
                        <div>
                            <div *ngIf="!details.isAdmc()">
                                <button *ngIf="!details.isAdml(this.structure.id)"
                                        (click)="addAdml()">
                                    <s5l>adml.add</s5l>
                                    <i class="fa fa-cog"></i>
                                </button>
                                <button *ngIf="details.isAdml(this.structure.id)"
                                        (click)="showConfirmation = true">
                                    <s5l>adml.remove</s5l>
                                    <i class="fa fa-cog"></i>
                                </button>
                            </div>
                            <div *ngFor="let func of details.functions">
                                <div>
                                    {{ func[0] | translate }}
                                    <span *ngIf="func[1] && func[1].length > 0 && getStructure(func[1][0])">
                                    ({{ getStructures(func[1]) }})
                                </span>
                                    <span *ngIf="func[1] && func[1].length > 0 && !getStructure(func[1][0])">
                                    ({{ 'member.of.n.structures' | translate:{count: func[1].length} }})
                                </span>
                                </div>
                                <div *ngIf="func[0] == 'ADMIN_LOCAL'">
                                    <lightbox-confirm
                                            [show]="showConfirmation"
                                            [title]="'warning'"
                                            (onConfirm)="removeAdml()"
                                            (onCancel)="showConfirmation = false">
                                        <p>{{ 'user.remove.adml.disclaimer.info' | translate:{username: user.displayName} }}</p>
                                        <p>{{ 'user.remove.adml.disclaimer.confirm' | translate }}</p>
                                    </lightbox-confirm>
                                </div>
                            </div>
                        </div>
                    </form-field>
                    <form-field label="send.reset.password" *ngIf="!details.activationCode">
                        <div>
                            <div class="sendPassword">
                                <input type="email" [(ngModel)]="passwordResetMail" name="passwordResetMail"
                                       [attr.placeholder]="'send.reset.password.email.placeholder' | translate"
                                       #passwordMailInput="ngModel" [pattern]="emailPattern">
                                <button (click)="sendResetPasswordMail(passwordResetMail)"
                                        [disabled]="!passwordResetMail || passwordMailInput.errors">
                                    <span><s5l>send.reset.password.button</s5l></span>
                                    <i class="fa fa-envelope"></i>
                                </button>
                            </div>

                            <div class="sendPassword" *ngIf="smsModule">
                                <input type="tel" [(ngModel)]="passwordResetMobile" name="passwordResetMobile"
                                       [attr.placeholder]="'send.reset.password.mobile.placeholder' | translate"
                                       #passwordMobileInput="ngModel">
                                <button class="mobile"
                                        (click)="sendResetPasswordMobile(passwordResetMobile)"
                                        [disabled]="!passwordResetMobile || passwordMobileInput.errors">
                                    <span><s5l>send.reset.password.button</s5l></span>
                                    <i class="fa fa-mobile"></i>
                                </button>
                            </div>
                        </div>
                    </form-field>

                    <form-field label="password.renewal.code" *ngIf="!details.activationCode">
                        <div>
                            <button (click)="clickOnGenerateRenewalCode()">
                                <span><s5l>generate.password.renewal.code</s5l></span>
                            </button>
                            <span *ngIf="renewalCode">
                                <s5l *ngIf="config['reset-code-delay'] && config['reset-code-delay'] > 0" [s5l-params]="{numberOfDays: millisecondToDays(config['reset-code-delay'])}">generated.password.renewal.code.with.lifespan</s5l>
                                <s5l *ngIf="config['reset-code-delay'] == 0">generated.password.renewal.code</s5l>
                                 : {{renewalCode}}</span>
                        </div>
                    </form-field>

                    <form-field label="massmail">
                        <div>
                            <button (click)="sendIndividualMassMail('pdf')">
                                <span><s5l>individual.massmail.pdf</s5l></span>
                                <i class="fa fa-file-pdf-o"></i>
                            </button>
                            <button (click)="sendIndividualMassMail('mail')" [disabled]="!details.email">
                                <span><s5l>individual.massmail.mail</s5l></span>
                                <i class="fa fa-envelope"></i>
                            </button>
                        </div>
                    </form-field>
                </fieldset>
            </form>
        </panel-section>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserInfoSection extends AbstractSection implements OnInit {
    passwordResetMail: string;
    passwordResetMobile: string;
    smsModule: boolean;
    showConfirmation = false;
    downloadAnchor = null;
    downloadObjectUrl = null;
    renewalCode: string | undefined = undefined;

    userInfoSubscriber: Subscription;

    loginAliasPattern = /^[0-9a-z\-\.]+$/;

    @Input() structure: StructureModel;
    @Input() user: UserModel;
    @Input() config: Config;

    @ViewChild('infoForm') infoForm: NgForm;

    @ViewChild('loginAliasInput') loginAliasInput: AbstractControl;

    private SECONDS_IN_DAYS = 24 * 3600;
    private MILLISECONDS_IN_DAYS = this.SECONDS_IN_DAYS * 1000;

    millisecondToDays(millisecondTimestamp: number): number {
        return Math.ceil(millisecondTimestamp / this.MILLISECONDS_IN_DAYS);
    }

    constructor(
        private http: HttpClient,
        private bundles: BundlesService,
        private ns: NotifyService,
        public spinner: SpinnerService,
        private cdRef: ChangeDetectorRef,
        private userInfoService: UserInfoService) {
        super();
    }

    ngOnInit() {
        this.passwordResetMail = this.details.email;
        this.passwordResetMobile = this.details.mobile;
        PlatformInfoService.isSmsModule().then(res => {
            this.smsModule = res;
            this.cdRef.markForCheck();
        });

        this.userInfoSubscriber = this.userInfoService.getState()
            .subscribe(() => this.cdRef.markForCheck());
    }

    protected onUserChange() {
        if (!this.details.activationCode) {
            this.passwordResetMail = this.details.email;
            this.passwordResetMobile = this.details.mobile;
        }
    }

    addAdml() {
        this.spinner.perform('portal-content', this.details.addAdml(this.structure.id))
            .then(() => {
                this.ns.success({
                    key: 'notify.user.add.adml.content',
                    parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                }, 'notify.user.add.adml.title');
                this.cdRef.markForCheck();
            }).catch(err => {
            this.ns.error({
                key: 'notify.user.add.adml.error.content',
                parameters: {user: this.user.firstName + ' ' + this.user.lastName}
            }, 'notify.user.add.adml.error.title', err);
        });
    }

    removeAdml() {
        this.showConfirmation = false;
        this.spinner.perform('portal-content', this.details.removeAdml())
            .then(() => {
                this.ns.success({
                    key: 'notify.user.remove.adml.content',
                    parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                }, 'notify.user.remove.adml.title');
                this.cdRef.markForCheck();
            }).catch(err => {
            this.ns.error({
                key: 'notify.user.remove.adml.error.content',
                parameters: {user: this.user.firstName + ' ' + this.user.lastName}
            }, 'notify.user.remove.adml.error.title', err);
        });
    }

    sendResetPasswordMail(email: string) {
        this.spinner.perform('portal-content', this.details.sendResetPassword({type: 'email', value: email}))
            .then(() => {
                this.ns.success({
                    key: 'notify.user.sendResetPassword.email.content',
                    parameters: {
                        user: this.user.firstName + ' ' + this.user.lastName,
                        mail: email
                    }
                }, 'notify.user.sendResetPassword.email.title');
            })
            .catch(err => {
                this.ns.error({
                    key: 'notify.user.sendResetPassword.email.error.content',
                    parameters: {
                        user: this.user.firstName + ' ' + this.user.lastName,
                        mail: email
                    }
                }, 'notify.user.sendResetPassword.email.error.title', err);
            })
    }

    sendResetPasswordMobile(mobile: string) {
        this.spinner.perform('portal-content', this.details.sendResetPassword({type: 'mobile', value: mobile}))
            .then(() => {
                this.ns.success({
                    key: 'notify.user.sendResetPassword.mobile.content',
                    parameters: {
                        user: this.user.firstName + ' ' + this.user.lastName,
                    }
                }, 'notify.user.sendResetPassword.mobile.title');
            })
            .catch(err => {
                this.ns.error({
                    key: 'notify.user.sendResetPassword.mobile.error.content',
                    parameters: {
                        user: this.user.firstName + ' ' + this.user.lastName,
                        mobile: mobile
                    }
                }, 'notify.user.sendResetPassword.mobile.error.title', err);
            })
    }

    sendIndividualMassMail(type: string) {
        this.spinner.perform('portal-content', this.details.sendIndividualMassMail(type))
            .then(res => {
                var infoKey;
                if (type != 'mail') {
                    this.ajaxDownload(res.data, this.user.firstName + '_' + this.user.lastName + '.pdf');
                    infoKey = 'massmail.pdf.done';
                } else {
                    infoKey = 'massmail.mail.done';
                }

                this.ns.success({
                    key: infoKey,
                    parameters: {}
                }, 'massmail');
            })
            .catch(err => {
                this.ns.error({
                    key: 'massmail.error',
                    parameters: {}
                }, 'massmail', err);
            });
    }

    private createDownloadAnchor() {
        this.downloadAnchor = document.createElement('a');
        this.downloadAnchor.style = 'display: none';
        document.body.appendChild(this.downloadAnchor);
    }

    private ajaxDownload(blob, filename) {
        if (window.navigator.msSaveOrOpenBlob) {
            //IE specific
            window.navigator.msSaveOrOpenBlob(blob, filename);
        } else {
            //Other browsers
            if (this.downloadAnchor === null) {
                this.createDownloadAnchor();
            }
            if (this.downloadObjectUrl !== null) {
                window.URL.revokeObjectURL(this.downloadObjectUrl);
            }
            this.downloadObjectUrl = window.URL.createObjectURL(blob);
            var anchor = this.downloadAnchor;
            anchor.href = this.downloadObjectUrl;
            anchor.download = filename;
            anchor.click();
        }
    }

    generateMergeKey() {
        this.spinner.perform('portal-content', this.details.generateMergeKey());
    }

    getStructures(fn): string {
        return fn.map((id: string) => this.getStructure(id).name).join(', ');
    }

    updateLoginAlias() {
        this.spinner.perform('portal-content', this.details.updateLoginAlias()
            .then(res => {
                this.ns.success({
                    key: 'notify.user.updateLoginAlias.content',
                    parameters: {
                        user: this.user.firstName + ' ' + this.user.lastName
                    }
                }, 'notify.user.updateLoginAlias.title');
                this.infoForm.reset(this.details);
            })
            .catch(err => {
                if (err
                    && err.response
                    && err.response.data
                    && err.response.data.error
                    && (err.response.data.error.includes('already exists') || err.response.data.error.includes('existe déjà'))) {
                    this.ns.error({
                        key: 'notify.user.updateLoginAlias.uniqueConstraint.content',
                        parameters: {
                            loginAlias: this.details.loginAlias
                        }
                    }, 'notify.user.updateLoginAlias.uniqueConstraint.title');
                } else {
                    this.ns.error({
                        key: 'notify.user.updateLoginAlias.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName
                        }
                    }, 'notify.user.updateLoginAlias.error.title', err);
                }
                this.details.loginAlias = '';
                this.loginAliasInput.setErrors({'incorrect': true});
            })
        );
    }

    clickOnGenerateRenewalCode() {
        this.generateRenewalCode(this.user.login)
            .subscribe(data => {
                this.renewalCode = data.renewalCode;
                this.cdRef.markForCheck();
            });
    }

    generateRenewalCode(login: string): Observable<{ renewalCode: string }> {
        return this.http.post<{ renewalCode: string }>('/auth/generatePasswordRenewalCode',
            new HttpParams().set('login', login).toString(), {
                headers: new HttpHeaders().set('Content-Type', 'application/x-www-form-urlencoded')
            });
    }

    displayDate(date: string): string {
        return new Date(date).toLocaleDateString(this.bundles.currentLanguage);
    }
}
