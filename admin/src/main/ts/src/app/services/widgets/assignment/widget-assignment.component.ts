import { Component, EventEmitter, Injector, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { NotifyService } from 'src/app/core/services/notify.service';
import { GroupModel } from 'src/app/core/store/models/group.model';
import { RoleModel } from 'src/app/core/store/models/role.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { WidgetModel } from 'src/app/core/store/models/widget.model';
import { filterWidgetsByLevelsOfEducation } from '../../_shared/services-list/services-list.component';
import { Assignment } from '../../_shared/services-types';

@Component({
    selector: 'ode-widget-assignment',
    templateUrl: 'widget-assigment.component.html'
})

export class WidgetAssignmentComponent extends OdeComponent implements OnChanges {
    @Input() 
    structure?:StructureModel;
    @Input()
    widget: WidgetModel;
    @Input()
    assignmentGroupPickerList: Array<GroupModel>;

    @Output()
    mandatoryToggle: EventEmitter<Assignment> = new EventEmitter<Assignment>();
    @Output()
    add: EventEmitter<Assignment> = new EventEmitter();
    @Output()
    remove: EventEmitter<Assignment> = new EventEmitter();

    selectedRole: RoleModel;
    showRoleAttributionLightbox = false;
    
    constructor(
            private notifyService: NotifyService,
            injector: Injector) {
        super(injector);
    }

    ngOnChanges(changes: SimpleChanges): void {
        if( changes.widget ) {
            this.checkStructureLevelOfEducation();
        }
    }

    private checkStructureLevelOfEducation(): void {
        if( this.structure && filterWidgetsByLevelsOfEducation([this.widget], this.structure.levelsOfEducation).length === 0 ) {
            this.notifyService.notify(
                'services.widget-assignment.level-error.content', 
                'services.widget-assignment.level-error.title', 
                '', 
                'warning', {
                    layout:'center',
                    timeout: 600
                }
            );
        }
}    

    public openRoleAttributionLightbox(role: RoleModel) {
        this.selectedRole = role;
        this.showRoleAttributionLightbox = true;
    }
}