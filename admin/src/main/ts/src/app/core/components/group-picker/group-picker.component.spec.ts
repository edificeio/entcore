import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {GroupPickerComponent, groupPickerLocators} from './group-picker.component';
import {Component, DebugElement, EventEmitter, Input, Output} from '@angular/core';
import {SijilModule} from 'sijil';
import {By} from '@angular/platform-browser';
import {MonoSelectComponent} from '../mono-select/mono-select.component';
import {LightBoxComponent} from '../lightbox/lightbox.component';
import {FormsModule} from '@angular/forms';
import {OrderPipe} from '../../pipes';
import { StructureModel } from '../../store/models/structure.model';
import { GroupModel } from '../../store/models/group.model';

describe('GroupPickerComponent', () => {
    let component: GroupPickerComponent;
    let fixture: ComponentFixture<GroupPickerComponent>;
    let mockListComponent: MockListComponent;
    let mockOrderPipe: OrderPipe;

    beforeEach(async(() => {
        mockOrderPipe = jasmine.createSpyObj('OrderPipe', ['transform']);

        TestBed.configureTestingModule({
            declarations: [
                GroupPickerComponent,
                MockListComponent,
                MonoSelectComponent,
                LightBoxComponent
            ],
            providers: [
                {useValue: mockOrderPipe, provide: OrderPipe}
            ],
            imports: [
                SijilModule.forRoot(),
                FormsModule
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(GroupPickerComponent);

        component = fixture.debugElement.componentInstance;
        component.lightboxTitle = '';
        component.list = [];
        component.types = [];
        component.show = true;
        component.sort = '';
        component.filters = () => true;
        component.searchPlaceholder = '';
        component.noResultsLabel = '';
        component.activeStructure = {id: 'structure1', name: 'structure1'} as StructureModel;
        component.structures = [
            {id: 'structure1', name: 'structure1'} as StructureModel,
            {id: 'structure2', name: 'structure2'} as StructureModel
        ];
        (mockOrderPipe.transform as jasmine.Spy).and.returnValue(component.structures);

        fixture.detectChanges();

        mockListComponent = fixture.debugElement.query(By.directive(MockListComponent)).componentInstance;
    }));

    it('should create a GroupPickerComponent', () => {
        expect(component).toBeDefined();
    });

    it('should display ProfileGroup, FunctionalGroup, ManualGroup filter buttons', () => {
        component.types = ['ProfileGroup', 'FunctionalGroup', 'ManualGroup'];
        fixture.detectChanges();
        expect(getFilterButtons(fixture).length).toBe(3);
    });

    it(`should emit a 'pick' event when the list onSelect event is emitted with the given group`, () => {
        let result: GroupModel = {id: '', name: ''} as GroupModel;
        const givenGroup: GroupModel = {id: 'group1', name: 'group1'} as GroupModel;

        component.pick.subscribe((event: GroupModel) => result = event);
        mockListComponent.onSelect.emit(givenGroup);
        expect(result.id).toBe('group1');
    });
});

function getFilterButtons(fixture: ComponentFixture<GroupPickerComponent>): DebugElement[] {
    return fixture.debugElement.queryAll(By.css(groupPickerLocators.filterButton));
}

@Component({
    selector: 'list',
    template: ''
})
class MockListComponent {
    @Input()
    model: GroupModel[] = [];

    @Input()
    sort = '';

    @Input()
    searchPlaceholder = '';

    @Input()
    noResultsLabel = '';

    @Output()
    onSelect: EventEmitter<{}> = new EventEmitter();

    @Input()
    filters: () => boolean = () => true

    @Input()
    inputFilter: () => boolean = () => true

}
