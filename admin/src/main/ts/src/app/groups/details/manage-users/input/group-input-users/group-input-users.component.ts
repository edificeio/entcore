import {
  Component,
  Injector,
  Input,
  OnDestroy,
  OnInit,
  Output,
  EventEmitter,
  OnChanges,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { SelectOption } from "ngx-ode-ui";
import { UserListService } from "src/app/core/services/userlist.service";
import { UsersService } from "src/app/users/users.service";
import { globalStore } from "src/app/core/store/global.store";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { UserModel } from "src/app/core/store/models/user.model";
import {
  DeleteFilter,
  UserlistFiltersService,
} from "../../../../../core/services/userlist.filters.service";
import { SpinnerService } from "ngx-ode-ui";

@Component({
  selector: "ode-group-input-users",
  templateUrl: "./group-input-users.component.html",
  styleUrls: ["./group-input-users.component.scss"],
  providers: [UserListService],
})
export class GroupInputUsersComponent
  extends OdeComponent
  implements OnInit, OnDestroy, OnChanges
{
  @Input() model: UserModel[] = [];
  @Input() searchInput: boolean = false;
  @Output() selectUsers: EventEmitter<UserModel[]> = new EventEmitter();

  public excludeDeletedUsers: DeleteFilter;

  nbUser: number;
  searchTerm: string;
  // list elements stored by store pipe in list component
  // (takes filters in consideration)
  storedElements: UserModel[] = [];

  // Users selected by enduser
  selectedUsers: UserModel[] = [];

  structure: StructureModel;
  structures: StructureModel[] = [];

  structureOptions: SelectOption<StructureModel>[] = [];

  constructor(
    public userLS: UserListService,
    public listFilters: UserlistFiltersService,
    public usersService: UsersService,
    public spinner: SpinnerService,
    injector: Injector
  ) {
    super(injector);
    this.excludeDeletedUsers = new DeleteFilter(listFilters.$updateSubject);
    this.excludeDeletedUsers.outputModel = ["users.not.deleted"];
  }

  ngOnInit(): void {
    super.ngOnInit();
    
    this.subscriptions.add(
      this.listFilters.$updateSubject.subscribe(() => {
        this.changeDetector.markForCheck();
      })
    );
  }

  ngOnChanges(): void {
    if (this.searchInput) {
      this.model = [];
      this.changeDetector.markForCheck();
    }
  }

  selectUser(u: UserModel): void {
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

  selectAll(): void {
    this.selectedUsers = this.storedElements;
    this.selectUsers.emit(this.selectedUsers);
  }

  deselectAll(): void {
    this.selectedUsers = [];
    this.selectUsers.emit(this.selectedUsers);
  }

  refreshListCount(list): void {
    this.nbUser = list.length;
  }

  search = (): void => {
    console.log(this.userLS.inputFilter);
    
    this.spinner.perform(
      "portal-content",
      this.usersService
        .search(this.userLS.inputFilter)
        .then(data => {
          this.model = data;
          this.refreshListCount(data);
          this.changeDetector.markForCheck();
        })
    );
  };
}
