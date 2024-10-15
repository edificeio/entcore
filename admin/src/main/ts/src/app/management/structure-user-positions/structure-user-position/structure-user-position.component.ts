import {
  Component,
  EventEmitter,
  Injector,
  Input,
  Output,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { UserPosition } from "src/app/core/store/models/userPosition.model";
import { UserPositionService } from "src/app/core/services/user-position.service";
import { MatDialog } from "@angular/material/dialog";
import { BundlesService } from "ngx-ode-sijil";
import { NotifyService } from "src/app/core/services/notify.service";

@Component({
  selector: "ode-structure-user-position",
  templateUrl: "./structure-user-position.component.html",
  styleUrls: ["./structure-user-position.component.scss"],
})
export class StructureUserPositionComponent extends OdeComponent {
  @Input() structureId: string;
  @Input() userPosition: UserPosition;
  
  @Output() userPositionUpdated: EventEmitter<UserPosition> = new EventEmitter();
  @Output() userPositionDeleted: EventEmitter<UserPosition> = new EventEmitter();

  get editable(): boolean {
    return this.userPosition.source !== "AAF";
  }

  get sourceLabel(): string {
    return this.bundles.translate(`user-position.source.${this.userPosition.source}`);
  }

  public showUserPositionLightbox: boolean = false;

  constructor(
    injector: Injector,
    private userPositionServices: UserPositionService,
    private ns: NotifyService,
    private bundles: BundlesService,
    public dialog: MatDialog
  ) {
    super(injector);
  }

  edit() {
    this.showUserPositionLightbox = true;
  }
  
  onCloseEdit(userPosition: UserPosition) {
    if (userPosition) {
      this.userPosition = userPosition;
      this.userPositionUpdated.emit(userPosition);
    }
    this.showUserPositionLightbox = false;
    this.changeDetector.markForCheck();
  }

  delete(): void {
    const userPosition = this.userPosition;
    this.userPositionServices.deleteUserPosition(userPosition.id)
      .then( () => {
        this.ns.success(
          "notify.user-position.delete.success.content",
          "notify.user-position.success.title"
        );
        this.userPositionDeleted.emit(userPosition);
        this.changeDetector.markForCheck();
      })
      .catch(err => {
        this.ns.error(
          {
              key: 'notify.user-position.delete.error.content',
              parameters: {
                position: userPosition.name
              }
          }, 'notify.user-position.delete.error.title', err);
      });
  }
}
