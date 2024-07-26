import {
  Component,
  EventEmitter,
  Injector,
  Input,
  Output,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { UserPosition } from "src/app/core/store/models/userPosition.model";
import { UserPositionServices } from "src/app/core/services/user-position.service";
import { MatDialog } from "@angular/material/dialog";
import { UserPositionModalComponent } from "../../../_shared/user-position-modal/user-position-modal.component";

@Component({
  selector: "ode-structure-user-position",
  templateUrl: "./structure-user-position.component.html",
  styleUrls: ["./structure-user-position.component.scss"],
})
export class StructureUserPositionComponent extends OdeComponent {
  @Input() structureId: string;
  @Input() userPosition: UserPosition;
  
  @Output() userPositionUpdated: EventEmitter<UserPosition> = new EventEmitter();

  get editable(): boolean {
    return this.userPosition.source !== "AAF";
  }

  constructor(
    injector: Injector,
    private userPositionServices: UserPositionServices,
    public dialog: MatDialog
  ) {
    super(injector);
  }

  edit() {
    const dialogRef = this.dialog.open(UserPositionModalComponent, {
      width: "70%",
      height: "50%",
      data: { structureId: this.structureId, userPosition: this.userPosition },
    });

    this.subscriptions.add(
      dialogRef.afterClosed().subscribe((userPosition: UserPosition) => {
        // TODO : add confirmation toaster
        if (userPosition) {
          this.userPosition = userPosition;
          this.userPositionUpdated.emit(userPosition);
          this.changeDetector.markForCheck();
        }
      })
    );
  }

  delete(): void {
    // TODO : add confirmation toaster
    this.userPositionServices.deleteUserPosition(this.userPosition.id, this.structureId);
  }
}
