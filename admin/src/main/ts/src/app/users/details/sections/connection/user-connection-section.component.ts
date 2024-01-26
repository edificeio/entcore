import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  ViewChild,
} from "@angular/core";
import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { AbstractControl, NgForm } from "@angular/forms";
import { Observable, Subscription } from "rxjs";
import { BundlesService } from "ngx-ode-sijil";

import { AbstractSection } from "../abstract.section";
import { UserInfoService } from "../info/user-info.service";
import { Config } from "../../../../core/resolvers/Config";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { UserModel } from "src/app/core/store/models/user.model";
import { NotifyService } from "src/app/core/services/notify.service";
import { SpinnerService } from "ngx-ode-ui";
import { PlatformInfoService } from "src/app/core/services/platform-info.service";
import { SessionModel } from "src/app/core/store/models/session.model";
import { Session } from "src/app/core/store/mappings/session";
import { catchError, tap } from "rxjs/operators";

@Component({
  selector: "ode-user-connection-section",
  templateUrl: "./user-connection-section.component.html",
  styleUrls: ['./user-connection-section.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserConnectionSectionComponent
  extends AbstractSection
  implements OnInit
{
  email: string;
  passwordResetMail: string;
  passwordResetMobile: string;
  smsModule: boolean | string;
  showMergedLogins: boolean = false;
  showChangeLoginConfirmation: boolean = false;
  showMassMailConfirmation = false;
  downloadAnchor = null;
  downloadObjectUrl = null;
  renewalCode: string | undefined = undefined;

  userInfoSubscriber: Subscription;

  loginAliasPattern = /^[0-9a-z\.]+$/;

  isAdmc: boolean = false;
  isAdml: boolean = false;
  myID: string = "";
  isUpdateLoginSaved: boolean = false;
  isUpdateMailSaved: boolean = false;
  isHomePhoneSaved: boolean = false;
  isMobileSaved: boolean = false;

  @Input() structure: StructureModel;
  
  _inUser: UserModel;
  get inUser() {
    return this._inUser;
  }
  @Input() set inUser(user: UserModel) {
      this._inUser = user;
      this.user = user;
  }

  @Input() config: Config;
  @Input() simpleUserDetails: boolean;

  @ViewChild("connectionForm") connectionForm: NgForm;
  @ViewChild("mailForm") mailForm: NgForm;

  @ViewChild("loginInput", { static: false })
  loginInput: AbstractControl;

  @ViewChild("loginAliasInput")
  loginAliasInput: AbstractControl;

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
    private userInfoService: UserInfoService
  ) {
    super();
  }

  async ngOnInit() {
    this.email = this.details.email;
    this.passwordResetMail = this.details.email;
    this.passwordResetMobile = this.details.mobile;
    PlatformInfoService.isSmsModule().then(res => {
      this.smsModule = res;
      this.cdRef.markForCheck();
    });

    this.userInfoSubscriber = this.userInfoService
      .getState()
      .subscribe(() => this.cdRef.markForCheck());

    const session: Session = await SessionModel.getSession();
    this.isAdmc = session.isADMC();
    this.isAdml = session.isADML();
    this.myID   = session.userId;
    this.cdRef.markForCheck();
  }

  protected onUserChange() {
    this.email = this.details.email;
    if (!this.details.activationCode) {
      this.passwordResetMail = this.details.email;
      this.passwordResetMobile = this.details.mobile;
    }
    this.renewalCode = undefined;
  }

  sendResetPasswordMail(email: string) {
    if( this.isForbidden )
      return;
    this.spinner
      .perform(
        "portal-content",
        this.details.sendResetPassword({ type: "email", value: email })
      )
      .then(() => {
        this.ns.success(
          {
            key: "notify.user.sendResetPassword.email.content",
            parameters: {
              user: this.user.firstName + " " + this.user.lastName,
              mail: email,
            },
          },
          "notify.user.sendResetPassword.email.title"
        );
      })
      .catch(err => {
        this.ns.error(
          {
            key: "notify.user.sendResetPassword.email.error.content",
            parameters: {
              user: this.user.firstName + " " + this.user.lastName,
              mail: email,
            },
          },
          "notify.user.sendResetPassword.email.error.title",
          err
        );
      });
  }

  sendResetPasswordMobile(mobile: string) {
    if( this.isForbidden )
      return;
    this.spinner
      .perform(
        "portal-content",
        this.details.sendResetPassword({ type: "mobile", value: mobile })
      )
      .then(() => {
        this.ns.success(
          {
            key: "notify.user.sendResetPassword.mobile.content",
            parameters: {
              user: this.user.firstName + " " + this.user.lastName,
            },
          },
          "notify.user.sendResetPassword.mobile.title"
        );
      })
      .catch(err => {
        this.ns.error(
          {
            key: "notify.user.sendResetPassword.mobile.error.content",
            parameters: {
              user: this.user.firstName + " " + this.user.lastName,
              mobile,
            },
          },
          "notify.user.sendResetPassword.mobile.error.title",
          err
        );
      });
  }

  sendIndividualMassMail(type: string) {
    this.showMassMailConfirmation = false;
    this.spinner
      .perform("portal-content", this.details.sendIndividualMassMail(type))
      .then(res => {
        let infoKey;
        if (type != "mail") {
          this.ajaxDownload(
            res.data,
            this.user.firstName + "_" + this.user.lastName + ".pdf"
          );
          infoKey = "massmail.pdf.done";
        } else {
          infoKey = "massmail.mail.done";
        }

        this.ns.success(
          {
            key: infoKey,
            parameters: {},
          },
          "massmail"
        );
      })
      .catch(err => {
        this.ns.error(
          {
            key: "massmail.error",
            parameters: {},
          },
          "massmail",
          err
        );
      });
  }

  private createDownloadAnchor() {
    this.downloadAnchor = document.createElement("a");
    this.downloadAnchor.style = "display: none";
    document.body.appendChild(this.downloadAnchor);
  }

  private ajaxDownload(blob, filename) {
    const nav: any = window.navigator;
    if (nav && nav.msSaveOrOpenBlob) {
      // IE specific
      nav.msSaveOrOpenBlob(blob, filename);
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
    this.spinner.perform("portal-content", this.details.generateMergeKey().then(() => this.cdRef.markForCheck()));
  }

  unmerge(mergedLogin:string) {
    const payload = {
      originalUserId: this.details.id,
      mergedLogins: [mergedLogin]
    }
    this.spinner.perform("portal-content", 
      this.http.post<{mergedLogins:Array<string>}>('/directory/duplicate/user/unmergeByLogins', payload).pipe(
        tap( result => {
          this.ns.success({
            key: 'notify.user.unmerge.content',
            parameters: {mergedLogin: mergedLogin}
          }, 'notify.user.unmerge.title');

          if( result.mergedLogins ) {
            this.details.mergedLogins = result.mergedLogins;
          } else {
            this.details.mergedLogins.splice( this.details.mergedLogins.indexOf(mergedLogin), 1 );
          }
        }),
        catchError( err => {
          this.ns.error({
            key: 'notify.user.unmerge.error.content',
            parameters: {mergedLogin: mergedLogin}
          }, 'notify.user.unmerge.error.title', err);
          throw err;
        })
      ).toPromise()    
    );
  }

  /* => An AMDL can not edit another administrator info. */
  get isForbidden(): boolean {
    return this.isAdml && this.details && (this.details.isAdmc() || this.details.isAdml()) && this.details.id != this.myID;
  }

  get isMyAdmlAccount(): boolean {
    return this.isAdml && this.details.id === this.myID;
  }

  updateMail() {
    if( this.isForbidden )
      return;
    if (this.isMyAdmlAccount) {
      const redirectUrl = encodeURI(document.location.href);
      window.location.href = `/auth/validate-mail?force=true&redirect=${redirectUrl}`;
      return;
    }
    this.details.email = this.email;
    this.spinner.perform('portal-content', this.details.updateMail())
      .then(() => {
        this.ns.success(
          {
            key: 'notify.user.email.content',
            parameters: {
              user: this.details.firstName + ' ' + this.details.lastName
            }
          }, 'notify.user.email.title');

        this.userInfoService.setState(this.details);
        this.isUpdateMailSaved = true;
      })
      .catch(err => {
        this.ns.error(
          {
            key: 'notify.user.email.error.content',
            parameters: {
              user: this.user.firstName + ' ' + this.user.lastName
            }
          }, 'notify.user.email.error.title', err);
      });
  }

  updateHomePhone() {
    if( this.isForbidden )
      return;
    this.spinner.perform('portal-content', this.details.updateHomePhone())
    .then(() => {
      this.ns.success(
        {
          key: 'notify.user.updatePhone.content',
          parameters: {
            user: this.details.firstName + ' ' + this.details.lastName
          }
        }, 'notify.user.updatePhone.title');

      this.userInfoService.setState(this.details);
      this.isHomePhoneSaved = true;
    })
    .catch(err => {
      this.ns.error(
        {
          key: 'notify.user.updatePhone.error.content',
          parameters: {
            user: this.user.firstName + ' ' + this.user.lastName
          }
        }, 'notify.user.updatePhone.error.title', err);
    });
  }

  updateMobile() {
    if( this.isForbidden )
      return;
    if (this.isMyAdmlAccount) {
      const redirectUrl = encodeURI(document.location.href);
      window.location.href = `/auth/validate-mail?type=sms&force=true&redirect=${redirectUrl}`;
      return;
    }
    this.spinner.perform('portal-content', this.details.updateMobile())
    .then(() => {
      this.ns.success(
        {
          key: 'notify.user.updatePhone.content',
          parameters: {
            user: this.details.firstName + ' ' + this.details.lastName
          }
        }, 'notify.user.updatePhone.title');

      this.userInfoService.setState(this.details);
      this.isMobileSaved = true;
    })
    .catch(err => {
      this.ns.error(
        {
          key: 'notify.user.updatePhone.error.content',
          parameters: {
            user: this.user.firstName + ' ' + this.user.lastName
          }
        }, 'notify.user.updatePhone.error.title', err);
    });
  }

  updateLoginAlias() {
    this.spinner.perform(
      "portal-content",
      this.details
        .updateLoginAlias()
        .then(res => {
          this.ns.success(
            {
              key: "notify.user.updateLoginAlias.content",
              parameters: {
                user: this.details.firstName + " " + this.details.lastName,
              },
            },
            "notify.user.updateLoginAlias.title"
          );
          this.connectionForm.reset(this.details);
        })
        .catch(err => {
          if (
            err &&
            err.response &&
            err.response.data &&
            err.response.data.error &&
            (err.response.data.error.includes("already exists") ||
              err.response.data.error.includes("existe déjà"))
          ) {
            this.ns.error(
              {
                key: "notify.user.updateLoginAlias.uniqueConstraint.content",
                parameters: {
                  loginAlias: this.details.loginAlias,
                },
              },
              "notify.user.updateLoginAlias.uniqueConstraint.title"
            );
          } else {
            this.ns.error(
              {
                key: "notify.user.updateLoginAlias.error.content",
                parameters: {
                  user: this.user.firstName + " " + this.user.lastName,
                },
              },
              "notify.user.updateLoginAlias.error.title",
              err
            );
          }
          this.details.loginAlias = "";
          this.loginAliasInput.setErrors({ incorrect: true });
        })
    );
  }

  updateLogin() {
    this.spinner.perform(
      "portal-content",
      this.details
        .updateLogin()
        .then(res => {
          this.ns.success(
            {
              key: "notify.user.updateLogin.content",
              parameters: {
                user: this.details.firstName + " " + this.details.lastName,
              },
            },
            "notify.user.updateLogin.title"
          );
          this.mailForm.reset(this.details);
          this.isUpdateLoginSaved = true;
        })
        .catch(err => {
          if (
            err &&
            err.response &&
            err.response.data &&
            err.response.data.error &&
            (err.response.data.error.includes("already exists") ||
              err.response.data.error.includes("existe déjà"))
          ) {
            this.ns.error(
              {
                key: "notify.user.updateLogin.uniqueConstraint.content",
                parameters: {
                  login: this.details.login,
                },
              },
              "notify.user.updateLogin.uniqueConstraint.title"
            );
          } else {
            this.ns.error(
              {
                key: "notify.user.updateLogin.error.content",
                parameters: {
                  user: this.user.firstName + " " + this.user.lastName,
                },
              },
              "notify.user.updateLogin.error.title",
              err
            );
          }
          this.details.login = "";
          this.loginInput.setErrors({ incorrect: true });
        })
    );
  }

  clickOnGenerateRenewalCode() {
    if( this.isForbidden )
      return;
    this.generateRenewalCode(this.user.login).subscribe(data => {
      this.renewalCode = data.renewalCode;
      this.cdRef.markForCheck();
    });
  }

  generateRenewalCode(login: string): Observable<{ renewalCode: string }> {
    return this.http.post<{ renewalCode: string }>(
      "/auth/generatePasswordRenewalCode",
      new HttpParams().set("login", login).toString(),
      {
        headers: new HttpHeaders().set(
          "Content-Type",
          "application/x-www-form-urlencoded"
        ),
      }
    );
  }

  displayDate(date: string): string {
    return new Date(date).toLocaleDateString(this.bundles.currentLanguage);
  }

  showLightbox() {
    this.showMassMailConfirmation = true;
  }

}
