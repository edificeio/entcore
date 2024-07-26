import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  OnChanges,
  OnInit,
} from "@angular/core";

import { AbstractSection } from "../abstract.section";
import { SpinnerService } from "ngx-ode-ui";
import { UserPosition } from "src/app/core/store/models/userPosition.model";
import { UserPositionServices } from "src/app/core/services/user-position.service";

@Component({
  selector: "ode-user-positions-section",
  templateUrl: "./user-positions-section.component.html",
  inputs: ["user", "structure"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserPositionsSectionComponent
  extends AbstractSection
  implements OnInit, OnChanges
{
  positionList: UserPosition[];
  userPositions: UserPosition[];
  showUserPositionAssigmentLightbox: boolean = false;
  showUserPositionLightbox: boolean = false;

  get userPositionsUpdated(): boolean {
    if (!this.user.userDetails.positions && this.userPositions.length > 0) {
      return true;
    }
    if (
      this.user.userDetails.positions.map((p) => p.id) !==
      this.userPositions.map((p) => p.id)
    ) {
      return true;
    }
    return false;
  }

  constructor(
    public spinner: SpinnerService,
    protected cdRef: ChangeDetectorRef,
    private userPositionServices: UserPositionServices
  ) {
    super();
  }

  async ngOnInit() {
    this.positionList = await this.userPositionServices.searchUserPositions();
    this.userPositions = this.user.userDetails.positions
      ? [...this.user.userDetails.positions]
      : [];
  }

  ngOnChanges() {}

  protected onUserChange() {
    this.userPositions = this.user.userDetails.positions
      ? [...this.user.userDetails.positions]
      : [];
    this.cdRef.markForCheck();
  }

  deleteUserPosition(position: UserPosition) {
    this.userPositions = this.userPositions.filter((p) => p.id !== position.id);
    this.cdRef.markForCheck();
  }

  openAssignUserPositionModal() {
    this.showUserPositionAssigmentLightbox = true;
  }

  createUserPosition() {
    this.showUserPositionAssigmentLightbox = false;
    this.showUserPositionLightbox = true;
  }

  addUserPositionToList(userPosition: UserPosition) {
    this.userPositions.push(userPosition);
    this.showUserPositionAssigmentLightbox = false;
    this.showUserPositionLightbox = false;
  }

  saveUpdate() {}
}
