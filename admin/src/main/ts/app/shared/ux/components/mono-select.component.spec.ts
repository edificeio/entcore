import { MonoSelectComponent, monoSelectLocators as locators } from './mono-select.component';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { SijilModule } from 'sijil';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { Component } from '@angular/core';
import { SelectOption } from './multi-select.component';

describe('MonoSelectComponent', () => {
    let component: MonoSelectComponent<any>;
    let form: MockFormComponent<any>;
    let fixture: ComponentFixture<MockFormComponent<any>>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                MonoSelectComponent,
                MockFormComponent
            ],
            imports: [
                SijilModule.forRoot(),
                FormsModule
            ]

        }).compileComponents();
        fixture = TestBed.createComponent(MockFormComponent);
        form = fixture.debugElement.componentInstance;
        fixture.detectChanges();
        component = fixture.debugElement.query(By.directive(MonoSelectComponent)).componentInstance;
    }));

    it('should create a MultiSelectComponent', () => {
        expect(component).toBeTruthy();
    });

    it('should display the given options', () => {
        component.options = [{label: '1', value: 1}, {label: '2', value: 2}];
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelectorAll(locators.option).length).toBe(2);
    });
});

@Component({
    selector: `mock-form`,
    template: `
        <mono-select [(ngModel)]="model"
                     [options]="options"></mono-select>`
})
class MockFormComponent<K> {
    model: any;
    options: Array<SelectOption<K>> = [];
}