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
  @Input() public searchPrefix: string;
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

  @Output() newPositionNameProposed: EventEmitter<String> =
  new EventEmitter();

  private previousProposal: string;

  constructor(injector: Injector) {
    super(injector);
  }

  filterByInput = (item: any): boolean => {
    return !!this.searchPrefix
      ? item.name
          .toLowerCase()
          .indexOf(this.searchPrefix.toLowerCase()) >= 0
      : true;
  };

  isSelected = (userPosition: UserPosition) => {
    return (
      this.selectedUserPosition &&
      userPosition &&
      this.selectedUserPosition.name.trim() === userPosition.name.trim()
    );
  };

  selectUserPosition = (userPosition: UserPosition) => {
    this.selectedUserPosition = userPosition;
    this.changeDetector.markForCheck();
  };

  checkEmitNewPositionName(filteredList:[]) {
    const searchPrefix = this.searchPrefix?.trim() ?? "";
    // When filtered list is empty and current search prefix is not,
    if(filteredList.length == 0 && searchPrefix.length > 0) {
        // If the search prefix has not been proposed yet, then do it now.
        if( this.previousProposal !== searchPrefix) {
          this.newPositionNameProposed.emit(searchPrefix);
          this.previousProposal = searchPrefix;
        }
    }
    // If the proposal has not been resetted yet, then do it now.
    else if(this.previousProposal) {
      this.newPositionNameProposed.emit();
      this.previousProposal = searchPrefix;
    }
  }
}
