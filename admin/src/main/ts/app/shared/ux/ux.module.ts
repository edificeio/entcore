import { CommonModule } from '@angular/common'
import { ModuleWithProviders, NgModule, Provider } from '@angular/core'
import { FormsModule } from '@angular/forms'

import { SijilModule } from 'sijil';

import { DatepickerComponent, 
    FormErrorsComponent, 
    FormFieldComponent, 
    ItemTreeComponent,
    LightBoxComponent,
    LightboxConfirmComponent, 
    ListComponent, 
    MultiComboComponent,
    PanelSectionComponent, 
    PortalComponent,
    PushPanelComponent, 
    SearchInputComponent,
    SideLayoutComponent, 
    SidePanelComponent, 
    SpinnerCubeComponent,
    StepComponent,
    TooltipComponent,
    WizardComponent,
    SimpleSelectComponent } from './components'
import { AnchorDirective, DynamicTemplateDirective } from './directives'
import { FilterPipe, OrderPipe, StorePipe, LimitPipe, FlattenObjectArrayPipe } from './pipes'
import { DynamicModuleImportsService, LabelsService } from './services';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        SijilModule.forChild(),
        InfiniteScrollModule
    ],
    declarations: [
        // components
        DatepickerComponent,
        FormErrorsComponent,
        FormFieldComponent,
        ItemTreeComponent,
        LightBoxComponent,
        LightboxConfirmComponent,
        ListComponent,
        MultiComboComponent,
        PanelSectionComponent,
        PortalComponent,
        PushPanelComponent,
        SearchInputComponent,
        SideLayoutComponent,
        SidePanelComponent,
        SpinnerCubeComponent,
        StepComponent,
        TooltipComponent,
        WizardComponent,
        SimpleSelectComponent,
        // directives
        AnchorDirective,
        DynamicTemplateDirective,
        // pipes
        FilterPipe,
        FlattenObjectArrayPipe,
        LimitPipe,
        OrderPipe,
        StorePipe
    ],
    exports: [
        //components
        DatepickerComponent,
        FormErrorsComponent,
        FormFieldComponent,
        ItemTreeComponent,
        LightBoxComponent,
        LightboxConfirmComponent,
        ListComponent,
        MultiComboComponent,
        PanelSectionComponent,
        PortalComponent,
        PushPanelComponent,
        SearchInputComponent,
        SideLayoutComponent,
        SidePanelComponent,
        SpinnerCubeComponent,
        StepComponent,
        TooltipComponent,
        WizardComponent,
        // directives
        AnchorDirective,
        DynamicTemplateDirective,
        // pipes
        FilterPipe,
        FlattenObjectArrayPipe,
        LimitPipe,
        OrderPipe,
        StorePipe
    ],
    providers: [],
    entryComponents: [SimpleSelectComponent]
})
export class UxModule {
    static forRoot(labelsProvider: Provider): ModuleWithProviders {
        return {
            ngModule: UxModule,
            providers: [
                DynamicModuleImportsService,
                labelsProvider || LabelsService
            ]
        };
    }

    static forChild() : ModuleWithProviders {
        return {
            ngModule: UxModule,
            providers: []
        }
    }
}
