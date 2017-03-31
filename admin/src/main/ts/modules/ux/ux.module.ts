import { SijilModule } from 'sijil'
import { InfraComponentsModule } from 'infra-components/dist'
import { FormsModule } from '@angular/forms'
import { CommonModule } from '@angular/common'
import { NgModule } from '@angular/core'

import { FormErrors, FormField, ListComponent, PanelSection, SideLayout, SpinnerComponent, Datepicker } from './components'
import { AnchorDirective } from './directives'

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        SijilModule.forChild(),
        InfraComponentsModule
    ],
    declarations: [
        SpinnerComponent,
        SideLayout,
        PanelSection,
        ListComponent,
        FormField,
        FormErrors,
        AnchorDirective,
        Datepicker
    ],
    exports: [
        SpinnerComponent,
        SideLayout,
        PanelSection,
        ListComponent,
        FormField,
        FormErrors,
        AnchorDirective,
        Datepicker
    ]
})
export class UxModule{}