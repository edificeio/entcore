import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { GarService } from './gar.service';
import { NotifyService } from 'src/app/core/services/notify.service';
import { BundlesService } from 'ngx-ode-sijil';
import { SelectOption } from 'ngx-ode-ui';
import { globalStore } from 'src/app/core/store/global.store';

export class GarSettings {
  uaiList: string = "";
  garOptionSelected: string = null;

  uaiListRegex: RegExp = /^[0-9]{7}[a-zA-Z]([\r\n,;][0-9]{7}[a-zA-Z])*$/;
  canApplyGar(): boolean
  {
    return this.uaiList && this.uaiListRegex.test(this.uaiList) &&
      this.garOptionSelected != null;
  }

  lightboxTitle: string;
  lightboxMessage: string;
  lightboxList: string[] = [];
  lightboxCanValidate: boolean = false;
  structures: StructureModel[];
}

@Component(
{
  selector: 'ode-gar',
  templateUrl: './gar.component.html',
  styleUrls: ['./gar.component.scss']
})

export class GarComponent extends OdeComponent implements OnInit, OnDestroy
{
  public showConfirmLightbox = false;
  public showErrorLightbox = false;

  public garSettings: GarSettings = new GarSettings();
  public garOptions: Array<SelectOption<String>> = [];
  public garMap: Map<string, string> = new Map<string, string>();


  constructor(injector: Injector,
    private infoService: GarService,
    private notify: NotifyService,
    private bundles: BundlesService)
  {
    super(injector);
  }

  ngOnInit(): void
  {
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
          }
          this.changeDetector.markForCheck();
        }
      }
    );
  }

  check(): void
  {
    let uaiArray: string[] = this.garSettings.uaiList.match(/[0-9]{7}[a-zA-Z]/g);
    let uaiSize = uaiArray.length;
    if (uaiSize != 0) {
      this.infoService.checkGAR(uaiArray).subscribe(
        {
          next: (data) =>
          {
            if (data.length != uaiSize) // means there are non-existent UAIs on the list
            {
              let uais = data.map(struc => struc.UAI);
              this.garSettings.lightboxTitle = this.bundles.translate("admc.misc.gar.lightbox.confirm.error.title");
              this.garSettings.lightboxList = uaiArray.filter(uai => !uais.includes(uai.toUpperCase()));
              this.garSettings.lightboxMessage = this.bundles.translate("admc.misc.gar.lightbox.confirm.error.message");
              this.garSettings.lightboxCanValidate = false;
            }
            else
            {
              this.garSettings.lightboxTitle = this.bundles.translate("admc.misc.gar.lightbox.confirm.title");
              this.garSettings.lightboxMessage = this.bundles.translate("admc.misc.gar.lightbox.confirm.message", {label: this.garMap.get(this.garSettings.garOptionSelected), id: this.garSettings.garOptionSelected});
              this.garSettings.lightboxList = data.map(struc => `${struc.UAI} - ${struc.name}`);
              this.garSettings.structures = data as StructureModel[];
              this.garSettings.lightboxCanValidate = true;
            }
            this.showConfirmLightbox = true;
            this.changeDetector.markForCheck();
          },
          error: (error) =>
          {
            this.notify.error("admc.misc.gar.error.uai.notify.content", "admc.misc.gar.error.notify.title");
          }
        }
      );
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

          if (errors.length == targetUAI.length) {
            this.notify.notify("admc.misc.gar.global.error.notify.content", "admc.misc.gar.error.notify.title", null, "error", { timeout: 5000 });
          } else if (errors.length != 0) {
            this.notify.notify("admc.misc.gar.partial.error.notify.content", "admc.misc.gar.error.notify.title", null, "error", { timeout: 5000 });
            this.garSettings.lightboxList = errors;
            this.showErrorLightbox = true;
            globalStore.structures.sync();
          } else {
            this.notify.notify("admc.misc.gar.success.notify.content", "admc.misc.gar.success.notify.title", null, "success", { timeout: 5000 });
            globalStore.structures.sync();
          }
          this.changeDetector.markForCheck();
        },
        error: (error) =>
        {
          this.notify.error("admc.misc.gar.global.error.notify.content", "admc.misc.gar.error.notify.title");
        }
      });
  }
}
