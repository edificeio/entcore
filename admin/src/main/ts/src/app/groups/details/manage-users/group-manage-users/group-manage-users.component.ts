import {
  Component,
  EventEmitter,
  Injector,
  OnInit,
  Output,
  ViewChild,
  ElementRef,
  OnChanges,
  SimpleChanges,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { Subscription } from "rxjs";
import { UserModel } from "../../../../core/store/models/user.model";
import { GroupsStore } from "../../../groups.store";
import { SpinnerService } from "ngx-ode-ui";
import { NotifyService } from "src/app/core/services/notify.service";
import { GroupInputUsersComponent } from "../input/group-input-users/group-input-users.component";
import { GroupOutputUsersComponent } from "../output/group-output-users/group-output-users.component";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { globalStore } from "src/app/core/store/global.store";
import { Session } from "src/app/core/store/mappings/session";
import { SessionModel } from "src/app/core/store/models/session.model";
import { UserDetailsModel } from "src/app/core/store/models/userdetails.model";

@Component({
  selector: "ode-group-manage-users",
  templateUrl: "./group-manage-users.component.html",
  styleUrls: ["./group-manage-users.component.scss"],
})
export class GroupManageUsersComponent
  extends OdeComponent
  implements OnInit, OnChanges
{
  @Output()
  closeEmitter: EventEmitter<void> = new EventEmitter<void>();

  @ViewChild(GroupInputUsersComponent, { static: false })
  groupInputUsersComponent: GroupInputUsersComponent;
  @ViewChild(GroupOutputUsersComponent, { static: false })
  groupOutputUsersComponent: GroupOutputUsersComponent;

  inputUsers: UserModel[];

  inputUsersSelected: UserModel[];
  outputUsersSelected: UserModel[];

  structure: StructureModel;
  structures: StructureModel[] = [];

  structureFilter: string = "";
  isDropdownOpened: boolean = false;
  isDropdownVisible: boolean = false;

  isADML: boolean = false;
  hasStructures: boolean = false;

  tabs = [
    {
      label: "Rechercher dans un établissement",
      active: 1,
    },
    {
      label: "Rechercher dans toutes les structures",
      active: 2,
    },
  ];

  selectedTab: number = 1;
  isLoading: boolean = false;

  private groupSubscriber: Subscription;

  constructor(
    public groupsStore: GroupsStore,
    private ns: NotifyService,
    private spinner: SpinnerService,
    private notifyService: NotifyService,
    injector: Injector
  ) {
    super(injector);
  }

  ngOnInit(): void {
    super.ngOnInit();

    this.structure = this.groupsStore.structure;
    this.structures = globalStore.structures.data;

    if (
      this.groupsStore.structure.users.data &&
      this.groupsStore.structure.users.data.length < 1
    ) {
      this.isLoading = true;
      this.groupsStore.structure.users.sync().then(() => {
        this.populateInputUsers(this.groupsStore.structure.users.data);
        this.isLoading = false;
      });
    } else {
      this.populateInputUsers(this.groupsStore.structure.users.data);
    }

    this.groupSubscriber = this.route.params.subscribe(params => {
      if (params.groupId) {
        this.populateInputUsers(this.groupsStore.structure.users.data);
      }
    });

    this.setContext();
  }

  ngOnChanges(changes: SimpleChanges) {
    super.ngOnChanges(changes);
    if (changes["model"]) {
      if (this.structure) {
        this.structureChange(this.structure);
      }
    }
  }

  isSelectedTab(tab) {
    this.selectedTab = tab.active;

    if (this.selectedTab === 1) {
      this.populateInputUsers(this.structure.users.data);
      this.isDropdownVisible = true;
    }

    if (this.selectedTab === 2) {
      this.isDropdownVisible = false;
    }
  }

  setContext = async () => {
    const session: Session = await SessionModel.getSession();

    if (session.functions && session.functions["ADMIN_LOCAL"]) {
      const { code, scope } = session.functions["ADMIN_LOCAL"];

      this.hasStructures = scope && scope.length > 1;
      this.isADML = code === "ADMIN_LOCAL";
    } else {
      return false;
    }

    if (this.isADML && this.hasStructures) {
      this.isDropdownVisible = !this.isDropdownVisible;
    }
  };

  addUsers(): void {
    this.spinner.perform(
      "group-manage-users",
      this.groupsStore.group
        .addUsers(this.inputUsersSelected)
        .then(() => {
          this.groupsStore.group.users = this.groupsStore.group.users.concat(
            this.inputUsersSelected
          );
          this.inputUsers = this.inputUsers.filter(
            u =>
              this.inputUsersSelected.filter(sel => sel.id == u.id).length == 0
          );
          this.inputUsersSelected = [];
          this.groupInputUsersComponent.selectedUsers = [];
          this.notifyService.success("notify.group.manage.users.added.content");
          this.changeDetector.markForCheck();
        })
        .catch(err => {
          this.notifyService.error(
            "notify.group.manage.users.added.error.content",
            "notify.group.manage.users.added.error.title",
            err
          );
        })
    );
  }

  removeUsers(): void {
    this.spinner.perform(
      "group-manage-users",
      this.groupsStore.group
        .removeUsers(this.outputUsersSelected)
        .then(() => {
          this.groupsStore.group.users = this.groupsStore.group.users.filter(
            gu => this.outputUsersSelected.indexOf(gu) === -1
          );
          this.inputUsers = this.inputUsers.concat(this.outputUsersSelected);
          this.outputUsersSelected = [];
          this.groupOutputUsersComponent.selectedUsers = [];
          this.notifyService.success(
            "notify.group.manage.users.removed.content"
          );
          this.changeDetector.markForCheck();
        })
        .catch(err => {
          this.notifyService.error(
            "notify.group.manage.users.removed.error.content",
            "notify.group.manage.users.removed.error.title",
            err
          );
        })
    );
  }

  onInputSelectUsers(users: UserModel[]): void {
    this.inputUsersSelected = users;
  }

  onOutputSelectUsers(users: UserModel[]): void {
    this.outputUsersSelected = users;
  }

  populateInputUsers(groupStructure): void {
    this.inputUsers = this.filterUsers(
      groupStructure,
      this.groupsStore.group.users
    );
    this.changeDetector.markForCheck();
  }

  private filterUsers(sUsers: UserModel[], gUsers: UserModel[]): UserModel[] {
    return sUsers.filter(u => gUsers.map(x => x.id).indexOf(u.id) === -1);
  }

  isAddUsersButtonDisabled(): boolean {
    return (
      !this.inputUsersSelected ||
      (this.inputUsersSelected && this.inputUsersSelected.length === 0)
    );
  }

  isRemoveUsersButtonDisabled(): boolean {
    return (
      !this.outputUsersSelected ||
      (this.outputUsersSelected && this.outputUsersSelected.length === 0)
    );
  }

  onDropdown(): void {
    this.isDropdownOpened = !this.isDropdownOpened;
    if (this.isDropdownOpened) this.structureFilter = "";
    this.changeDetector.markForCheck();
  }

  structureChange(s: StructureModel): void {
    const selectedStructure: StructureModel = globalStore.structures.data.find(
      globalS => globalS.id === s.id
    );
    this.structure = selectedStructure;

    if (
      selectedStructure.users &&
      selectedStructure.users.data &&
      selectedStructure.users.data.length < 1
    ) {
      this.spinner.perform(
        "group-manage-users",
        selectedStructure.users
          .sync()
          .then(() => {
            this.inputUsers = selectedStructure.users.data.filter(
              u =>
                this.groupsStore.group.users.map(x => x.id).indexOf(u.id) === -1
            );
            this.isDropdownOpened = false;
            this.changeDetector.markForCheck();
          })
          .catch(err => {
            this.ns.error(
              {
                key: "notify.structure.syncusers.error.content",
                parameters: { structure: s.name },
              },
              "notify.structure.syncusers.error.title",
              err
            );
          })
      );
    } else {
      this.inputUsers = selectedStructure.users.data.filter(
        u => this.groupsStore.group.users.map(x => x.id).indexOf(u.id) === -1
      );
      this.isDropdownOpened = false;
      this.changeDetector.markForCheck();
    }
  }

  filterByInput = (structure: StructureModel): boolean => {
    return !!this.structureFilter
      ? structure.name
          .toLowerCase()
          .indexOf(this.structureFilter.toLowerCase()) >= 0
      : true;
  };
}
