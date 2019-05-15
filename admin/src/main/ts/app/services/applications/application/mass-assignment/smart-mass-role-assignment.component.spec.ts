import { SmartMassRoleAssignment } from './smart-mass-role-assignment.component';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Assignment, Profile, Role, Structure, MassAssignment } from '../../../shared/assignment-types';
import { By } from '@angular/platform-browser';
import { ServicesStore } from '../../../services.store';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import { NotifyService } from '../../../../core/services';

describe('SmartMassRoleAssignment', () => {
    let component: SmartMassRoleAssignment;
    let fixture: ComponentFixture<SmartMassRoleAssignment>;
    let httpController: HttpTestingController;
    let massRoleAssignmentComponent: MockMassRoleAssignment;
    let servicesStore: ServicesStore;
    let activatedRoute: ActivatedRoute;
    let mockNotifyService: NotifyService;

    beforeEach(() => {
        servicesStore = {structure: {id: 'myStructure'}} as ServicesStore;
        activatedRoute = {
            data: Observable.of({
                roles: [
                    {id: 'myRole1', name: 'myRole1', transverse: false},
                    {id: 'myRole2', name: 'myRole2', transverse: false},
                    {id: 'myRole3', name: 'myRole3', transverse: true}
                ]
            })
        } as ActivatedRoute;
        mockNotifyService = jasmine.createSpyObj('NotifyService', ['success', 'error']);
    });

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [
                SmartMassRoleAssignment,
                MockMassRoleAssignment
            ],
            providers: [
                {provide: NotifyService, useValue: mockNotifyService},
                {provide: ServicesStore, useValue: servicesStore},
                {provide: ActivatedRoute, useValue: activatedRoute}
            ],
            imports: [
                HttpClientTestingModule
            ]

        }).compileComponents();
        fixture = TestBed.createComponent(SmartMassRoleAssignment);
        component = fixture.debugElement.componentInstance;
        httpController = TestBed.get(HttpTestingController);
        fixture.detectChanges();
        massRoleAssignmentComponent = fixture.debugElement.query(By.directive(MockMassRoleAssignment)).componentInstance;
    }));

    it('should create a SmartMassRoleAssignment component', () => {
        expect(component).toBeTruthy();
    });
    it('should keep only non-transverse roles', () => {
        expect(component.roles).toEqual([
            {id: 'myRole1', name: 'myRole1'},
            {id: 'myRole2', name: 'myRole2'},
        ]);
    });

    describe('assign', () => {
        it('should call PUT /appregistry/structures/myStructure/roles', () => {
            component.assign({
                roles: [
                    {id: 'myRole1', name: 'myRole1'},
                    {id: 'myRole2', name: 'myRole2'}
                ],
                profiles: ['Student'],
                structure: {id: 'myStructure', name: 'my structure'}
            });
            const requestController = httpController.expectOne('/appregistry/structures/myStructure/roles').request;
            expect(requestController.method).toBe('PUT');
            expect(requestController.body).toEqual({roles: ['myRole1', 'myRole2'], profiles: ['Student']});
        });
    });

    it('should call PUT /appregistry/structures/myStructure/roles when child MassRoleAssignment emits a submitAssignment event', () => {
        massRoleAssignmentComponent.submitAssignment.emit({
            structure: {id: 'myStructure', name: 'my structure'},
            profiles: ['Guest'],
            roles: [{id: 'myRole2', name: 'myRole2'}]
        });
        const requestController = httpController.expectOne('/appregistry/structures/myStructure/roles').request;
        expect(requestController.method).toBe('PUT');
        expect(requestController.body).toEqual({roles: ['myRole2'], profiles: ['Guest']});
    });

    it('should call DELETE /appregistry/structures/myStructure/roles when child MassRoleAssignment emits a submitUnassignment event', () => {
        massRoleAssignmentComponent.submitUnassignment.emit({
            structure: {id: 'myStructure', name: 'my structure'},
            profiles: ['Guest'],
            roles: [{id: 'myRole2', name: 'myRole2'}]
        });
        const requestController = httpController.expectOne('/appregistry/structures/myStructure/roles').request;
        expect(requestController.method).toBe('DELETE');
        expect(requestController.body).toEqual({roles: ['myRole2'], profiles: ['Guest']});
    });
});


@Component({
    selector: 'mass-role-assignment',
    template: ``
})
class MockMassRoleAssignment {
    @Input()
    public structure: Structure;
    @Input()
    public roles: Array<Role>;
    @Input()
    public profiles: Array<Profile>;
    @Output()
    public submitAssignment: EventEmitter<MassAssignment> = new EventEmitter<MassAssignment>();
    @Output()
    public submitUnassignment: EventEmitter<MassAssignment> = new EventEmitter<MassAssignment>();
}
