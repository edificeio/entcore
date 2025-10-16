import {Component, Injector, Input} from "@angular/core";
import {OdeComponent} from "ngx-ode-core";
import {NotifyService} from "../../../../core/services/notify.service";
import {GroupModel} from "../../../../core/store/models/group.model";
import {StructureModel} from "../../../../core/store/models/structure.model";
import {GroupsService, GroupUpdatePayload} from "../../../groups.service";
import {HttpErrorResponse} from "@angular/common/http";

class ManualGroupAutolinkFormModel {
  personnelSubSectionRadio: string;
  selectedUsersPositions: Array<string> = [];
}

@Component({
  selector: 'ode-manual-group-autolink',
  templateUrl: './manual-group-autolink.component.html',
  styleUrls: ['./manual-group-autolink.component.scss']
})
export class ManualGroupAutolinkComponent extends OdeComponent {
  @Input()
  group: GroupModel;

  @Input()
  structure: StructureModel;

  @Input()
  usersPositionsOptions: Array<string>;

  @Input()
  showActions: boolean;

  public form: ManualGroupAutolinkFormModel;
  public showFunctionsPicker: boolean;
  public showUsersPositionsPicker: boolean;
  public checked: boolean;

  constructor(
      private readonly notifyService: NotifyService,
      private readonly groupsService: GroupsService,
      injector: Injector
  ) {
    super(injector);
  }

  ngOnInit() {
    super.ngOnInit();
    this.initForm();
  }

  ngOnChanges(): void {
    this.initForm();
  }

  public unselectUsersPositions(item: string): void {
    if (this.showActions) {
      this.form.selectedUsersPositions.splice(this.form.selectedUsersPositions.indexOf(item), 1);
    }
  }

  public handleUsersPositionsClick($event): void {
    if (this.form.personnelSubSectionRadio === 'usersPositions') {
      this.showUsersPositionsPicker = true;
      this.showFunctionsPicker = false;
    } else {
      this.form.selectedUsersPositions = [];
      this.showUsersPositionsPicker = false;
    }
  }

  public onSubmit(): void {
    if (!this.showActions) {
      return;
    }

    const groupUpdatePayload: GroupUpdatePayload = {
      name: this.group.name,
        linkedUserPositions: []
    };

    if (this.form.personnelSubSectionRadio === 'usersPositions' &&
      this.form.selectedUsersPositions &&
      this.form.selectedUsersPositions.length > 0) {
        groupUpdatePayload.linkedUserPositions = this.form.selectedUsersPositions;
    }

      this.groupsService
          .update(this.group.id, groupUpdatePayload)
          .subscribe(
              () => {
                  this.notifyService.success(
                      'group.details.broadcast.autolink.notify.success.content',
                      'group.details.broadcast.autolink.notify.success.title'
                  );
              },
              (error: HttpErrorResponse) => {
                  this.notifyService.error(
                      'group.details.broadcast.autolink.notify.error.content',
                      'group.details.broadcast.autolink.notify.error.title',
                      error
                  );
              }
          );
  }

    private initForm(): void {
        this.form = new ManualGroupAutolinkFormModel();

        this.form.personnelSubSectionRadio = 'functionGroups';

        if (this.group?.linkedUserPositions) {
            for (const p of this.group.linkedUserPositions) {
                if (this.usersPositionsOptions?.includes(p)) {
                    this.form.selectedUsersPositions.push(p);
                    this.form.personnelSubSectionRadio = 'usersPositions';
                    this.showUsersPositionsPicker = true;
                }
            }
        }
    }
}
