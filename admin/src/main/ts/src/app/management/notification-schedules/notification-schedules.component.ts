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
  public timezones: SelectOption<string>[] = [
    {
      value: "Europe/Paris",
      label: "Europe/Paris (GMT+1)",
    },
  ];
  public showConfirmLightbox = false;

  public enabled = false;
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
          this._resetForm();
          this._getTimezones();
          this._getQuietHours();
          this.changeDetector.markForCheck();
        }
      }),
    );
  }

  private _resetForm() {
    this.enabled = false;
    this.timezone = "Europe/Paris";
  }

  private _getTimezones(): void {
    this.timezones = this.timezoneService.getAvailableTimezones().map(
      (tz) =>
        ({
          value: tz,
          label: tz,
        }) as SelectOption<string>,
    );
  }

  private _getQuietHours() {
    this.timezoneService.getStructureQuietHours(this.structure.id).subscribe({
      next: (data) => {
        if (data != null && typeof data === "object" && data.timezone) {
          this.enabled = data.quietHours.enabled;
          this.timezone = data.timezone;
        }
        this.changeDetector.markForCheck();
      },
    });
  }

  public save() {
    const promise = this.timezoneService
      .setStructureQuietHours(this.structure.id, this.timezone, this.enabled)
      .toPromise();
    this.spinner
      .perform("portal-content", promise)
      .then((u) => {
        this.notify.success(
          "management.structure.notification.schedules.save.success.content",
          "management.structure.notification.schedules.save.success.title",
        );
      })
      .catch((error) => {
        this.notify.error(
          "management.structure.notification.schedules.save.error.content",
          "management.structure.notification.schedules.save.error.title",
        );
      });
  }
}
