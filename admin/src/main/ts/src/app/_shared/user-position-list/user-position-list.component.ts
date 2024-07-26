import {
  Component,
  EventEmitter,
  Injector,
  Input,
  Output,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { UserPosition } from "src/app/core/store/models/userPosition.model";

@Component({
  selector: "ode-user-position-list",
  templateUrl: "./user-position-list.component.html",
  styleUrls: ["./user-position-list.component.scss"],
})
export class UserPositionListComponent extends OdeComponent {
  @Input() userPositionList: UserPosition[] = [];
  private _selectedUserPosition: UserPosition;
  @Input() get selectedUserPosition(): UserPosition {
    return this._selectedUserPosition;
  }
  set selectedUserPosition(value: UserPosition) {
      this._selectedUserPosition = value;
      this.selectedUserPositionChange.emit(value);
      this.changeDetector.markForCheck();
  }
  @Output() selectedUserPositionChange: EventEmitter<UserPosition> =
    new EventEmitter();

  public searchPositionPrefix: string;

  constructor(injector: Injector) {
    super(injector);
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

  selectUserPosition = (userPosition: UserPosition) => {
    this.selectedUserPosition = userPosition;
    this.changeDetector.markForCheck();
  };
}
