import { UploadFilesComponent } from './upload-files.component'
import { ComponentFixture, async, TestBed } from '@angular/core/testing';
import { Directive, Input } from '@angular/core';
import { SijilModule } from 'sijil';
import { InputFileService } from '../services/inputFile.service';

describe('UploadFilesComponent', () => {
    let component: UploadFilesComponent;
    let fixture: ComponentFixture<UploadFilesComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [UploadFilesComponent, MockDragAndDropFilesDirective],
            providers: [InputFileService],
            imports: [SijilModule.forRoot()]
        }).compileComponents();

        fixture = TestBed.createComponent(UploadFilesComponent);
        component = fixture.debugElement.componentInstance;
    }));

    it('should create a UploadFilesComponent', () => {
        expect(component).toBeDefined();
    });
});

@Directive({
    selector: '[dragAndDropFiles]'
})
export class MockDragAndDropFilesDirective {
    @Input()
    allowedExtensions: Array<string> = [];
    @Input()
    maxFilesNumber: number = 1;
    @Input()
    disabled: boolean = false;
}