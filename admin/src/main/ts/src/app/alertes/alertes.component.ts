import { OdeComponent } from 'ngx-ode-core';
import { Component, OnInit, Injector } from '@angular/core';

@Component({
  selector: 'ode-alertes',
  templateUrl: './alertes.component.html',
  styleUrls: ['./alertes.component.scss']
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
