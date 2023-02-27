import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from 'src/app/core/services/routing.service';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { StructureInformationsService } from './structure-informations.service';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Session } from 'src/app/core/store/mappings/session';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { BundlesService } from 'ngx-ode-sijil';
import { Context } from 'src/app/core/store/mappings/context';

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

export class DuplicationSettings {
  applications: boolean = false;
  widgets: boolean = false;
  distribution: boolean = false;
  mobileapp: boolean = false;
  education: boolean = false;
  uaiList: string = "";

  uaiListRegex: RegExp = /^[0-9]{7}[a-zA-Z]([\r\n,;][0-9]{7}[a-zA-Z])*$/;
  canDuplicateSettings(): boolean
  {
    return this.uaiList && this.uaiListRegex.test(this.uaiList) &&
      (this.applications || this.distribution || this.education ||
      this.mobileapp || this.widgets);
  }

  lightboxTitle: string;
  lightboxMessage: string;
  lightboxList: string[] = [];
  lightboxCanValidate: boolean = false;
  structures: StructureModel[];
}

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
  public structEnableMFA: boolean;
  public withMfa: boolean = false;
  public labelEnableMFA: string;
  public initialStructEnableMFA: boolean; // Used to detect and warn changes on MFA management
  public showMfaWarningLightbox = false;
  public isADMC: boolean = false;
  public showSettingsLightbox = false;

  public metrics: StructureMetrics = new StructureMetrics();
  public settings: DuplicationSettings = new DuplicationSettings();

  constructor(injector: Injector,
    private infoService: StructureInformationsService,
    private notify: NotifyService,
    private bundles: BundlesService)
  {
    super(injector);
  }

  async admcSpecific() {
    const session: Session = await SessionModel.getSession();
    this.isADMC = session.isADMC();
    const context: Context = await SessionModel.getContext();
    if( context && context.mfaConfig && context.mfaConfig.length>0 ) {
      this.withMfa = true;
      this.labelEnableMFA = this.bundles.translate("management.structure.informations.enableMFA", {type: 'SMS'});
    }
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
        this.structEnableMFA = this.initialStructEnableMFA = !this.structure.ignoreMFA;
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
                  case "Guest": continue;
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

  checkThenUpdate(): void {
    if( this.structEnableMFA !== this.initialStructEnableMFA ) {
      this.showMfaWarningLightbox = true;
    } else {
      this.updateStructure();
    }
  }

  confirmMfa(): void {
    this.initialStructEnableMFA = this.structEnableMFA;
    this.showMfaWarningLightbox = false;
    this.checkThenUpdate();
  }

  updateStructure(): void
  {
    this.infoService.update(this.structure.id, this.structName, this.structUAI, this.structHasApp, !this.structEnableMFA).subscribe(
      {
        next: (data) =>
        {
          this.structure.manualName = this.structName != this.structure.name;
          this.structure.name = this.structName;
          this.structure.UAI = this.structUAI;
          this.structure.hasApp = this.structHasApp;
          this.structure.ignoreMFA = !this.structEnableMFA;
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

  /* detachParent(parentId: string): void
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
  } */

  duplicateSettings(): void
  {
    let uaiArray: string[] = this.settings.uaiList.match(/[0-9]{7}[a-zA-Z]/g);
    let uaiSize = uaiArray.length;
    if (uaiSize != 0) {
      this.infoService.checkUAIs(uaiArray).subscribe(
        {
          next: (data) =>
          {
            if (data.length != uaiSize) // means there are non-existent UAIs on the list
            {
              let uais = data.map(struc => struc.UAI);
              this.settings.lightboxTitle = this.bundles.translate("management.structure.informations.duplication.setting.lightbox.error.title");
              this.settings.lightboxList = uaiArray.filter(uai => !uais.includes(uai.toUpperCase()));
              this.settings.lightboxMessage = this.bundles.translate("management.structure.informations.duplication.setting.lightbox.error.message");
              this.settings.lightboxCanValidate = false;
            }
            else
            {
              this.settings.lightboxTitle = this.bundles.translate("management.structure.informations.duplication.setting.lightbox.title");
              this.settings.lightboxMessage = this.bundles.translate("management.structure.informations.duplication.setting.lightbox.message", {name: this.structure.name});
              this.settings.lightboxList = data.map(struc => `${struc.UAI} - ${struc.name}`);
              this.settings.structures = data as StructureModel[];
              this.settings.lightboxCanValidate = true;
            }
            this.showSettingsLightbox = true;
            this.changeDetector.markForCheck();
          },
          error: (error) =>
          {
            this.notify.error("management.structure.informations.duplication.setting.uai.notify.error.content", "management.structure.informations.duplication.setting.uai.notify.error.title");
          }
        }
      )
    }
  }

  launchDuplication(): void
  {
    this.infoService.duplicate(this.structure, this.settings).subscribe({next: (data) => {}});
    this.notify.notify("management.structure.informations.duplication.setting.notify.content", "management.structure.informations.duplication.setting.notify.title", null, "success", { timeout: 5000 });
    this.showSettingsLightbox = false;
  }

  closeLightbox(): void
  {
    this.showSettingsLightbox = false;
    this.changeDetector.markForCheck();
  }
}
