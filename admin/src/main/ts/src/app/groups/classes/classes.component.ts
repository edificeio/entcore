import {
  ChangeDetectionStrategy,
  Component,
  Injector,
  OnDestroy,
  OnInit
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { ClassModel } from "src/app/core/store/models/structure.model";
import { GroupsStore } from "../groups.store";

@Component({
  selector: "ode-classes",
  templateUrl: "./classes.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClassesComponent
  extends OdeComponent
  implements OnInit, OnDestroy
{
  classInputFilter: string;
  selectedClass: ClassModel;

  constructor(public groupsStore: GroupsStore, injector: Injector) {
    super(injector);
  }

  ngOnInit() {
    super.ngOnInit();
    
    this.subscriptions.add(
      this.groupsStore.$onchange.subscribe(field => {
        if (field === "structure") {
          this.changeDetector.markForCheck();
          this.changeDetector.detectChanges();
        }
      })
    );
  }

  isSelected = (c: ClassModel) => {
    return this.groupsStore.class && c && this.groupsStore.class.id === c.id;
  };

  filterByInput = (c: ClassModel) => {
    return !this.classInputFilter || c.name.toLowerCase().indexOf(this.classInputFilter.toLowerCase()) >= 0
  };

  showCompanion(): boolean {
    const classId = this.groupsStore.class ? this.groupsStore.class.id : "";
    const classRoute = `/admin/${this.groupsStore.structure.id}/groups/classes`;

    if (this.router.isActive(classRoute + "/create", true)) {
      return true;
    }

    if (this.router.isActive(`${classRoute}/${classId}/details`, true)) {
      return true;
    }

    return false;
  }

  closePanel() {
    this.router.navigateByUrl(`/admin/${this.groupsStore.structure.id}/groups/classes`);
  }

  routeToClass(c: ClassModel) {
    this.router.navigate([c.id, "details"], { relativeTo: this.route });
  }
}
