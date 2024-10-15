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
import { UserPositionService } from "src/app/core/services/user-position.service";
import { NotifyService } from "src/app/core/services/notify.service";
import { SpinnerService } from "ngx-ode-ui";

@Component({
  selector: "ode-user-position-modal",
  templateUrl: "./user-position-modal.component.html",
  styleUrls: ["./user-position-modal.component.scss"],
})
export class UserPositionModalComponent extends OdeComponent implements OnInit {
  @ViewChild("userPositionNameInput") userPositionNameInputElement: ElementRef;

  @Input() structureId: string;

  private _userPosition: UserPosition;
  get userPosition(): UserPosition {
    return this._userPosition;
  }
  @Input() set userPosition(value: UserPosition) {
    this._userPosition = value;
    this.editableName = value?.name;
  }
  private _show: boolean = true;
  @Input() get show(): boolean {
    return this._show;
  }
  set show(value: boolean) {
    this._show = value;
    if (value) {
      this.saving = false;
    }
  }

  @Output() onClose: EventEmitter<UserPosition> = new EventEmitter();

  public editableName: string = "";
  public saving: boolean = false;

  get isUpdateModal(): boolean {
    return !!this.userPosition?.id;
  }

  constructor(
    injector: Injector,
    private ns: NotifyService,
    public spinner: SpinnerService,
    private userPositionService: UserPositionService
  ) {
    super(injector);
  }

  ngOnInit(): void {
    this.saving = false;

    if (!this.userPosition) {
      this.userPosition = { name: "", source: "MANUAL" };
    }
  }

  async save(): Promise<void> {
    if (this.saving) return;
    this.saving = true;
    if (this.isUpdateModal) {
      this.userPosition = await this.spinner
        .perform(
          "portal-content",
          this.userPositionService.updateUserPosition({
            ...this.userPosition,
            name: this.editableName,
          })
        )
        .then((updated) => {
          this.ns.success(
            "notify.user-position.update.success.content",
            "notify.user-position.success.title"
          );
          return updated;
        })
        .catch((err) => {
          this.ns.error(
            {
              key: "notify.user-position.update.error.content",
              parameters: {
                position: this.userPosition.name,
              },
            },
            "notify.user-position.update.error.title",
            err
          );
          return this.userPosition;
        });
    } else {
      this.userPosition = await this.spinner
        .perform(
          "portal-content",
          this.userPositionService.createUserPosition({
            name: this.editableName,
            structureId: this.structureId,
          })
        )
        .then((created) => {
          this.ns.success(
            "notify.user-position.create.success.content",
            "notify.user-position.success.title"
          );
          return created;
        })
        .catch((err) => {
          debugger;
          if (
            err.response.status === 409 &&
            err.response?.data?.existingPositionId
          ) {
            this.ns.error(
              {
                key: "notify.user-position.create.error.duplicate.content",
                parameters: {
                  position: this.editableName,
                },
              },
              "notify.user-position.create.error.title",
              err
            );
          } else {
            this.ns.error(
              {
                key: "notify.user-position.create.error.content",
                parameters: {
                  position: this.editableName,
                },
              },
              "notify.user-position.create.error.title",
              err
            );
          }
          return undefined;
        });
    }
    this.onClose.emit(this.userPosition);
    this.userPosition = undefined;
    this.editableName = undefined;
    this.show = false;
  }

  cancel() {
    this.onClose.emit();
    this.userPosition = undefined;
    this.editableName = undefined;
    this.show = false;
    this.saving = false;
  }
}
