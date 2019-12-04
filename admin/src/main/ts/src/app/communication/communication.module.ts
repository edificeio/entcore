import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {NgxOdeSijilModule} from 'ngx-ode-sijil';
import {NgxOdeUiModule} from 'ngx-ode-ui';
import {CommunicationRulesComponent} from './communication-rules.component/communication-rules.component';
import {GroupCardComponent} from './group-card.component/group-card.component';
import {GroupsCommunicationComponent} from './groups-communication.component/groups-communication.component';
import { CoreModule } from '../core/core.module';

@NgModule({
    imports: [
        CoreModule,
        CommonModule,
        NgxOdeSijilModule.forChild(),
        NgxOdeUiModule.forChild()
    ],
    declarations: [
        CommunicationRulesComponent,
        GroupCardComponent,
        GroupsCommunicationComponent
    ],
    providers: [],
    exports: [
        CommunicationRulesComponent,
        GroupCardComponent,
        GroupsCommunicationComponent
    ]
})
export class CommunicationModule {
}
