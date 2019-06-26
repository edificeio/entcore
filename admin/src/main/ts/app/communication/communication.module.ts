import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { SijilModule } from 'sijil';
import { UxModule } from '../shared/ux/ux.module';
import { CommunicationRulesComponent } from './communication-rules.component';
import { GroupCardComponent } from './group-card.component';
import { GroupsCommunicationComponent } from './groups-communication.component';

@NgModule({
    imports: [
        CommonModule,
        SijilModule.forChild(),
        UxModule.forChild()
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
