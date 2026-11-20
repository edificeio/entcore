import { OdeComponent } from 'ngx-ode-core';
import {Component, OnInit, Injector, ViewEncapsulation} from '@angular/core';
import { canAccessReportedContents } from './reported-contents/reported-contents.access';

interface AlertesTab {
  label: string;
  view: string;
}

/** BETA tab, only shown to users having the right (see canAccessReportedContents). */
const CONTENTS_TAB: AlertesTab = {label: 'alerts.tab.contents', view: 'contenus'};
const NOTIFICATIONS_TAB: AlertesTab = {label: 'alerts.tab.notifications', view: 'signalements'};

@Component({
  selector: 'ode-alertes',
  templateUrl: './alertes.component.html',
  styleUrls: ['./alertes.component.scss'],
  encapsulation: ViewEncapsulation.None

})
export class AlertesComponent extends OdeComponent implements OnInit {
  tabs: AlertesTab[] = [NOTIFICATIONS_TAB];

  constructor(injector: Injector) {
    super(injector);
  }

  ngOnInit() {
    super.ngOnInit();
    canAccessReportedContents().then(allowed => {
      if (allowed) {
        this.tabs = [CONTENTS_TAB, NOTIFICATIONS_TAB];
        this.changeDetector.markForCheck();
      }
    });
  }

}
