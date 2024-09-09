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
  private _searchContent: string;
  @Input() get searchContent(): string {
    return this._searchContent;
  }
  set searchContent(value: string) {
    this._searchContent = value;
    this.searchContentChange.emit(value);
  }
  @Output() searchContentChange: EventEmitter<String> = new EventEmitter();

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
    return !!this.searchContent
      ? item.name
          .toLowerCase()
          .indexOf(this.searchContent.toLowerCase()) >= 0
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
