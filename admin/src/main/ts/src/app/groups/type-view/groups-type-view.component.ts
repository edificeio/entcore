import {
  ChangeDetectionStrategy,
  Component,
  Injector,
  OnDestroy,
  OnInit,
  Input
} from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { GroupModel } from "../../core/store/models/group.model";
import { GroupsStore } from "../groups.store";

@Component({
  selector: "ode-groups-type-view",
  templateUrl: "./groups-type-view.component.html",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupsTypeViewComponent
  extends OdeComponent
  implements OnInit, OnDestroy
{
  noResultsLabel: string;
  groupType: string;
  groupInputFilter: string;
  selectedGroup: GroupModel;

  constructor(public groupsStore: GroupsStore, injector: Injector) {
    super(injector);
  }

  ngOnInit() {
    super.ngOnInit();
    // Init trad label when no result (default group)
    this.noResultsLabel = 'list.results.no.groups';
    
    this.subscriptions.add(
      this.route.params.subscribe(params => {
        this.groupsStore.group = null;
        const type = params.groupType;
        const allowedTypes = [
          "manualGroup",
          "profileGroup",
          "functionalGroup",
          "functionGroup",
          "broadcastGroup",
        ];
        if (type && allowedTypes.indexOf(type) >= 0) {
          this.groupType = params.groupType;
          this.changeDetector.markForCheck();
        } else {
          this.router.navigate([".."], { relativeTo: this.route });
        }
      })
    );
    this.subscriptions.add(
      this.groupsStore.$onchange.subscribe(field => {
        if (field === "structure") {
          this.changeDetector.markForCheck();
          this.changeDetector.detectChanges();
        }
      })
    );

    // handle change detection from create button click of group-root.component
    this.subscriptions.add(
      this.route.url.subscribe(path => {
        if (this.groupType === 'broadcastGroup') {
            this.noResultsLabel = 'list.results.no.broadcast';
        } else {
            this.noResultsLabel = 'list.results.no.groups';
        }
        this.changeDetector.markForCheck();
      })
    );
  }

  // Need this method to get first letter uppercase to compare
    // groupType (param from route) vs group.type from object group
    capitalize = (s) => {
        if (typeof s !== 'string') return '';
        return s.charAt(0).toUpperCase() + s.slice(1)
    }

    // Filter list using [filters] depending on route's param (broadcastGroup)
    // Return booleans
    filterByGroup = (group: GroupModel) => {
        const { type, subType } = group;

        if (this.groupType === 'broadcastGroup') {
            return type === 'ManualGroup' && subType === 'BroadcastGroup';
        }

        return type === this.capitalize(this.groupType) && subType !== 'BroadcastGroup';
    };

  isSelected = (group: GroupModel) => {
    return this.groupsStore.group && group && this.groupsStore.group.id === group.id;
  };

  filterByInput = (group: GroupModel) => {
    if (!this.groupInputFilter) {
      return true;
    }
    return (
      group.name.toLowerCase().indexOf(this.groupInputFilter.toLowerCase()) >= 0
    );
  };

  showCompanion(): boolean {
    const groupTypeRoute =
      "/admin/" +
      (this.groupsStore.structure ? this.groupsStore.structure.id : "") +
      "/groups/" +
      this.groupType;

    let res: boolean =
      this.router.isActive(groupTypeRoute + "/create", true) ||
      this.router.isActive(groupTypeRoute + "/list", true);
    if (this.groupsStore.group) {
      res =
        res ||
        this.router.isActive(
          groupTypeRoute + "/" + this.groupsStore.group.id + "/details",
          true
        ) ||
        this.router.isActive(
          groupTypeRoute + "/" + this.groupsStore.group.id + "/communication",
          true
        );
    }
    return res;
  }

  closePanel() {
    this.router.navigateByUrl(
      "/admin/" +
        (this.groupsStore.structure ? this.groupsStore.structure.id : "") +
        "/groups/" +
        this.groupType
    );
  }

  routeToGroup(g: GroupModel) {
    this.router.navigate([g.id, "details"], { relativeTo: this.route });
  }
}
