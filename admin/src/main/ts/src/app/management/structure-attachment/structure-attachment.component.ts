import {
  AfterViewInit,
  EventEmitter,
  ChangeDetectionStrategy,
  Component,
  Injector,
  Input,
  Output,
  OnDestroy,
  OnChanges,
  OnInit,
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { StructureModel } from "../../core/store/models/structure.model";
import { globalStore } from "../../core/store/global.store";
import { NotifyService } from "src/app/core/services/notify.service";
import { StructureAttachmentService } from "./structure-attachment.service";

@Component({
  selector: "ode-structure-attachment",
  templateUrl: "./structure-attachment.component.html",
  styleUrls: ["./structure-attachment.component.scss"],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StructureAttachmentComponent
  extends OdeComponent
  implements OnInit, OnChanges, OnDestroy, AfterViewInit
{
  @Input() currentStructure;
  @Input() isADMC;

  @Output() selectStructures: EventEmitter<StructureModel[]> =
    new EventEmitter();

  searchTerm: string;
  currentId: string;
  boobool: boolean;
  structures: StructureModel[];
  initialList: StructureModel[];
  selectedStructure: StructureModel[] = [];
  showAddStructuresLightBox:boolean = false;

  constructor(
    injector: Injector,
    private infoService: StructureAttachmentService,
    private notify: NotifyService
  ) {
    super(injector);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.init();
  }

  ngOnChanges(): void {
    this.init();
  }

  init = (): void => {
    this.currentId = this.currentStructure.id;
    this.structures = globalStore.structures.data;
    
    this.structures = this.structures.filter(
      structure => structure.id !== this.currentId
    );

    this.initialList = [...this.structures];
  };

  selectStructure(structure: StructureModel): void {
    if (this.selectedStructure.indexOf(structure) === -1) {
      this.selectedStructure.push(structure);
    } else {
      this.selectedStructure = this.selectedStructure.filter(
        item => item.id !== structure.id
      );
    }
    this.selectStructures.emit(this.selectedStructure);
  }

  isSelected = (structure: StructureModel) => {
    return this.selectedStructure.indexOf(structure) > -1;
  };

  filterByStructure = (structure: StructureModel) => {
    if (this.currentStructure.parents) {
      return !this.currentStructure.parents.some(({ id: id2 }) => id2 === structure.id)
    }

    return true;
}

  showLightBox = ():void => {
    this.showAddStructuresLightBox = true;
    document.body.style.overflowY = "hidden";
  }

  closeLightBox = ():void => {
    this.showAddStructuresLightBox = false;
    document.body.style.overflowY = "auto";
  }

  onSubmit = ():void => {
    if (!this.searchTerm) this.structures = this.initialList;
    this.structures = this.initialList.filter(structure => {
      return (
        structure.name.toLowerCase().indexOf(this.searchTerm.toLowerCase()) >=
          0 ||
        (structure.UAI !== null &&
          structure.UAI.toLowerCase().indexOf(this.searchTerm.toLowerCase()) >=
            0)
      );
    });

    this.changeDetector.markForCheck();
  };

  cancelAttach() {
    this.selectedStructure = [];
    this.closeLightBox();
  }

  attachParent = (): void => {
    this.currentStructure.parents = this.currentStructure.parents || [];
    this.selectedStructure.map(structure => {
      if (!this.currentStructure.parents.includes(structure)) {
        this.infoService.defineParent(this.currentId, structure.id).subscribe({
          next: data => {
            this.currentStructure.parents.push(structure);

            this.notify.success(
              "management.structure.informations.attach.parent.success.content",
              "management.structure.informations.attach.parent.success.title"
            );
            this.changeDetector.detectChanges();
            this.changeDetector.markForCheck();
          },
          error: error => {
            this.notify.notify(
              "management.structure.informations.attach.parent.error.content",
              "management.structure.informations.attach.parent.error.title",
              error.statusText,
              "error"
            );
          },
        });
      }
    });
  };

  detachParent = (parentId: string): void => {
    this.infoService.detachParent(this.currentId, parentId).subscribe({
      next: data => {
        for (let i = this.currentStructure.parents.length; i-- > 0; ) {
          if (this.currentStructure.parents[i].id == parentId) {
            this.currentStructure.parents.splice(i, 1);
          }
        }

        this.notify.success(
          "management.structure.informations.detach.parent.success.content",
          "management.structure.informations.detach.parent.success.title"
        );
        this.init();
        this.changeDetector.detectChanges();
        this.changeDetector.markForCheck();
      },
      error: error => {
        this.notify.notify(
          "management.structure.informations.detach.parent.error.content",
          "management.structure.informations.detach.parent.error.title",
          error.statusText,
          "error"
        );
      },
    });
  };
}
