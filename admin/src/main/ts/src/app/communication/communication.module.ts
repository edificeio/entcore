import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgxOdeSijilModule } from 'ngx-ode-sijil';
import { NgxOdeUiModule } from 'ngx-ode-ui';
import { CommunicationRulesComponent } from './communication-rules.component/communication-rules.component';
import { GroupCardComponent } from './group-card.component/group-card.component';
import { GroupPickerComponent } from './group-picker/group-picker.component';
import { GroupsCommunicationComponent } from './groups-communication.component/groups-communication.component';
@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        NgxOdeSijilModule.forChild(),
        NgxOdeUiModule
    ],
    declarations: [
        CommunicationRulesComponent,
        GroupCardComponent,
        GroupsCommunicationComponent,
        GroupPickerComponent
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
