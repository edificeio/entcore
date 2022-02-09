import {
  Component,
  EventEmitter,
  Injector,
  Input,
  Output,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { BundlesService } from "ngx-ode-sijil";
import { UserListService } from "src/app/core/services/userlist.service";
import { UserModel } from "../../../../../core/store/models/user.model";

@Component({
  selector: "ode-group-output-users",
  templateUrl: "./group-output-users.component.html",
  styleUrls: ["./group-output-users.component.scss"],
  providers: [UserListService],
})
export class GroupOutputUsersComponent extends OdeComponent {
  @Input() model: UserModel[] = [];
  @Input() showUserStructure: boolean = false;
  @Output() selectUsers: EventEmitter<UserModel[]> = new EventEmitter();

  // list elements stored by store pipe in list component
  // (takes filters in consideration)
  storedElements: UserModel[] = [];

  // Users selected by enduser
  selectedUsers: UserModel[] = [];

  allUsersChecked:boolean = false;

  constructor(injector: Injector, public userLS: UserListService, private bundles: BundlesService) {
    super(injector);
  }

  selectUser(u): void {
    if (this.selectedUsers.indexOf(u) === -1) {
      this.selectedUsers.push(u);
    } else {
      this.selectedUsers = this.selectedUsers.filter(su => su.id !== u.id);
    }
    this.selectUsers.emit(this.selectedUsers);
  }

  isSelected = (user: UserModel) => {
    return this.selectedUsers.indexOf(user) > -1;
  };

  userStructures(item: UserModel) {
    if(item.structures && item.structures.length > 0) {
        let result = item.structures[0].name;
        if (item.structures.length > 1) {
            result += ` + ${item.structures.length-1} ${this.bundles.translate("others")}`;
        }
        return result;
    }
    return "";
  }

  toggleSelectedUsers(): void {
    const selectedUsersLength = this.selectedUsers.length;
    const storedElementsLength = this.storedElements.length;

    if (selectedUsersLength !== storedElementsLength) {
      this.selectedUsers = this.storedElements;
      this.allUsersChecked = true;
    } else {
      this.selectedUsers = [];
      this.allUsersChecked = false;
    }
    this.selectUsers.emit(this.selectedUsers);
  }
}
