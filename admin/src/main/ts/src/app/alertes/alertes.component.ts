import { OdeComponent } from 'ngx-ode-core';
import {Component, OnInit, Injector, ViewEncapsulation} from '@angular/core';

@Component({
  selector: 'ode-alertes',
  templateUrl: './alertes.component.html',
  styleUrls: ['./alertes.component.scss'],
  encapsulation: ViewEncapsulation.None

})
export class AlertesComponent extends OdeComponent implements OnInit {
  tabs = [
    {label: 'reports', view: 'signalements'}
];
  constructor(injector: Injector) {
    super(injector);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
