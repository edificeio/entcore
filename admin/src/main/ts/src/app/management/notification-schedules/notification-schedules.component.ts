import { Component, Injector, OnDestroy, OnInit } from "@angular/core";
import { Data } from "@angular/router";
import { OdeComponent } from "ngx-ode-core";
import { SelectOption } from "ngx-ode-ui";
import { NotifyService } from "src/app/core/services/notify.service";
import { routing } from "src/app/core/services/routing.service";
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
  //Angular hack to access the enum in the HTML
  //  public EDTImportFlux = EDTImportFlux;
  //  public EDTImportMode = EDTImportMode;

  public structure: StructureModel;
  public activated = false;
  public timezones: SelectOption<String>[] = [
    {
      value: "Europe/Paris",
      label: "Europe/Paris (GMT+1)",
    },
  ];
  public timezone = this.timezones[0].value;
  public showConfirmLightbox = false;

  constructor(
    injector: Injector,
    private notify: NotifyService,
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
    /*
    this.timetableService.getClassesMapping(this.structure.id).subscribe({
      next: (data) =>
      {
        this.unknownClasses = data.unknownClasses == null ? [] : data.unknownClasses.sort();

        if(data.classNames != null)
        {
          this.classNames = new Array<SelectOption<String>>(data.classNames.length);

          data.classNames = data.classNames.sort();
          for(let i = this.classNames.length; i-- > 0;)
            this.classNames[i] = { label: data.classNames[i].toString(), value: data.classNames[i], };
        }
        else
          this.classNames = [];

        this.classesMapping = data.classesMapping == null ? {} : data.classesMapping;

        this.changeDetector.markForCheck();
      }
    });
        */
  }

  updateHours() {}
}
