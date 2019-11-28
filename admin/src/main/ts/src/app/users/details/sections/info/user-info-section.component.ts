import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit, ViewChild} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {AbstractControl, NgForm} from '@angular/forms';
import {Observable, Subscription} from 'rxjs';
import {BundlesService} from 'sijil';

import {AbstractSection} from '../abstract.section';
import {UserInfoService} from './user-info.service';
import {Config} from '../../../../core/resolvers/Config';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { UserModel } from 'src/app/core/store/models/user.model';
import { NotifyService } from 'src/app/core/services/notify.service';
import { SpinnerService } from 'src/app/core/services/spinner.service';
import { PlatformInfoService } from 'src/app/core/services/platform-info.service';

@Component({
    selector: 'ode-user-info-section',
    templateUrl: './user-info-section.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserInfoSectionComponent extends AbstractSection implements OnInit {
    passwordResetMail: string;
    passwordResetMobile: string;
    smsModule: boolean | string;
    showConfirmation = false;
    showMassMailConfirmation = false;
    downloadAnchor = null;
    downloadObjectUrl = null;
    renewalCode: string | undefined = undefined;

    userInfoSubscriber: Subscription;

    loginAliasPattern = /^[0-9a-z\-\.]+$/;

    @Input() structure: StructureModel;
    @Input() user: UserModel;
    @Input() config: Config;

    @ViewChild('infoForm', {static: false}) infoForm: NgForm;

    @ViewChild('loginAliasInput', {static: false}) loginAliasInput: AbstractControl;

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
        this.renewalCode = undefined;
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
            });
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
                        mobile
                    }
                }, 'notify.user.sendResetPassword.mobile.error.title', err);
            });
    }

    sendIndividualMassMail(type: string) {
        this.showMassMailConfirmation = false;
        this.spinner.perform('portal-content', this.details.sendIndividualMassMail(type))
            .then(res => {
                let infoKey;
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

    generateMergeKey() {
        this.spinner.perform('portal-content', this.details.generateMergeKey());
    }

    displayAdmlStructureNames(structureIds: string[]): string {
        let notInGlobalStoreStructure = false;
        const structureNames: string[] = [];

        structureIds.forEach((structureId: string) => {
            const structure: StructureModel = this.getStructure(structureId);
            if (!structure) {
                notInGlobalStoreStructure = true;
            } else {
                structureNames.push(structure.name);
            }
        });

        if (notInGlobalStoreStructure) {
            return this.bundles.translate('member.of.n.structures', {count: structureIds.length});
        } else {
            return structureNames.join(', ');
        }
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
                this.loginAliasInput.setErrors({incorrect: true});
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

    showLightbox() {
        this.showMassMailConfirmation = true;
    }
}
