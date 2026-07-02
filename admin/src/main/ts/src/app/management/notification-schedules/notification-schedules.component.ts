import {
  Component,
  Injector,
  OnDestroy,
  OnInit,
  ViewChild,
} from "@angular/core";
import { NgForm } from "@angular/forms";
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
  @ViewChild("tzForm") private tzForm?: NgForm;

  /* Currently selected structure */
  public structure: StructureModel;
  /* List of available timezones for the user to choose from. */
  public timezones?: SelectOption<string>[];

  /* User input to filter the displayed timezones (frontend filter only). */
  public timezoneInputFilter: string;

  /* Truthy when the lightbox to choose a timezone is visible */
  public showTimezones = false;
  /* Truthy when the lightbox to confirm the changes is visible */
  public showConfirmLightbox = false;

  /* Truthy when the form is unlocked (=when quiet hours are activated) */
  public enabled = false;
  /* The timezone of the selected structure. */
  public timezone?: SelectOption<string>;

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
        }
      }),
    );
  }

  private _resetForm() {
    this.enabled = false;
    this.timezone = { value: "Europe/Paris", label: "Europe/Paris" };
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

  filterByInput = (option: SelectOption<string>) => {
    if (!this.timezoneInputFilter) {
      return true;
    }
    return (
      option.label
        .toLowerCase()
        .indexOf(this.timezoneInputFilter.toLowerCase()) >= 0
    );
  };

  isSelected = (option: SelectOption<string>) => {
    return this.timezone && option && this.timezone.value === option.value;
  };

  onTimezoneSelect = (option: SelectOption<string>) => {
    this.timezone = option;
    this.showTimezones = false;

    this.tzForm?.form.markAsDirty();
    this.tzForm?.form.markAsTouched();
    this.changeDetector.markForCheck();
  };

  public get confirmTitle() {
    return `management.structure.notification.schedules.confirm.${
      this.enabled ? "on" : "off"
    }`;
  }

  private _getQuietHours() {
    this.timezoneService.getStructureQuietHours(this.structure.id).subscribe({
      next: (data) => {
        if (data != null && typeof data === "object" && data.timezone) {
          this.enabled = data.quietHours.enabled;
          const optionToSelect = this.timezones?.find(
            (option) => option.value === data.timezone,
          );
          this.timezone = optionToSelect ?? {
            value: data.timezone,
            label: data.timezone,
          };

          this.changeDetector.detectChanges();
        }
        // ode-mono-select makes tzForm dirty => make it pristine again
        setTimeout(() => {
          this.tzForm?.form.markAsPristine();
          this.tzForm?.form.markAsUntouched();
          this.changeDetector.markForCheck();
        });
      },
    });
  }

  public save() {
    const promise = this.timezoneService
      .setStructureQuietHours(
        this.structure.id,
        this.timezone?.value ?? "Europe/Paris",
        this.enabled,
      )
      .toPromise();
    this.spinner
      .perform("portal-content", promise)
      .then((u) => {
        this.notify.success(
          "management.structure.notification.schedules.save.success.content",
          "management.structure.notification.schedules.save.success.title",
        );

        this.tzForm?.form.markAsPristine();
        this.tzForm?.form.markAsUntouched();
        this.changeDetector.markForCheck();
      })
      .catch((error) => {
        this.notify.error(
          "management.structure.notification.schedules.save.error.content",
          "management.structure.notification.schedules.save.error.title",
        );
      });
  }
}
