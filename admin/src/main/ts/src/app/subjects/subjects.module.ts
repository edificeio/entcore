import {CommonModule} from '@angular/common'
import {FormsModule} from '@angular/forms'
import {RouterModule} from '@angular/router'
import {NgModule} from '@angular/core'
import {NgxOdeSijilModule} from 'ngx-ode-sijil';
import {NgxOdeUiModule} from 'ngx-ode-ui';
import {routes} from './subjects.routing'
import {SubjectsComponent} from './subjects.component'
import {SubjectsStore} from "./subjects.store";
import {SubjectCreate} from "./create/subject-create.component";
import {SubjectsResolver} from "./subjects.resolver";
import {SubjectDetails} from "./details/subject-details.component";
import {SubjectsService} from "./subjects.service";


@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        NgxOdeUiModule,
        NgxOdeSijilModule.forChild(),
        RouterModule.forChild(routes)
    ],
    declarations: [
        SubjectsComponent,
        SubjectCreate,
        SubjectDetails
    ],
    exports: [
        RouterModule
    ],
    providers: [
        SubjectsResolver,
        SubjectsStore,
        SubjectsService
    ]
})
export class SubjectsModule {
}