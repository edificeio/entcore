import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Injector, Input } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { SpinnerService, trim } from 'ngx-ode-ui';
import { catchError, flatMap, map, tap } from 'rxjs/operators';
import { NotifyService } from 'src/app/core/services/notify.service';
import { GroupModel } from '../../../core/store/models/group.model';
import { GroupsStore } from '../../groups.store';



@Component({
    selector: 'ode-group-create',
    templateUrl: './group-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupCreateComponent extends OdeComponent {
    @Input() label: string;

    newGroup: GroupModel = new GroupModel();
    isBroadcastGroup: boolean = false;

    constructor(private http: HttpClient,
                private groupsStore: GroupsStore,
                private ns: NotifyService,
                private spinner: SpinnerService,
                injector: Injector,
                private location: Location) {
                  super(injector);
    }

    ngOnInit(): void {
      this.label = "create.group.name";

      const groupTypeRoute = "/admin/" + (this.groupsStore.structure ? this.groupsStore.structure.id : "") +
      "/groups/broadcastGroup";

      this.isBroadcastGroup = this.router.isActive(
        groupTypeRoute + "/create",
        true
      );

      if (this.isBroadcastGroup) this.label = "create.broadcastlist.name";
    }

    createNewGroup() {
        this.newGroup.structureId = this.groupsStore.structure.id;

        const body = !this.isBroadcastGroup ? {
          name: this.newGroup.name,
          structureId: this.newGroup.structureId
        } : {
          name: this.newGroup.name,
          structureId: this.newGroup.structureId,
          subType: "BroadcastGroup"
        }
    
        let internalCommunicationRule = this.isBroadcastGroup ? "NONE" : "BOTH"

        this.spinner.perform(
          "portal-content",
          this.http
            .post<GroupModel>("/directory/group", body)
            .pipe(
              flatMap((createdGroup) =>
                this.http
                  .post<{ number: number }>(
                    `/communication/group/${createdGroup.id}?direction=${internalCommunicationRule}`,
                    {}
                  )
                  .pipe(map(() => createdGroup))
              ),
              tap((createdGroup) => {
                this.newGroup.id = createdGroup.id;
                this.newGroup.createdAt = createdGroup.createdAt;
                this.newGroup.createdByName = createdGroup.createdByName;
                this.newGroup.type = "ManualGroup";
                this.newGroup.subType =
                  this.isBroadcastGroup && "BroadcastGroup";
                this.groupsStore.structure.groups.data.push(this.newGroup);
                this.groupsStore.group = this.newGroup;

                this.ns.success(
                  {
                    key: "notify.group.create.content",
                    parameters: { group: this.newGroup.name },
                  },
                  "notify.group.create.title"
                );

                this.router.navigate(["..", createdGroup.id, "details"], {
                  relativeTo: this.route,
                  replaceUrl: false,
                });
              }),
              catchError((err) => {
                this.ns.error(
                  {
                    key: "notify.group.create.error.content",
                    parameters: { group: this.newGroup.name },
                  },
                  "notify.group.create.error.title",
                  err
                );
                throw err;
              })
            )
            .toPromise()
        );
    }

    cancel() {
        this.location.back();
    }

    onGroupNameBlur(name: string): void {
        this.newGroup.name = trim(name);
    }
}
