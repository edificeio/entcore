import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit, ViewChild, Input, Output, EventEmitter } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Data, NavigationEnd } from "@angular/router";
import { OdeComponent } from "ngx-ode-core";
import { SpinnerService } from "ngx-ode-ui";
import { NotifyService } from "src/app/core/services/notify.service";
import { Config } from "src/app/core/resolvers/Config";
import { MergedWithDescription, UnlinkedUserDetails, UnlinkedUserService } from "../unlinked.service";
import { AbstractControl, NgForm } from "@angular/forms";
import { catchError, tap } from "rxjs/operators";

@Component({
  selector: "ode-unlinked-user-details",
  templateUrl: "./user-details.component.html",
  styleUrls: ["./user-details.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UnlinkedUserDetailsComponent extends OdeComponent implements OnInit, OnDestroy {
  @ViewChild('administrativeForm') administrativeForm: NgForm;
  @ViewChild('firstNameInput')     firstNameInput: AbstractControl;
  @ViewChild('lastNameInput')      lastNameInput: AbstractControl;

  @Output() ondelete: EventEmitter<UnlinkedUserDetails> = new EventEmitter<UnlinkedUserDetails>();

  public config: Config;

  private SECONDS_IN_DAYS = 24 * 3600;
  private MILLISECONDS_IN_DAYS = this.SECONDS_IN_DAYS * 1000;

  public showRemoveUserConfirmation = false;
//  public showPersEducNatBlockingConfirmation = false;
  public showMergedLogins: boolean = false;
  public details: UnlinkedUserDetails;
  public mergedWithDetails: MergedWithDescription|null = null;
  public imgSrc: string;
  public imgLoaded: boolean = false;
  public editMode: boolean = false;

  constructor(
    injector: Injector,
    public spinner: SpinnerService,
    private http: HttpClient,
    private ns: NotifyService,
    private svc:UnlinkedUserService,
  ) {
    super(injector);
  }

  ngOnInit() {
    super.ngOnInit();
    this.subscriptions.add(
      this.route.data.subscribe((data: Data) => {
        this.details = data.userDetails as UnlinkedUserDetails;
        this.config = data.config as Config;

        this.imgSrc = "/userbook/avatar/" + this.details.id + "?thumbnail=100x100";
        this.editMode = false;
        this.changeDetector.markForCheck();
      })
    );

    // Scroll top in case of details switching, see comments on CAV2 #280
    this.router.events.subscribe(evt => {
      if (!(evt instanceof NavigationEnd)) return;
      window.scrollTo(0, 0);
    });
  }

  showMergedWithDetails() {
    if( this.details.mergedWith ) {
      this.spinner.perform("portal-content",
        this.svc.getMergedWithDetails(this.details.mergedWith).then( desc => {
          this.mergedWithDetails = desc;
          this.showMergedLogins = true;
          this.changeDetector.markForCheck();
        })
      );
    }
  }

  unmerge() {
    this.spinner.perform("portal-content", 
      this.svc.unmerge(this.details.mergedWith, this.details.login)
      .then( ()=> {
        if( this.details.mergedWith ) delete this.details.mergedWith;
        this.changeDetector.markForCheck();
      })
    );
  }

  restoreUser() {
    const parameters = {
      user: `${this.details.firstName} ${this.details.lastName}`
    };
    this.spinner
      .perform("portal-content", this.svc.restore(this.details))
      .then(() => {
        this.changeDetector.markForCheck();
        this.ns.success(
          { key: "notify.user.restore.content", parameters:parameters },
          { key: "notify.user.restore.title", parameters:parameters }
        );
      })
      .catch(() => {
        this.ns.error(
          { key: "notify.user.restore.error.content", parameters:parameters },
          { key: "notify.user.restore.error.title", parameters:parameters }
        );
    });
  }

  removeUser() {
    const parameters = {
      user: `${this.details.firstName} ${this.details.lastName}`,
      numberOfDays: this.millisecondToDays(this.config["delete-user-delay"]),
    };
    const msgRoot = this.isActive() ? "notify.user.predelete" : "notify.user.delete";

    this.spinner
      .perform("portal-content", this.svc.delete(this.details))
      .then(() => {
        if( this.isActive() ) {
          // User is now pre-deleted
          this.details.deleteDate = Date.now();
        } else {
          // Notify deletion
          this.ondelete.emit( this.details );
        }
        this.ns.success(
          { key: msgRoot+".content", parameters:parameters },
          { key: msgRoot+".title", parameters:parameters }
        );
      })
      .catch(err => {
        this.ns.error(
          { key: msgRoot+".error.content", parameters:parameters },
          { key: msgRoot+".error.title", parameters:parameters },
          err
        );
      })
      .finally( () => {
        this.showRemoveUserConfirmation = false;
        this.changeDetector.markForCheck();
      });
  }

  saveAdministrativeData() {
    const updatedFields = {};
    Object.keys(this.administrativeForm.controls).forEach( ctlName => {
      const ctl = this.administrativeForm.controls[ctlName];
      if( ctl && ctl.dirty ) {
        updatedFields[ctlName] = ctl.value;
      }
    });
    
    if( Object.keys(updatedFields).length ) {
      this.spinner.perform('portal-content', this.svc.update(this.details.id, updatedFields))
      .then( u => {
          //this.administrativeForm.reset(updatedFields);
          Object.assign( this.details, u);
          Object.keys(this.administrativeForm.controls).forEach( ctlName => {
            if( typeof u[ctlName] !== "undefined" ) {
              const ctl = this.administrativeForm.controls[ctlName];
              ctl.setValue( u[ctlName] );
            }
          });

          this.ns.success(
              {
                  key: 'notify.user.update.content',
                  parameters: {
                      user: this.details.firstName + ' ' + this.details.lastName
                  }
              }, 'notify.user.update.title');
      })
      .catch(err => {
          this.ns.error(
              {
                  key: 'notify.user.update.error.content',
                  parameters: {
                      user: this.details.firstName + ' ' + this.details.lastName
                  }
              }, 'notify.user.update.error.title', err);
      });
    }
  }

  millisecondToDays(millisecondTimestamp: number): number {
    return Math.ceil(millisecondTimestamp / this.MILLISECONDS_IN_DAYS);
  }

  millisecondsUntilEffectiveDeletion(timestamp: number): number {
    return timestamp + this.config["delete-user-delay"] - new Date().getTime();
  }

  millisecondsUntilPreDeletion(timestamp: number, profile: string): number {
    return (
      timestamp +
      this.config[profile.toLowerCase() + "-pre-delete-delay"] -
      new Date().getTime()
    );
  }

  isContextAdml() {
    if( this.details.functions && this.details.functions.length > 0 ) {
      const admlIndex = this.details.functions.findIndex(
        f => f[0] === "ADMIN_LOCAL"
      );
      return admlIndex >= 0;
    }
    return false;
  }

  isUnblocked() {
    return this.details != null && !this.details.blocked;
  }

  isRemovable() {
    return !this.details.deleteDate;
  }

  isActive() {
    return !(
      this.details.activationCode && this.details.activationCode.length > 0
    );
  }

  onToggleEdit() {
    if( this.editMode ) {
        this.saveAdministrativeData();
    }
    this.editMode = !this.editMode;
  }

  imgLoad() {
    this.imgLoaded = true;
  }

  isStudent() {
    return this.details.type.indexOf("Student")>=0;
  }
}
