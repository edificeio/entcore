import {
  ChangeDetectionStrategy,
  Component,
  Injector,
  OnDestroy,
  OnInit,
} from "@angular/core";
import { Data, NavigationEnd } from "@angular/router";
import { OdeComponent } from "ngx-ode-core";
import { Subscription } from "rxjs";
import { Session } from "src/app/core/store/mappings/session";
import { SessionModel } from "src/app/core/store/models/session.model";
import { CommunicationRulesService } from "../../communication/communication-rules.service";
import { routing } from "../../core/services/routing.service";
import { GroupsStore } from "../groups.store";
import { GroupType } from "src/app/core/store/models/group.model";

@Component({
  selector: "ode-groups-root",
  templateUrl: "./groups.component.html",
  providers: [CommunicationRulesService],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GroupsComponent extends OdeComponent implements OnInit, OnDestroy {
  // Subscribers
  private structureSubscriber: Subscription;
  isADMC: boolean = false;

  // Tabs
  tabs = [
    { label: "Classes", view: "classes", visible: false },  // admin only
    { label: "ManualGroup", view: "manualGroup", visible: true },
    { label: "ProfileGroup", view: "profileGroup", visible: true },
    { label: "FunctionalGroup", view: "functionalGroup", visible: true },
    { label: "FunctionGroup", view: "functionGroup", visible: false },  // optional
    { label: "BroadcastGroup", view: "broadcastGroup", visible: false }    // admin only
  ];

  groupsError: any;

  constructor(injector: Injector, public groupsStore: GroupsStore) {
    super(injector);
  }

  ngOnInit(): void {
    super.ngOnInit();
    // Watch selected structure
    this.subscriptions.add(
      routing.observe(this.route, "data").subscribe((data: Data) => {
        if (data.structure) {
          this.groupsStore.structure = data.structure;
          this.showOptionalTabs();
          this.changeDetector.markForCheck();
        }
      })
    );

    this.subscriptions.add(
      this.router.events.subscribe(e => {
        if (e instanceof NavigationEnd) {
          this.changeDetector.markForCheck();
        }
      })
    );

    this.showAdminTabs();
  }

  onError(error: Error) {
    console.error(error);
    this.groupsError = error;
  }

  createButtonHidden(keyword) {
    if( keyword==='classes' ) {
      return !this.router.isActive(`/admin/${this.groupsStore.structure.id}/groups/${keyword}`, false)
        || this.router.isActive(`/admin/${this.groupsStore.structure.id}/groups/${keyword}/create`, true)
    } else {
      return (
        !this.router.isActive(
          `/admin/${this.groupsStore.structure.id}/groups/${keyword}`,
          false
        ) ||
        this.router.isActive(
          `/admin/${this.groupsStore.structure.id}/groups/${keyword}/create`,
          true
        )
      );
    }
  }

  async showAdminTabs() {
    const session: Session = await SessionModel.getSession();
    this.isADMC = session.isADMC();
    const isADML = session.isADML();

    if (this.isADMC) {
      this.tabs.find(tab => tab.view==="classes").visible = true;
    }
    if ((isADML || this.isADMC) && this.groupsStore.structure?.children?.length) {
      this.tabs.find(tab => tab.view==="broadcastGroup").visible = true;
    }
    this.changeDetector.markForCheck();
  }

  showOptionalTabs() {
    const groups = this.groupsStore.structure?.groups?.data;
    // Function tab is visible only when not empty
    this.tabs.find(tab => tab.view==="functionGroup").visible = (
      Array.isArray(groups) &&
      groups.some(group=>(
        group.type==='FunctionGroup' || group.type==='FuncGroup' as GroupType)
      ));
  }
}
