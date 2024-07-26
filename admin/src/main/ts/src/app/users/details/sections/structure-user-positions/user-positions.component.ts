import { Component, Injector, OnDestroy, OnInit } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { Session } from "src/app/core/store/mappings/session";
import { SessionModel } from "src/app/core/store/models/session.model";
import { UserPosition } from "src/app/core/store/models/userPosition.model";
import { routing } from "src/app/core/services/routing.service";
import { Data } from "@angular/router";
import { UserPositionServices } from "src/app/core/services/user-position.service";
import { Location } from "@angular/common";
import { MatDialog } from "@angular/material/dialog";
import { UserPositionModalComponent } from "src/app/_shared/user-position-modal/user-position-modal.component";

@Component({
  selector: "ode-user-positions",
  templateUrl: "./user-positions.component.html",
  styleUrls: ["./user-positions.component.scss"],
})
export class StructureUserPositionsComponent
  extends OdeComponent
  implements OnInit, OnDestroy
{
  public structure: StructureModel;
  public isADMC: boolean = false;
  public showConfirmLightbox = false;
  public userPositionList: UserPosition[] = [];
  public searchPositionPrefix: string;
  private _selectedUserPosition: UserPosition;
  public get selectedUserPosition(): UserPosition{
    return this._selectedUserPosition;
  }
  set selectedUserPosition(value: UserPosition) {
    if (value != this._selectedUserPosition) {
      this._selectedUserPosition = value;
      this.openUserPositionDetails(value);
    }
    this.changeDetector.markForCheck();
  }

  constructor(
    injector: Injector,
    private location: Location,
    private userPositionServices: UserPositionServices,
    public dialog: MatDialog
  ) {
    super(injector);
  }

  async admcSpecific() {
    const session: Session = await SessionModel.getSession();
    this.isADMC = session.isADMC();
    this.changeDetector.markForCheck();
  }

  async ngOnInit(): Promise<void> {
    this.subscriptions.add(
      routing.observe(this.route, "data").subscribe((data: Data) => {
        if (data.structure) {
          this.structure = data.structure;
          this.userPositionServices
            .searchUserPositions({
              structureIds: [this.structure.id],
            })
            .then((userPositions) => {
              this.userPositionList = userPositions;
              if (this.router.url.endsWith("/create")) {
                this.createUserPosition();
              } else {
                this.route.params.subscribe((params) => {
                  let userPostionId = params["userPositionId"];
                  if (userPostionId) {
                    const userPosition = this.userPositionList.find(
                      (userPosition) => userPosition.id === userPostionId
                    );
                    this.userPositionList.find(
                      (userPosition) => userPosition.id === userPostionId
                    );
                    if (userPosition) {
                      this.selectedUserPosition = userPosition;
                    } else {
                      console.error("User position not found");
                    }
                    this.changeDetector.markForCheck();
                  }
                });
              }
              this.changeDetector.markForCheck();
            });
        }
      })
    );

    this.admcSpecific();
  }

  filterByInput = (item: any): boolean => {
    return !!this.searchPositionPrefix
      ? item.name
          .toLowerCase()
          .indexOf(this.searchPositionPrefix.toLowerCase()) >= 0
      : true;
  };

  isSelected = (userPosition: UserPosition) => {
    return (
      this.selectedUserPosition &&
      userPosition &&
      this.selectedUserPosition.id === userPosition.id
    );
  };

  openUserPositionDetails = (userPosition: UserPosition) => {
    this.selectedUserPosition = userPosition;
    let url = this.router.url;

    // Get base route for position to navigate after.
    if (!url.endsWith("/positions")) {
      let splitUrl = url.split("/");
      splitUrl = splitUrl.slice(0, splitUrl.length - 1);
      url = splitUrl.join("/");
    }
    this.location.go(`${url}/${userPosition.id}`);
    this.changeDetector.markForCheck();
  };

  createUserPosition() {
    const dialogRef = this.dialog.open(UserPositionModalComponent, {
      width: "70%",
      height: "50%",
      data: { structureId: this.structure._id },
    });

    this.subscriptions.add(
      dialogRef.afterClosed().subscribe((userPosition: UserPosition) => {
        if (userPosition) {
          // TODO : add confirmation toaster
          this.userPositionList.push(userPosition);
          this.selectedUserPosition = userPosition;

          // Redirect to the new user position URL 
          if (this.router.url.endsWith("/create")) {
            const url = this.router.url.replace("/create", "");
            this.location.go(`${url}/${userPosition.id}`);
          }
          this.changeDetector.markForCheck();
        }
      })
    );
    this.changeDetector.markForCheck();
  }

  userPostionUpdated(userPosition: UserPosition) {
    const userPositionUpdatedIndex = this.userPositionList.findIndex(
      (userP) => userP.id === userPosition.id
    );
    this.userPositionList[userPositionUpdatedIndex] = userPosition;
    this.changeDetector.markForCheck();
  }
}
