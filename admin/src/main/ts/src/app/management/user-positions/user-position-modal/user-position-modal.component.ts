import {
  AfterViewInit,
  Component,
  ElementRef,
  Inject,
  Injector,
  Input,
  ViewChild,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { UserPosition } from "src/app/core/store/models/userPosition.model";
import { UserPositionServices } from "src/app/core/services/user-position.service";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";

@Component({
  selector: "ode-user-position-modal",
  templateUrl: "./user-position-modal.component.html",
  styleUrls: ["./user-position-modal.component.scss"],
})
export class UserPositionModalComponent extends OdeComponent {
  @ViewChild("userPositionNameInput") userPositionNameInputElement: ElementRef;

  public structureId: string;
  public userPosition: UserPosition;
  public editableName: string = "";

  constructor(
    injector: Injector,
    @Inject(MAT_DIALOG_DATA)
    public data: { userPosition: UserPosition; structureId: string },
    private dialogRef: MatDialogRef<UserPositionModalComponent>,
    private userPositionServices: UserPositionServices
  ) {
    super(injector);
    if (data.userPosition) {
      this.userPosition = { ...data.userPosition };
      this.editableName = this.userPosition.name;
    } else {
      this.userPosition = {name: "", source: "MANUAL"};
    }
    this.structureId = data.structureId;
  }

  async save(): Promise<void> {
    this.userPosition.name = this.editableName;
    
    if (this.userPosition.id) {
      this.userPosition = await this.userPositionServices.updateUserPosition(
        this.userPosition
      );
    } else {
      this.userPosition = await this.userPositionServices.createUserPosition({
        name: this.userPosition.name,
        
        structureId: this.structureId,
      });
    }
    this.dialogRef.close(this.userPosition);
  }

  cancel() {
    this.dialogRef.close();
  }
}
