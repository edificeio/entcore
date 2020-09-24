import { Component, Injector, OnDestroy, OnInit, SystemJsNgModuleLoader } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from 'src/app/core/services/routing.service';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { StructureInformationsService } from './structure-informations.service';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Session } from 'src/app/core/store/mappings/session';
import { SessionModel } from 'src/app/core/store/models/session.model';

class UserMetric {
  active: number = 0;
  inactive: number = 0;
};

class StructureMetrics {
  students: UserMetric = new UserMetric();
  relatives: UserMetric = new UserMetric();
  teachers: UserMetric = new UserMetric();
  personnels: UserMetric = new UserMetric();
};

@Component(
{
  selector: 'ode-structure-informations',
  templateUrl: './structure-informations.component.html',
  styleUrls: ['./structure-informations.component.scss']
})

export class StructureInformationsComponent extends OdeComponent implements OnInit, OnDestroy
{
  public structure: StructureModel;

  public structName: string;
  public structUAI: string;
  public structHasApp: boolean;
  public isADMC: boolean = false;

  public metrics: StructureMetrics = new StructureMetrics();

  constructor(injector: Injector, private infoService: StructureInformationsService, private notify: NotifyService)
  {
    super(injector);
  }

  async admcSpecific() {
    const session: Session = await SessionModel.getSession();
    this.isADMC = session.isADMC();
    this.changeDetector.markForCheck();
  }

  ngOnInit(): void
  {
    this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) =>
    {
      if (data.structure)
      {
        this.structure = data.structure;
        this.structName = this.structure.name;
        this.structUAI = this.structure.UAI;
        this.structHasApp = this.structure.hasApp;
        console.log(this.structure);
        this.infoService.getMetrics(this.structure.id).subscribe(
          {
            next: (data) =>
            {
              let dmetrics = data.metrics;
              for(let i = dmetrics.length; i-- > 0;)
              {
                let path;
                switch(dmetrics[i].profile)
                {
                  case "Student": path = "students"; break;
                  case "Relative": path = "relatives"; break;
                  case "Teacher": path = "teachers"; break;
                  case "Personnel": path = "personnels"; break;
                }
                this.metrics[path].inactive = dmetrics[i].inactive;
                this.metrics[path].active = dmetrics[i].active;
              }
              this.changeDetector.markForCheck();
            },
            error: (error) =>
            {
              // Fail silently
            }
          }
        );
      }
    }));
    this.admcSpecific();
  }

  updateStructure(): void
  {
    this.infoService.update(this.structure.id, this.structName, this.structUAI, this.structHasApp).subscribe(
      {
        next: (data) =>
        {
          this.structure.manualName = this.structName != this.structure.name;
          this.structure.name = this.structName;
          this.structure.UAI = this.structUAI;
          this.structure.hasApp = this.structHasApp;
          this.notify.success("management.structure.informations.update.notify.success.content", "management.structure.informations.update.notify.success.title");
          this.changeDetector.markForCheck();
        },
        error: (error) =>
        {
          this.notify.notify("management.structure.informations.update.notify.error.content", "management.structure.informations.update.notify.error.title", error, "error");
        }
      });
  }

  resetManualName(): void
  {
    this.infoService.resetManualName(this.structure.id).subscribe(
      {
        next: (data) =>
        {
          this.structure.manualName = false;
          if(this.structure.feederName != null)
          {
            this.structName = this.structure.feederName;
            this.structure.name = this.structure.feederName;
          }
          this.notify.success("management.structure.informations.reset.name.success.content", "management.structure.informations.reset.name.success.title");
          this.changeDetector.markForCheck();
        },
        error: (error) =>
        {
          this.notify.notify("management.structure.informations.update.notify.error.content", "management.structure.informations.update.notify.error.title", error, "error");
        }
      }
    );
  }

  detachParent(parentId: string): void
  {
    this.infoService.detachParent(this.structure.id, parentId).subscribe(
      {
        next: (data) =>
        {
          for(let i = this.structure.parents.length; i-- > 0;)
            if(this.structure.parents[i].id == parentId)
              this.structure.parents.splice(i, 1);

          this.notify.success("management.structure.informations.detach.parent.success.content", "management.structure.informations.detach.parent.success.title");
          this.changeDetector.markForCheck();
        },
        error: (error) =>
        {
          this.notify.notify("management.structure.informations.update.notify.error.content", "management.structure.informations.update.notify.error.title", error, "error");
        }
      }
    );
  }
}
