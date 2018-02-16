import { Component, Input, ViewChild, ChangeDetectorRef, OnInit, ChangeDetectionStrategy } from '@angular/core'
import { NgModel, NgForm, AbstractControl } from '@angular/forms'

import { AbstractSection } from '../abstract.section'
import { SpinnerService, NotifyService, PlateformeInfoService } from '../../../../core/services'
import { UserDetailsModel } from '../../../../core/store';

import { UserInfoService } from './user-info.service'
import { Subscription } from 'rxjs';

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
                <form-field label="mergeKey" *ngIf="user.type === 'Relative'">
                    <div>
                        <span *ngIf="details.mergeKey">{{ details.mergeKey }}</span>
                        <button class= "noflex"
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
                        <div *ngFor="let function of details.functions">
                            <div>
                                {{ function[0] | translate }}
                                <span *ngIf="function[1] && function[1].length > 0 && getStructure(function[1][0])">
                                    ({{ getStructures(function[1]) }})
                                </span>
                                <span *ngIf="function[1] && function[1].length > 0 && !getStructure(function[1][0])">
                                    ({{ 'member.of.n.structures' | translate:{ count: function[1].length } }})
                                </span>
                            </div>
                            <div *ngIf="function[0] == 'ADMIN_LOCAL'">
                                <lightbox-confirm
                                    [show]="showConfirmation"
                                    [title]="'warning'"
                                    (onConfirm)="removeAdml()"
                                    (onCancel)="showConfirmation = false">
                                    <p>{{ 'user.remove.adml.disclaimer.info' | translate:{ username: user.displayName } }}</p>
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
                                [disabled]="!passwordResetMobile || passwordMobileInput.errors ">
                                <span><s5l>send.reset.password.button</s5l></span>
                                <i class="fa fa-mobile"></i>
                            </button>
                        </div>
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
    passwordResetMail: string
    passwordResetMobile: string
    smsModule: boolean
    showConfirmation: boolean = false;
    downloadAnchor = null;
    downloadObjectUrl = null;

    userInfoSubscriber: Subscription;
    loginAliasPattern = /^[0-9a-z\-\.]+$/;

    @Input() structure;
    @Input() user;

    @ViewChild('infoForm') infoForm : NgForm;
    @ViewChild('loginAliasInput') loginAliasInput: AbstractControl;

    constructor(
        private ns: NotifyService,
        public spinner: SpinnerService,
        private cdRef: ChangeDetectorRef,
        private userInfoService: UserInfoService) {
        super();
    }

    ngOnInit() {
        this.passwordResetMail = this.details.email;
        this.passwordResetMobile = this.details.mobile;
        PlateformeInfoService.isSmsModule().then(res => {
            this.smsModule = res
            this.cdRef.markForCheck()
        });

        this.userInfoSubscriber = this.userInfoService.getState().subscribe(
            userInfoState => {
                this.cdRef.markForCheck();
            }
        );
    }

    protected onUserChange(){
        if(!this.details.activationCode) {
            this.passwordResetMail = this.details.email
            this.passwordResetMobile = this.details.mobile
        }
    }

    addAdml() {
        this.spinner.perform('portal-content', this.details.addAdml(this.structure.id))
            .then(res => {
                this.ns.success({
                        key: 'notify.user.add.adml.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.add.adml.title');
                this.cdRef.markForCheck();
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.add.adml.error.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.add.adml.error.title', err)
            })
    }

    removeAdml() {
        this.showConfirmation = false;
        this.spinner.perform('portal-content', this.details.removeAdml())
            .then(res => {
                this.ns.success({
                        key: 'notify.user.remove.adml.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.remove.adml.title');
                this.cdRef.markForCheck();
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.remove.adml.error.content',
                        parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                    }, 'notify.user.remove.adml.error.title', err)
            })
    }

    sendResetPasswordMail(email: string) {
        this.spinner.perform('portal-content', this.details.sendResetPassword({type: 'email', value: email}))
            .then(res => {
                this.ns.success({
                        key: 'notify.user.sendResetPassword.email.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                            mail: email
                        }
                    }, 'notify.user.sendResetPassword.email.title')
            })
            .catch(err => {
                this.ns.error({
                        key: 'notify.user.sendResetPassword.email.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                            mail: email   
                        }
                    }, 'notify.user.sendResetPassword.email.error.title', err)
            })
    }

    sendResetPasswordMobile(mobile: string) {
        this.spinner.perform('portal-content', this.details.sendResetPassword({type: 'mobile', value: mobile}))
            .then(res => {
                this.ns.success({
                        key: 'notify.user.sendResetPassword.mobile.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                        }
                    }, 'notify.user.sendResetPassword.mobile.title')
            })
            .catch(err => {
                this.ns.error({
                        key: 'notify.user.sendResetPassword.mobile.error.content',
                        parameters: {
                            user: this.user.firstName + ' ' + this.user.lastName,
                            mobile: mobile   
                        }
                    }, 'notify.user.sendResetPassword.mobile.error.title', err)
            })
    }

    sendIndividualMassMail(type: string) {
        this.spinner.perform('portal-content', this.details.sendIndividualMassMail(type))
            .then(res => {
                var infoKey;
                if(type != 'mail') {
                    this.ajaxDownload(res.data, this.user.firstName + '_' + this.user.lastName + '.pdf');
                    infoKey = 'massmail.pdf.done';
                }else{
                    infoKey = 'massmail.mail.done';
                }

                this.ns.success({
                        key: infoKey,
                        parameters: {}
                    }, 'massmail')
            })
            .catch(err => {
                this.ns.error({
                        key: 'massmail.error',
                        parameters: {}
                    }, 'massmail', err)
            })
    }
    
    private createDownloadAnchor() {
        this.downloadAnchor = document.createElement('a');
        this.downloadAnchor.style = "display: none";
        document.body.appendChild(this.downloadAnchor);
    }

    private ajaxDownload(blob, filename) {
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
            var anchor = this.downloadAnchor
            anchor.href = this.downloadObjectUrl
            anchor.download = filename
            anchor.click()
        }
    }

    generateMergeKey() {
        this.spinner.perform('portal-content', this.details.generateMergeKey())
    }

    getStructures(fn) {
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
}
