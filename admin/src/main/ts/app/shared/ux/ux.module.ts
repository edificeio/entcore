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
    MultiSelectComponent,
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
    MessageStickerComponent,
    MessageBoxComponent,
    GroupPickerComponent,
    UploadFilesComponent } from './components'
import { AnchorDirective, DynamicTemplateDirective, DynamicComponentDirective, DragAndDropFilesDirective } from './directives'
import { FilterPipe, OrderPipe, StorePipe, LimitPipe, FlattenObjectArrayPipe, LocalizedDatePipe, BytesPipe } from './pipes'
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
        MultiSelectComponent,
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
        MessageStickerComponent,
        MessageBoxComponent,
        GroupPickerComponent,
        UploadFilesComponent,
        // directives
        AnchorDirective,
        DynamicTemplateDirective,
        DynamicComponentDirective,
        DragAndDropFilesDirective,
        // pipes
        FilterPipe,
        FlattenObjectArrayPipe,
        LimitPipe,
        OrderPipe,
        StorePipe,
        LocalizedDatePipe,
        BytesPipe
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
        MultiSelectComponent,
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
        MessageStickerComponent,
        MessageBoxComponent,
        GroupPickerComponent,
        UploadFilesComponent,
        // directives
        AnchorDirective,
        DynamicTemplateDirective,
        DynamicComponentDirective,
        DragAndDropFilesDirective,
        // pipes
        FilterPipe,
        FlattenObjectArrayPipe,
        LimitPipe,
        OrderPipe,
        StorePipe,
        LocalizedDatePipe,
        BytesPipe
    ],
    providers: [],
    entryComponents: [SimpleSelectComponent, MessageBoxComponent]
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
