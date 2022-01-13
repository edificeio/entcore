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
import { CommunicationRulesService } from "../../communication/communication-rules.service";
import { routing } from "../../core/services/routing.service";
import { GroupsStore } from "../groups.store";
import { Session } from "src/app/core/store/mappings/session";
import { SessionModel } from "src/app/core/store/models/session.model";

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
    { label: "ManualGroup", view: "manualGroup" },
    { label: "ProfileGroup", view: "profileGroup" },
    { label: "FunctionalGroup", view: "functionalGroup" },
    { label: "FunctionGroup", view: "functionGroup" },
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

    this.showBroadcastGroupTab();
  }

  onError(error: Error) {
    console.error(error);
    this.groupsError = error;
  }

  createButtonHidden(groupType) {
    return (
      !this.router.isActive(
        `/admin/${this.groupsStore.structure.id}/groups/${groupType}`,
        false
      ) ||
      this.router.isActive(
        `/admin/${this.groupsStore.structure.id}/groups/${groupType}/create`,
        true
      )
    );
  }

  async showBroadcastGroupTab() {
    const session: Session = await SessionModel.getSession();
    this.isADMC = session.isADMC();
    if (this.isADMC) {
      this.tabs.push({ label: "BroadcastGroup", view: "broadcastGroup" });
    }
    this.changeDetector.markForCheck();
  }
}
