import {CommonModule} from '@angular/common';
import {ModuleWithProviders, NgModule, Provider} from '@angular/core';
import {FormsModule} from '@angular/forms';

import {SijilModule} from 'sijil';

import {
  DatepickerComponent,
  EllipsisComponent,
  FormErrorsComponent,
  FormFieldComponent,
  GroupPickerComponent,
  ItemTreeComponent,
  LengthPipe,
  LightBoxComponent,
  LightboxConfirmComponent,
  ListComponent,
  MessageBoxComponent,
  MessageStickerComponent,
  MonoSelectComponent,
  MultiComboComponent,
  MultiSelectComponent,
  PagerComponent,
  PanelSectionComponent,
  PortalComponent,
  PushPanelComponent,
  SearchInputComponent,
  SideLayoutComponent,
  SidePanelComponent,
  SimpleSelectComponent,
  SpinnerCubeComponent,
  StepComponent,
  TooltipComponent,
  UploadFilesComponent,
  WizardComponent
} from './components';

import {
  AnchorDirective,
  DragAndDropFilesDirective,
  DynamicComponentDirective,
  DynamicTemplateDirective,
  ObjectURLDirective
} from './directives';
import {BytesPipe, FilterPipe, FlattenObjectArrayPipe, KeysPipe, LimitPipe, LocalizedDatePipe, OrderPipe, StorePipe} from './pipes';
import {DynamicModuleImportsService, InputFileService, LabelsService} from './services';
import {InfiniteScrollModule} from 'ngx-infinite-scroll';

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
        MonoSelectComponent,
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
        EllipsisComponent,
        PagerComponent,
        LengthPipe,
        MessageStickerComponent,
        MessageBoxComponent,
        // directives
        AnchorDirective,
        DynamicTemplateDirective,
        DynamicComponentDirective,
        DragAndDropFilesDirective,
        ObjectURLDirective,
        // pipes
        FilterPipe,
        FlattenObjectArrayPipe,
        LimitPipe,
        OrderPipe,
        StorePipe,
        LocalizedDatePipe,
        BytesPipe,
        KeysPipe
    ],
    exports: [
        // components
        DatepickerComponent,
        FormErrorsComponent,
        FormFieldComponent,
        ItemTreeComponent,
        LightBoxComponent,
        LightboxConfirmComponent,
        ListComponent,
        MonoSelectComponent,
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
        SimpleSelectComponent,
        EllipsisComponent,
        PagerComponent,
        LengthPipe,
        MessageStickerComponent,
        MessageBoxComponent,
        // directives
        AnchorDirective,
        DynamicTemplateDirective,
        DynamicComponentDirective,
        DragAndDropFilesDirective,
        ObjectURLDirective,
        // pipes
        FilterPipe,
        FlattenObjectArrayPipe,
        LimitPipe,
        OrderPipe,
        StorePipe,
        LocalizedDatePipe,
        BytesPipe,
        KeysPipe
    ],
    providers: [OrderPipe],
    entryComponents: [SimpleSelectComponent, MessageBoxComponent]
})
export class UxModule {
    static forRoot(labelsProvider: Provider): ModuleWithProviders {
        return {
            ngModule: UxModule,
            providers: [
                DynamicModuleImportsService,
                labelsProvider || LabelsService,
                InputFileService
            ]
        };
    }

    static forChild(): ModuleWithProviders {
        return {
            ngModule: UxModule,
            providers: []
        };
    }
}
