import { Component, Injector, OnDestroy, OnInit } from "@angular/core";
import { Data } from "@angular/router";
import { OdeComponent } from "ngx-ode-core";
import { SelectOption, SpinnerService } from "ngx-ode-ui";
import { NotifyService } from "src/app/core/services/notify.service";
import { routing } from "src/app/core/services/routing.service";
import { TimezoneService } from "src/app/core/services/timezone.service";
import { StructureModel } from "src/app/core/store/models/structure.model";

@Component({
  selector: "ode-notification-schedules",
  templateUrl: "./notification-schedules.component.html",
  styleUrls: ["./notification-schedules.component.scss"],
})
export class NotificationSchedulesComponent
  extends OdeComponent
  implements OnInit, OnDestroy
{
  public structure: StructureModel;
  public timezones: SelectOption<String>[] = [
    {
      value: "Europe/Paris",
      label: "Europe/Paris (GMT+1)",
    },
  ];
  public showConfirmLightbox = false;

  public activated = false;
  public timezone = this.timezones[0].value;

  constructor(
    injector: Injector,
    public spinner: SpinnerService,
    private notify: NotifyService,
    private timezoneService: TimezoneService,
  ) {
    super(injector);
  }

  ngOnInit(): void {
    this.subscriptions.add(
      routing.observe(this.route, "data").subscribe((data: Data) => {
        if (data.structure) {
          this.structure = data.structure;
          this._getTimezones();
        }
      }),
    );
  }

  private _getTimezones(): void {
    this.timezones = this.timezoneService.getAvailableTimezones().map(
      (tz) =>
        ({
          value: tz,
          label: tz,
        }) as SelectOption<string>,
    );
    //this.changeDetector.markForCheck();
  }

  updateHours() {
    const promise = this.timezoneService
      .setStructureUsersTzAndQuietHours(this.structure.id)
      .toPromise();
    this.spinner
      .perform("portal-content", promise)
      .then((u) => {
        this.notify.success(
          "management.structure.informations.attach.parent.success.content",
          "management.structure.informations.attach.parent.success.title",
        );
      })
      .catch((error) => {
        this.notify.notify(
          "management.structure.informations.attach.parent.error.content",
          "management.structure.informations.attach.parent.error.title",
          error.statusText,
          "error",
        );
      });
  }
}
