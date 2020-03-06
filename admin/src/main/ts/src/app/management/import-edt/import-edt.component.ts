import { Component, Injector, OnDestroy, OnInit, SystemJsNgModuleLoader } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from 'src/app/core/services/routing.service';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { ImportEDTReportsService } from './import-edt-reports.service';
import { ImportTimetableService } from './import-timetable.service';

export interface EDTReport
{
  _id: string,
  id: string,
  created: any, // mongo date
  source: string,
  manual: boolean,
  date: string,
  report?: string,
}

export enum EDTImportFlux
{
  DEFAULT="",
  NONE="NOP",
  EDT="EDT",
  UDT="UDT",
}

@Component(
{
  selector: 'ode-import-edt',
  templateUrl: './import-edt.component.html',
  styleUrls: ['./import-edt.component.scss']
})

export class ImportEDTComponent extends OdeComponent implements OnInit, OnDestroy
{
  private static NB_REPORTS_COLLAPSED = 5;

  //Angular hack to access the enum in the HTML
  private EDTImportFlux =  EDTImportFlux;

  private structure: StructureModel;
  private reportList: EDTReport[] = [];

  private seeMore: boolean = false;
  private shownReport: string;

  private changeFlux: EDTImportFlux = null;
  private showFluxChangeWarning: boolean = false;
  private importFile: FileList;

  constructor(injector: Injector, private reportService: ImportEDTReportsService, private timetableService: ImportTimetableService)
  {
    super(injector);
  }

  ngOnInit(): void
  {
    this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) =>
    {
      if (data.structure)
      {
        this.structure = data.structure;
        this.changeFlux = this.structure.timetable as EDTImportFlux;
        this._getReportsFromService();
      }
    }));
  }

  structureTitle(): String
  {
    return this.structure == null ? "" : this.structure.name + " — " + this.structure.UAI;
  }

  updateFluxType(): void
  {
    this.timetableService.setFluxType(this.structure.id, this.changeFlux).subscribe(
    {
      next: (data) =>
      {
        if(data.update == true)
        {
          this.structure.timetable = this.changeFlux;
          this.changeDetector.markForCheck();
        }
      }
    });
  }
  canImport(): boolean
  {
    return this.structure.timetable != EDTImportFlux.DEFAULT && this.structure.timetable != EDTImportFlux.NONE;
  }

  loadFile($event): void
  {
    this.importFile = $event.target.files;
  }

  manualImport(): void
  {
    this.timetableService.importFile(this.structure.id, this.importFile).then((data) =>
    {
    }).catch((err) =>
    {
    });
  }

  // ============================================ REPORTS ============================================

  getReports(): EDTReport[]
  {
    return this.seeMore == false ? this.reportList.slice(0, ImportEDTComponent.NB_REPORTS_COLLAPSED) : this.reportList;
  }

  hasMore(): boolean
  {
    return this.reportList.length > ImportEDTComponent.NB_REPORTS_COLLAPSED;
  }

  _getReportsFromService(): void
  {
    if(this.structure == null)
      return;

    this.reportService.getList(this.structure.id).subscribe(
    {
      next: (data: EDTReport[]) =>
      {
        this.reportList = data;
        this.changeDetector.markForCheck();
      }
    });
  }

  private _displayReport(report: EDTReport): void
  {
    this.shownReport = report.report;
    this.changeDetector.markForCheck();
  }

  private _fetchReport(report: EDTReport): void
  {
    if(report.report != null)
      this._displayReport(report);
    else
    {
      this.reportService.getReport(this.structure.id, report.id).subscribe(
      {
        next: (data: EDTReport) =>
        {
          report.report = data.report;
          this._displayReport(report);
        }
      });
    }
  }

  showReport(id: string): void
  {
    for(let i = this.reportList.length; i-- > 0;)
    {
      let report = this.reportList[i];
      if(report.id == id)
      {
        this._fetchReport(report);
        break;
      }
    }
  }

  hideReport(): void
  {
    this.shownReport = null;
  }
}
