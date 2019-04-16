import { ComponentFixture, async, TestBed } from "@angular/core/testing";
import { GroupPickerComponent } from "./group-picker.component";
import { Component, Input, DebugElement, Output, EventEmitter } from "@angular/core";
import { SijilModule } from "sijil";
import { GroupModel } from "../../../core/store";
import { groupPickerLocators } from './group-picker.component';
import { By } from "@angular/platform-browser";

describe('GroupPickerComponent', () => {
    let component: GroupPickerComponent;
    let fixture: ComponentFixture<GroupPickerComponent>;
    let mockListComponent: MockListComponent;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                GroupPickerComponent,
                MockLightboxComponent,
                MockListComponent
            ],
            providers: [],
            imports: [
                SijilModule.forRoot()
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(GroupPickerComponent);
        
        component = fixture.debugElement.componentInstance;
        component.title = '';
        component.list = [];
        component.types = [];
        component.show = false;
        component.sort = '';
        component.filters = () => {return true;}
        component.searchPlaceholder = '';
        component.noResultsLabel = '';
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
})

function getFilterButtons(fixture: ComponentFixture<GroupPickerComponent>): DebugElement[] {
    return fixture.debugElement.queryAll(By.css(groupPickerLocators.filterButton));
}

@Component({
    selector: 'lightbox',
    template: '<ng-content></ng-content>'
})
class MockLightboxComponent {
    @Input()
    show: boolean = false;
    
}

@Component({
    selector: 'list',
    template: ''
})
class MockListComponent {
    @Input()
    model: GroupModel[] = [];

    @Input()
    sort: string = '';

    @Input()
    filters: () => boolean = () => true;

    @Input()
    inputFilter: () => boolean = () => true;

    @Input() 
    searchPlaceholder: string = '';

    @Input() 
    noResultsLabel: string = '';

    @Output() 
    onSelect: EventEmitter<{}> = new EventEmitter()

}