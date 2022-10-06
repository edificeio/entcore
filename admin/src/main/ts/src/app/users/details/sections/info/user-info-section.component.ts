import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  ViewChild,
} from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { AbstractControl } from "@angular/forms";
import { Subscription } from "rxjs";
import { BundlesService } from "ngx-ode-sijil";

import { AbstractSection } from "../abstract.section";
import { UserInfoService } from "./user-info.service";
import { Config } from "../../../../core/resolvers/Config";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { UserModel } from "src/app/core/store/models/user.model";
import { NotifyService } from "src/app/core/services/notify.service";
import { SpinnerService } from "ngx-ode-ui";
import { PlatformInfoService } from "src/app/core/services/platform-info.service";
import { SessionModel } from "src/app/core/store/models/session.model";
import { Session } from "src/app/core/store/mappings/session";

@Component({
  selector: "ode-user-info-section",
  templateUrl: "./user-info-section.component.html",
  styleUrls: ['./user-info-section.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserInfoSectionComponent
  extends AbstractSection
  implements OnInit
{
  showAddAdmlConfirmation: boolean = false;
  showRemoveAdmlConfirmation: boolean = false;

  userInfoSubscriber: Subscription;

  isAdmc: boolean = false;

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
    this.userInfoSubscriber = this.userInfoService
      .getState()
      .subscribe(() => this.cdRef.markForCheck());

    const session: Session = await SessionModel.getSession();
    this.isAdmc = session.isADMC();
    this.cdRef.markForCheck();
  }

  protected onUserChange() {
  }

  addAdml() {
    this.showAddAdmlConfirmation = false;
    this.spinner
      .perform("portal-content", this.details.addAdml(this.structure.id))
      .then(() => {
        this.ns.success(
          {
            key: "notify.user.add.adml.content",
            parameters: {
              user: this.user.firstName + " " + this.user.lastName,
            },
          },
          "notify.user.add.adml.title"
        );
        this.cdRef.markForCheck();
      })
      .catch(err => {
        this.ns.error(
          {
            key: "notify.user.add.adml.error.content",
            parameters: {
              user: this.user.firstName + " " + this.user.lastName,
            },
          },
          "notify.user.add.adml.error.title",
          err
        );
      });
  }

  removeAdml() {
    this.showRemoveAdmlConfirmation = false;
    this.spinner
      .perform("portal-content", this.details.removeAdml())
      .then(() => {
        this.ns.success(
          {
            key: "notify.user.remove.adml.content",
            parameters: {
              user: this.user.firstName + " " + this.user.lastName,
            },
          },
          "notify.user.remove.adml.title"
        );
        this.cdRef.markForCheck();
      })
      .catch(err => {
        this.ns.error(
          {
            key: "notify.user.remove.adml.error.content",
            parameters: {
              user: this.user.firstName + " " + this.user.lastName,
            },
          },
          "notify.user.remove.adml.error.title",
          err
        );
      });
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
      return this.bundles.translate("member.of.n.structures", {
        count: structureIds.length,
      });
    } else {
      return structureNames.join(", ");
    }
  }

  displayDate(date: string): string {
    return new Date(date).toLocaleDateString(this.bundles.currentLanguage);
  }

  showAddAdmlButton() {
    if (this.details.isAdml(this.structure.id)) {
      return false;
    }
    if (this.isAdmc) {
      return true;
    }
    return this.user.type !== "Student" && this.user.type !== "Relative";
  }
}
