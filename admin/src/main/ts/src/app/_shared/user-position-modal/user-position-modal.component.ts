import {
  Component,
  ElementRef,
  EventEmitter,
  Injector,
  Input,
  OnInit,
  Output,
  ViewChild,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { UserPosition } from "src/app/core/store/models/userPosition.model";
import { UserPositionServices } from "src/app/core/services/user-position.service";

@Component({
  selector: "ode-user-position-modal",
  templateUrl: "./user-position-modal.component.html",
  styleUrls: ["./user-position-modal.component.scss"],
})
export class UserPositionModalComponent extends OdeComponent implements OnInit {
  @ViewChild("userPositionNameInput") userPositionNameInputElement: ElementRef;

  @Input() structureId: string;

  private _userPosition: UserPosition;
  @Input() get userPosition(): UserPosition {
    return this._userPosition;
  };
  set userPosition(value: UserPosition) {
    this._userPosition = value;
    this.editableName = value.name;
  }
  @Input() showUserPositionLightbox: boolean = true;

  @Output() onClose: EventEmitter<UserPosition> = new EventEmitter();
  
  public editableName: string = "";

  get isUpdateModal(): boolean {
    return !!this.userPosition.id;
  }
  
  constructor(
    injector: Injector,
    private userPositionServices: UserPositionServices
  ) {
    super(injector);
  }

  ngOnInit(): void {
    if (!this.userPosition) {
      this.userPosition = {name:"", source:"MANUAL"};
    }
  }


  async save(): Promise<void> {
    this.userPosition.name = this.editableName;
    
    if (this.isUpdateModal) {
      this.userPosition = await this.userPositionServices.updateUserPosition(
        this.userPosition
      );
    } else {
      this.userPosition = await this.userPositionServices.createUserPosition({
        name: this.userPosition.name,
        structureId: this.structureId,
      });
    }
    this.onClose.emit(this.userPosition);
    this.showUserPositionLightbox = false;
  }
  
  cancel() {
    this.onClose.emit();
    if (!this.isUpdateModal) {
      this.editableName = "";
    }
    this.showUserPositionLightbox = false;
  }
}
