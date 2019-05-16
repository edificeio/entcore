import { MassRoleAssignment } from './mass-role-assignment.component';
import { Profile, Role, MassAssignment } from '../../../shared/services-types';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { By } from '@angular/platform-browser';
import { SijilModule } from 'sijil';
import { MultiSelectComponent } from '../../../../shared/ux/components';

describe('MassRoleAssignment', () => {
    let component: MassRoleAssignment;
    let fixture: ComponentFixture<MassRoleAssignment>;
    const allRoles: Array<Role> = [{id: 'role1', name: 'role1'}, {id: 'role2', name: 'role2'}];
    const allProfiles: Array<Profile> = ['Guest', 'Personnel', 'Relative', 'Student', 'Teacher'];
    let assignmentLightbox: MockLightboxConfirmComponent;
    let unassignmentLightbox: MockLightboxConfirmComponent;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                MassRoleAssignment,
                MockLightboxConfirmComponent,
                MultiSelectComponent
            ],
            providers: [],
            imports: [
                SijilModule.forRoot(),
                ReactiveFormsModule
            ]

        }).compileComponents();
        fixture = TestBed.createComponent(MassRoleAssignment);
        component = fixture.debugElement.componentInstance;
        component.profiles = allProfiles;
        component.roles = allRoles;
        component.structure = {id: 'myStructure', name: 'my structure'};
        fixture.detectChanges();
        const lightboxes = fixture.debugElement.queryAll(By.directive(MockLightboxConfirmComponent));
        assignmentLightbox = lightboxes[0].componentInstance;
        unassignmentLightbox = lightboxes[1].componentInstance;
    }));

    it('should create a MassRoleAssignment component', () => {
        expect(component).toBeTruthy();
    });

    it('should display a multi-select with the given roles', () => {
        expect(component.roleOptions.length).toBe(allRoles.length);
    });

    it('should display a select with the given profiles', () => {
        expect(component.profileOptions.length).toBe(allProfiles.length);
    });

    describe('assign', () => {
        it(`should emit a 'submitAssignment' event with the given roles, profiles and current structureId`, () => {
            let result: MassAssignment = {structure: {id: '', name: ''}, profiles: [], roles: []};
            component.submitAssignment.subscribe((event: MassAssignment) => result = event);
            component.assign([{id: 'role1', name: 'role1'}], ['Student', 'Guest']);
            expect(result.structure.id).toBe('myStructure');
            expect(result.roles).toEqual([{id: 'role1', name: 'role1'}]);
            expect(result.profiles).toEqual(['Student', 'Guest']);
        });
    });

    describe('unassign', () => {
        it(`should emit a 'submitUnassignment' event with the given roles, profiles and current structureId`, () => {
            let result: MassAssignment = {structure: {id: '', name: ''}, profiles: [], roles: []};
            component.submitUnassignment.subscribe((event: MassAssignment) => result = event);
            component.unassign([{id: 'role1', name: 'role1'}], ['Student', 'Guest']);
            expect(result.structure.id).toBe('myStructure');
            expect(result.roles).toEqual([{id: 'role1', name: 'role1'}]);
            expect(result.profiles).toEqual(['Student', 'Guest']);
        });
    });

    it(`should emit a 'submitAssignment' event, when clicking on the assign button and confirm tooltip`, () => {
        let result: MassAssignment = {structure: {id: '', name: ''}, profiles: [], roles: []};
        component.submitAssignment.subscribe((event: MassAssignment) => result = event);
        component.assignmentForm.get('roles').setValue([{id: 'role1', name: 'role1'}]);
        component.assignmentForm.get('profiles').setValue(['Student']);
        fixture.detectChanges();
        fixture.nativeElement.querySelector('.lct-assign-button').click();
        expect(component.displayedLightbox).toBe('assignment');
        assignmentLightbox.onConfirm.emit();
        expect(result.structure.id).toBe('myStructure');
        expect(result.roles).toEqual([{id: 'role1', name: 'role1'}]);
        expect(component.displayedLightbox).toBe('none');
    });

    it(`should emit a 'submitUnassignment' event, when clicking on the unassign button and confirm tooltip`, () => {
        let result: MassAssignment = {structure: {id: '', name: ''}, profiles: [], roles: []};
        component.submitUnassignment.subscribe((event: MassAssignment) => result = event);
        component.assignmentForm.get('roles').setValue([{id: 'role1', name: 'role1'}]);
        component.assignmentForm.get('profiles').setValue(['Student']);
        fixture.detectChanges();
        fixture.nativeElement.querySelector('.lct-unassign-button').click();
        expect(component.displayedLightbox).toBe('unassignment');
        unassignmentLightbox.onConfirm.emit();
        expect(result.structure.id).toBe('myStructure');
        expect(result.roles).toEqual([{id: 'role1', name: 'role1'}]);
        expect(component.displayedLightbox).toBe('none');
    });
});

@Component({
    selector: 'lightbox-confirm',
    template: ``
})
class MockLightboxConfirmComponent {
    @Input('title')
    title: string;
    @Input('show')
    show: boolean;
    @Output('onConfirm')
    onConfirm: EventEmitter<void> = new EventEmitter<void>();
    @Output('onCancel')
    onCancel: EventEmitter<void> = new EventEmitter<void>();
}
