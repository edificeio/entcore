import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnInit,
} from "@angular/core";

import { AbstractSection } from "../abstract.section";
import { SpinnerService } from "ngx-ode-ui";
import { UserPosition } from "src/app/core/store/models/userPosition.model";
import { UserPositionServices } from "src/app/core/services/user-position.service";
import { UserInfoService } from "../info/user-info.service";
import { NotifyService } from "src/app/core/services/notify.service";

@Component({
  selector: "ode-user-positions-section",
  templateUrl: "./user-positions-section.component.html",
  styleUrls: ["./user-positions-section.component.scss"],
  inputs: ["user", "structure"],
  //changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserPositionsSectionComponent
  extends AbstractSection
  implements OnInit
{
  positionList: UserPosition[];
  userPositions: UserPosition[];
  searchPrefix: string = "";
  newPosition: UserPosition = {name: "", source: "MANUAL"};
  showUserPositionSelectionLightbox: boolean = false;
  showUserPositionCreationLightbox: boolean = false;
  showEmptyScreen: boolean = false;

  /** Truthy when detecting user's positions changes */
  get hasUserPositionsChanged(): boolean {
    return (!this.details.userPositions && this.userPositions.length > 0) || 
      (this.details.userPositions && 
        this.details.userPositions.map((p) => p.id) !== this.userPositions.map((p) => p.id));
  }

  /** List of available positions = all positions except those already assigned. */
  get filteredPositionList() {
    return this.positionList?.filter( position => !this.userPositions.some(value=>value.id===position.id)) ?? [];
  }

  constructor(
    private ns: NotifyService,
    public spinner: SpinnerService,
    protected cdRef: ChangeDetectorRef,
    private userInfoService: UserInfoService,
    private userPositionServices: UserPositionServices
  ) {
    super();
  }

  async ngOnInit() {
    this.setNewPositionName(undefined);
    this.positionList = await this.spinner
      .perform('portal-content', this.userPositionServices.searchUserPositions())
      .catch(err => {
        // TODO notification
        // this.ns.error(
        //     {
        //         key: 'notify.user.update.error.content',
        //         parameters: {
        //             user: this.user.firstName + ' ' + this.user.lastName
        //         }
        //     }, 'notify.user.update.error.title', err);
        return [];
      });
      
    // Memoize the initial user's positions
    this.userPositions = this.details.userPositions
      ? [...this.details.userPositions]
      : [];
  }

  protected onUserChange() {
    // Memoize the user's new positions
    this.userPositions = this.details.userPositions
      ? [...this.details.userPositions]
      : [];
    this.cdRef.markForCheck();
  }

  setNewPositionName(name) {
    name = name ? name.trim() : "";
    // Check if the name of this new position does not already exist in the list
    if( this.positionList && 
        !this.positionList.some(position => position.name===name) ) {
      this.newPosition = {name, source: "MANUAL"};
      this.showEmptyScreen = name && name.length;
    }
  }

  selectUserPosition(userPosition: UserPosition) {
    this.userPositions.push(userPosition);
    this.showUserPositionSelectionLightbox = false;
  }

  removeUserPosition(position: UserPosition) {
    this.userPositions = this.userPositions.filter((p) => p.id !== position.id);
  }

  openUserPositionCreationModal() {
    this.showUserPositionSelectionLightbox = false;
    this.showUserPositionCreationLightbox = true;
  }

  addUserPositionToList(position: UserPosition | undefined) {
    if( position ) {
      this.positionList.push(position);
    }
    this.showUserPositionCreationLightbox = false;
  }

  saveUpdate() {
    if( this.userPositions && this.userPositions.length>0) {
      this.details.userPositions = [...this.userPositions];
    } else {
      this.details.userPositions = [];
    }
    this.spinner
      .perform('portal-content', this.details.updateUserPositions())
      .then(() => {
        // TODO notification
        // this.ns.success(
        //     {
        //         key: 'notify.user.update.content',
        //         parameters: {
        //             user: this.details.firstName + ' ' + this.details.lastName
        //         }
        //     }, 'notify.user.update.title');

        this.userInfoService.setState(this.details);
      })
      .catch(err => {
        // TODO notification
        // this.ns.error(
        //     {
        //         key: 'notify.user.update.error.content',
        //         parameters: {
        //             user: this.user.firstName + ' ' + this.user.lastName
        //         }
        //     }, 'notify.user.update.error.title', err);
      });
  }
}
