import { Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { routing } from 'src/app/core/services/routing.service';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { ImportEDTReportsService } from './import-edt-reports.service';
import { ImportTimetableService } from './import-timetable.service';
import { SelectOption } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Session } from 'src/app/core/store/mappings/session';
import { SessionModel } from 'src/app/core/store/models/session.model';

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

export enum EDTImportMode
{
  DEFAULT,
  TIMETABLE_ONLY,
  GROUPS_ONLY
}

export interface TimetableClassesMapping
{
  unknownClasses: String[],
  classNames: String[],
  classesMapping: object, //Map<String, String>
}
export interface TimetableGroupsMapping
{
  unknownGroups: String[],
  groupNames: String[],
  groupsMapping: object, //Map<String, String>
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
  public EDTImportFlux =  EDTImportFlux;
  public EDTImportMode =  EDTImportMode;

  private structure: StructureModel;
  private reportList: EDTReport[] = [];

  public seeMore: boolean = false;
  public shownReport: string;
  public showFluxChangeWarning: boolean = false;

  public mainFlux: EDTImportFlux = null;
  public importMode: EDTImportMode = EDTImportMode.DEFAULT;
  public onetimeImport: EDTImportFlux = null;
  private importFile: FileList;

  public unknownClasses: String[] = [];
  public classNames: SelectOption<String>[] = [];
  public classesMapping: object = {}; // Map<String, String>

  public unknownGroups: String[] = [];
  public groupNames: SelectOption<String>[] = [];
  public groupsMapping: object = {}; // Map<String, String>

  private isADMC: boolean = false;

  constructor(injector: Injector, private reportService: ImportEDTReportsService, private timetableService: ImportTimetableService, private notify: NotifyService)
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
        this.mainFlux = this.structure.timetable as EDTImportFlux;
        this.onetimeImport = this.structure.punctualTimetable as EDTImportFlux;
        this._getReportsFromService();
        this._getClassesMapping();
        this._getGroupsMapping();
      }
    }));
    this.admcSpecific();
  }

  structureTitle(): String
  {
    return this.structure == null ? "" : this.structure.name + (this.structure.UAI == null ? "" : " — " + this.structure.UAI);
  }

  isFluxTypeBlocked(): boolean
  {
    return !(this.isADMC || this.structure.timetable == null);
  }

  updateFluxType(): void
  {
    let oldTimetable = this.structure.timetable;
    let error = (data) =>
    {
      this.notify.error("management.edt.flux.notify.error.content", "management.edt.flux.notify.error.title", data);
    };
    this.timetableService.setFluxType(this.structure.id, this.mainFlux).subscribe(
    {
      next: (data) =>
      {
        if(data.update == true || (oldTimetable == this.mainFlux || (oldTimetable == null && this.mainFlux == EDTImportFlux.DEFAULT)))
        {
          this.notify.success("management.edt.flux.notify.success.content", "management.edt.flux.notify.success.title");
          this.structure.timetable = this.mainFlux;
          this.structure.syncGroups(true);
          this._getClassesMapping();
          this._getGroupsMapping();
        }
        else
          error(data);
      },
      error: error,
    });
  }

  canImport(): boolean
  {
    return this.structure.timetable != EDTImportFlux.DEFAULT && this.structure.timetable != EDTImportFlux.NONE && this.structure.timetable != null;
  }

  canChooseImportMode(): boolean
  {
    return this.structure.timetable == EDTImportFlux.EDT || this.structure.timetable == EDTImportFlux.UDT;
  }

  canOnetimeImport(): boolean
  {
    return this.structure.timetable == EDTImportFlux.DEFAULT;
  }

  loadFile($event): void
  {
    this.importFile = $event.target.files;
  }

  manualImport(importMode: EDTImportMode, flux: EDTImportFlux): void
  {
    this.timetableService.importFile(this.structure.id, importMode, flux, this.importFile).then((data) =>
    {
      this.notify.success("management.edt.import.notify.success.content", "management.edt.import.notify.success.title");
      this._getReportsFromService();
      this.structure.syncGroups(true);
      this._getClassesMapping();
      this._getGroupsMapping();
    }).catch((err) =>
    {
      if(err.status == 0)
      {
        this.notify.notify("management.edt.import.notify.unknown.content", "management.edt.import.notify.unknown.title", err.statusText, "error", { timeout: 12000 });
      }
      if(err.error.error != null)
      {
        this.notify.notify("management.edt.import.notify.error.content", "management.edt.import.notify.error.title", err.error.error, "error", { timeout: 4000 });
      }
      for(let i in err.error.errors)
      {
        let msg = err.error.errors[i];

        if(Array.isArray(msg) == false)
          msg = [msg];

        for(let j =  0; j < msg.length; ++j)
          this.notify.notify("management.edt.import.notify.error.content", "management.edt.import.notify.error.title", msg[j], "error");
      }
    });
  }

  private _getClassesMapping(): void
  {
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
  }

  private _getGroupsMapping(): void
  {
    this.timetableService.getGroupsMapping(this.structure.id).subscribe({
      next: (data) =>
      {
        this.unknownGroups = data.unknownGroups == null ? [] : data.unknownGroups.sort();

        if(data.groupNames != null)
        {
          this.groupNames = new Array<SelectOption<String>>(data.groupNames.length);

          data.groupNames = data.groupNames.sort();
          for(let i = this.groupNames.length; i-- > 0;)
            this.groupNames[i] = { label: data.groupNames[i].toString(), value: data.groupNames[i], };
        }
        else
          this.groupNames = [];

        this.groupsMapping = data.groupsMapping == null ? {} : data.groupsMapping;

        this.changeDetector.markForCheck();
      }
    });
  }

  updateClassesMapping(): void
  {
    let cm: TimetableClassesMapping = {
      unknownClasses: null,
      classNames: null,
      classesMapping: null,
    };
    cm.unknownClasses = this.unknownClasses;

    cm.classNames = new Array<String>(this.classNames.length);
    for(let i = this.classNames.length; i-- > 0;)
      cm.classNames[i] = this.classNames[i].value;

    cm.classesMapping = {};
    for(let uk in this.classesMapping)
      if(this.classesMapping[uk] != null)
        cm.classesMapping[uk] = this.classesMapping[uk];

    this.timetableService.updateClassesMapping(this.structure.id, cm).subscribe(
    {
      next: (data) =>
      {
        this.notify.success("management.edt.correspondance.notify.success.content", "management.edt.correspondance.notify.success.title");
      },
      error: (error) =>
      {
        this.notify.notify("management.edt.correspondance.notify.error.content", "management.edt.correspondance.notify.error.title", error, "error");
      }
    });
  }

  updateGroupsMapping(): void
  {
    let gm: TimetableGroupsMapping = {
      unknownGroups: null,
      groupNames: null,
      groupsMapping: null,
    };
    gm.unknownGroups = this.unknownGroups;

    gm.groupNames = new Array<String>(this.classNames.length);
    for(let i = this.classNames.length; i-- > 0;)
      gm.groupNames[i] = this.classNames[i].value;

    gm.groupsMapping = {};
    for(let uk in this.groupsMapping)
      if(this.groupsMapping[uk] != null)
        gm.groupsMapping[uk] = this.groupsMapping[uk];

    this.timetableService.updateGroupsMapping(this.structure.id, gm).subscribe(
    {
      next: (data) =>
      {
        this.notify.success("management.edt.correspondance.group.notify.success.content", "management.edt.correspondance.group.notify.success.title");
      },
      error: (error) =>
      {
        this.notify.notify("management.edt.correspondance.group.notify.error.content", "management.edt.correspondance.group.notify.error.title", error, "error");
      }
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
