import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from 'src/app/core/services/routing.service';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { StructureGarService } from './structure-gar.service';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Session } from 'src/app/core/store/mappings/session';
import { SessionModel } from 'src/app/core/store/models/session.model';
import { BundlesService } from 'ngx-ode-sijil';
import { Context } from 'src/app/core/store/mappings/context';
import { SelectOption } from 'ngx-ode-ui';

export class GarSettings {
  garOptionSelected: string = null;

  canApplyGar(): boolean
  {
    return this.garOptionSelected != null;
  }

  lightboxTitle: string;
  lightboxMessage: string;
  lightboxList: string[] = [];
  lightboxCanValidate: boolean = false;
  structures: StructureModel[];
}

@Component(
{
  selector: 'ode-structure-gar',
  templateUrl: './structure-gar.component.html',
  styleUrls: ['./structure-gar.component.scss']
})

export class StructureGarComponent extends OdeComponent implements OnInit, OnDestroy
{
  public structure: StructureModel;
  public isADMC: boolean = false;
  public showConfirmLightbox = false;

  public garSettings: GarSettings = new GarSettings();
  public garId: string;
  public garLabel: string;
  public structHasGAR: boolean = false;
  public garOptions: Array<SelectOption<String>> = [];
  public garMap: Map<string, string> = new Map<string, string>();


  constructor(injector: Injector,
    private infoService: StructureGarService,
    private notify: NotifyService,
    private bundles: BundlesService)
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
        this.structHasGAR = (this.structure.exports || []).filter(element => element.includes("GAR-")).length != 0;
        this.infoService.getGarConfig().subscribe(
          {
            next: (data) =>
            {
              if (data != null && data.length > 0) {
                this.garOptions = new Array<SelectOption<String>>(data.length);
                for(let i = data.length; i-- > 0;)
                {
                  this.garOptions[i] = { label: data[i].label, value: data[i].value, };
                  this.garMap.set(data[i].value, data[i].label);
                }
                if (this.structHasGAR) {
                  this.garId = this.structure.exports.filter(element => element.includes("GAR-"))[0].substring(4);
                  this.garLabel = this.garMap.get(this.garId);
                }
              }
              this.changeDetector.markForCheck();
            }
          }
        );
      }
    }));
    this.admcSpecific();
  }

  check(): void
  {
    if (this.structure.UAI != null) {
      let uaiArray: string[] = new Array(this.structure.UAI);
      let uaiSize = uaiArray.length;

      this.infoService.checkGAR(uaiArray).subscribe(
        {
          next: (data) =>
          {
            if (data.length != uaiSize) // means there are non-existent UAIs on the list
            {
              let uais = data.map(struc => struc.UAI);
              this.garSettings.lightboxTitle = this.bundles.translate("management.structure.gar.lightbox.confirm.error.title");
              this.garSettings.lightboxList = uaiArray.filter(uai => !uais.includes(uai.toUpperCase()));
              this.garSettings.lightboxMessage = this.bundles.translate("management.structure.gar.lightbox.confirm.error.message");
              this.garSettings.lightboxCanValidate = false;
            }
            else
            {
              this.garSettings.lightboxTitle = this.bundles.translate("management.structure.gar.lightbox.confirm.title");
              this.garSettings.lightboxMessage = this.bundles.translate("management.structure.gar.lightbox.confirm.message", {label: this.garMap.get(this.garSettings.garOptionSelected), id: this.garSettings.garOptionSelected});
              this.garSettings.lightboxList = data.map(struc => `${struc.UAI} - ${struc.name}`);
              this.garSettings.structures = data as StructureModel[];
              this.garSettings.lightboxCanValidate = true;
            }
            this.showConfirmLightbox = true;
            this.changeDetector.markForCheck();
          },
          error: (error) =>
          {
            this.notify.error("management.structure.gar.error.uai.notify.content", "management.structure.gar.error.notify.title");
          }
        }
      );
    } else {
       this.notify.error("management.structure.gar.error.uai.notify.notfound", "management.structure.gar.error.notify.title");
    }
  }

  closeLightbox(): void
  {
    this.showConfirmLightbox = false;
    this.changeDetector.markForCheck();
  }

  applyGar(): void
  {
    this.showConfirmLightbox = false;
    let targetUAI = this.garSettings.structures.map(struc => struc.UAI);
    this.infoService.applyGAR(targetUAI, this.garSettings.garOptionSelected).subscribe(
      {
        next: (data) =>
        {
          let errors = data.errors;
          if (!errors.includes(this.structure.UAI)) {
            this.structure.exports = ["GAR-" + this.garSettings.garOptionSelected];
            this.garId = this.garSettings.garOptionSelected;
            this.garLabel = this.garMap.get(this.garId);
            this.structHasGAR = true;
          }

          if (errors.length == targetUAI.length) {
            this.notify.notify("management.structure.gar.global.error.notify.content", "management.structure.gar.error.notify.title", null, "error", { timeout: 5000 });
          } else {
            this.notify.notify("management.structure.gar.success.notify.content", "management.structure.gar.success.notify.title", null, "success", { timeout: 5000 });
          }
          this.changeDetector.markForCheck();
        },
        error: (error) =>
        {
          this.notify.error("management.structure.gar.global.error.notify.content", "management.structure.gar.error.notify.title");
        }
      });
  }
}
