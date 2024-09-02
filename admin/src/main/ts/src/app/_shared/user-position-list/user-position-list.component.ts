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
  private _searchPrefix: string;
  @Input() get searchPrefix(): string {
    return this._searchPrefix;
  }
  set searchPrefix(value: string) {
    this._searchPrefix = value;
    this.searchPrefixChange.emit(value);
  }
  @Output() searchPrefixChange: EventEmitter<String> = new EventEmitter();

  @Input() userPositionList: UserPosition[] = [];
  private _selectedUserPosition: UserPosition;
  @Input() get selectedUserPosition(): UserPosition {
    return this._selectedUserPosition;
  }
  set selectedUserPosition(value: UserPosition) {
    this._selectedUserPosition = value;
    this.selectedUserPositionChange.emit(value);
  }
  @Output() selectedUserPositionChange: EventEmitter<UserPosition> =
    new EventEmitter();

  private _filteredList: UserPosition[] =[];
  @Output() filteredListChange: EventEmitter<UserPosition[]> =
    new EventEmitter();

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
  };

  listChange(filteredList: UserPosition[]) {
    // Fix issue with ode-list component triggering update too often
    if (filteredList.join("") !== this._filteredList.join("")) {
      this._filteredList = filteredList;
      this.filteredListChange.emit(filteredList);
    }
  }
}
