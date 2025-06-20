import { HttpErrorResponse } from "@angular/common/http";
import { Component, Injector, Input } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { NotifyService } from "src/app/core/services/notify.service";
import { GroupModel } from "src/app/core/store/models/group.model";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { GroupsService, GroupUpdatePayload } from "src/app/groups/groups.service";

class AutolinkFormModel {
    subStructuresRadio: string;
    subStructuresIds: Array<string> = [];
    profile: string;
    teacherSubSectionRadio: string;
    personnelSubSectionRadio: string;
    selectedDisciplines: Array<string> = [];
    selectedFunctions: Array<string> = [];
    selectedUsersPositions: Array<string> = [];
}

type StructureListItem = {
    name: string;
    id: string;
    children: StructureListItem[];
    check: boolean;
}

@Component({
    selector: 'ode-group-autolink',
    templateUrl: './group-autolink.component.html',
    styleUrls: ['./group-autolink.component.scss'],
})
export class GroupAutolinkComponent extends OdeComponent {
    @Input()
    group: GroupModel;

    @Input()
    structure: StructureModel;

    @Input()
    disciplineOptions: Array<string>;
    
    @Input()
    functionOptions: Array<string>;

    @Input()
    showActions: boolean;

    @Input()
    usersPositionsOptions: Array<string>;

    public form: AutolinkFormModel;

    public lightboxSubStructureIds: Array<string> = [];
    public showSubStructutresPickerButton: boolean;
    public showSubStructuresLightbox: boolean;
    public showTeachersSubSection: boolean;
    public showPersonnelSubSection: boolean;
    public showDisciplinesPicker: boolean;
    public showFunctionsPicker: boolean;
    public showUsersPositionsPicker: boolean;
    // hack for AOT build (used for this.checked on radio onclick)
    public checked: boolean;
    public structureTreeItems: Array<StructureListItem>;

    constructor(
        private notifyService: NotifyService,
        private groupsService: GroupsService,
        injector: Injector
    ) {
        super(injector);
    }

    ngOnInit(): void {
        this.initForm();
    }

    ngOnChanges(): void {
        this.initForm();
    }

    private initForm(): void {
        this.form = new AutolinkFormModel();

        this.form.teacherSubSectionRadio = 'all';
        this.showPersonnelSubSection = false;
        this.showTeachersSubSection = false;

        if (this.group.autolinkTargetAllStructs) {
            this.form.subStructuresRadio = 'all';
        }

        if (this.group.autolinkTargetStructs && this.group.autolinkTargetStructs.length > 0) {
            this.form.subStructuresRadio = 'manual';
            this.form.subStructuresIds = this.group.autolinkTargetStructs;
            this.lightboxSubStructureIds = Object.assign([], this.form.subStructuresIds);
        }
        
        if (this.group.autolinkUsersFromGroups && this.group.autolinkUsersFromGroups.length > 0) {
            ['Teacher', 'Personnel', 'Student', 'Relative', 'AdminLocal'].forEach(profile => {
                if (this.group.autolinkUsersFromGroups.includes(profile)) {
                    this.form.profile = profile;
                }
            });

            if (this.group.autolinkUsersFromGroups.includes('HeadTeacher')) {
                this.form.teacherSubSectionRadio = 'HeadTeacher';
                this.form.profile = 'Teacher';
            }

            this.disciplineOptions.forEach(d => {
                if (this.group.autolinkUsersFromGroups.includes(d)) {
                    this.form.teacherSubSectionRadio = 'disciplines';
                    this.form.selectedDisciplines.push(d);
                    this.form.profile = 'Teacher';
                }
            });

            this.functionOptions.forEach(f => {
                if (this.group.autolinkUsersFromGroups.includes(f)) {
                    this.form.personnelSubSectionRadio = 'functionGroups';
                    this.form.selectedFunctions.push(f);
                    this.form.profile = 'Personnel';
                }
            });          
        }

        this.usersPositionsOptions.forEach(f => {
            if (this.group.autolinkUsersFromPositions.includes(f)) {
                this.form.personnelSubSectionRadio = 'usersPositions';
                this.form.selectedUsersPositions.push(f);
                this.form.profile = 'Personnel';
            }
        });
        this.structureTreeItems = this.getSubStructuresTreeItems();
    }

    public onAutolinkSubmit() {
        if (!this.showActions) {
          return;
        }
        const groupUpdatePayload: GroupUpdatePayload = {
            name: this.group.name,
            autolinkTargetAllStructs: false,
            autolinkTargetStructs: [],
            autolinkUsersFromGroups: [],
            autolinkUsersFromPositions: []
        };

        // populate subStructures information
        if (this.form.subStructuresRadio === 'all') {
            groupUpdatePayload.autolinkTargetAllStructs = true;
        } else if (this.form.subStructuresRadio === 'manual' && 
            this.form.subStructuresIds && 
            this.form.subStructuresIds.length > 0) {
            groupUpdatePayload.autolinkTargetStructs = this.form.subStructuresIds;
        }

        // populate autolinkUsersFromGroups from profiles selection
        if (this.form.profile === 'Teacher') {
            if (this.form.teacherSubSectionRadio === 'HeadTeacher') {
                groupUpdatePayload.autolinkUsersFromGroups.push('HeadTeacher');
            } else if (this.form.teacherSubSectionRadio === 'disciplines' && 
                this.form.selectedDisciplines && 
                this.form.selectedDisciplines.length > 0) {
                groupUpdatePayload.autolinkUsersFromGroups = groupUpdatePayload.autolinkUsersFromGroups.concat(this.form.selectedDisciplines);
            } else if (this.form.teacherSubSectionRadio === 'all') {
                groupUpdatePayload.autolinkUsersFromGroups.push('Teacher');
            }
        } else if (this.form.profile === 'Personnel') {
            if (this.form.personnelSubSectionRadio === 'functionGroups' &&
                this.form.selectedFunctions && 
                this.form.selectedFunctions.length > 0) {
                groupUpdatePayload.autolinkUsersFromGroups = groupUpdatePayload.autolinkUsersFromGroups.concat(this.form.selectedFunctions);
            } else if(this.form.personnelSubSectionRadio === 'usersPositions' &&
                this.form.selectedUsersPositions && 
                this.form.selectedUsersPositions.length > 0) {
                groupUpdatePayload.autolinkUsersFromPositions = groupUpdatePayload.autolinkUsersFromGroups.concat(this.form.selectedUsersPositions);
            } else {
                groupUpdatePayload.autolinkUsersFromGroups.push('Personnel');
            }
        } else {
            groupUpdatePayload.autolinkUsersFromGroups.push(this.form.profile);
        }

        this.groupsService.
            update(this.group.id, groupUpdatePayload).
            subscribe(
                () => {
                    this.notifyService.success(
                        'group.details.broadcast.autolink.notify.success.content' , 
                        'group.details.broadcast.autolink.notify.success.title'
                    );
                }, 
                (error: HttpErrorResponse) => {
                    this.notifyService.error(
                        'group.details.broadcast.autolink.notify.error.content', 
                        'group.details.broadcast.autolink.notify.error.title', 
                        error
                    );
                }
            );
    }

    public openSubStructuresLightbox() {
        this.lightboxSubStructureIds = Object.assign([], this.form.subStructuresIds);
        this.structureTreeItems = this.getSubStructuresTreeItems();
        this.showSubStructuresLightbox = true;
    }

    private getSubStructuresTreeItems = (): Array<StructureListItem> => {
        const myMap = (child: StructureModel) => {
            return {
                name: child.name,
                id: child.id,
                children: child.children && child.children.length > 0 ? child.children.map(myMap) : [],
                check: !!this.lightboxSubStructureIds && !!this.lightboxSubStructureIds.find(subId => subId === child.id)
            };
        };
        if (this.structure && this.structure.children) {
            return this.structure.children.map(myMap);
        }
        return [];
    }

    public addOrRemoveChild(child: StructureListItem): void {
        const index = this.lightboxSubStructureIds.findIndex(subId => subId === child.id);
        if (index === -1) {
            this.lightboxSubStructureIds.push(child.id);
            this.checkAllChildren(child.children);
        } else {
            this.lightboxSubStructureIds = this.lightboxSubStructureIds.slice(0, index).concat(this.lightboxSubStructureIds.slice(index + 1, this.lightboxSubStructureIds.length));
            this.uncheckAllChildren(child.children);
        }
        this.structureTreeItems = this.getSubStructuresTreeItems();
    }

    public saveAndClose(): void {
        this.form.subStructuresIds = this.lightboxSubStructureIds;
        this.showSubStructuresLightbox = false;
    }

    public unselectDiscipline(item: string): void {
      if (this.showActions) {
        this.form.selectedDisciplines.splice(this.form.selectedDisciplines.indexOf(item), 1);
      }
    }

    public unselectFunction(item: string): void {
      if (this.showActions) {
        this.form.selectedFunctions.splice(this.form.selectedFunctions.indexOf(item), 1);
      }
    }

    public unselectUsersPositions(item: string): void {
      if (this.showActions) {
        this.form.selectedUsersPositions.splice(this.form.selectedUsersPositions.indexOf(item), 1);
      } 
    }

    public handleFunctionsClick($event): void {
        if (this.form.personnelSubSectionRadio === 'functionGroups') {
            this.showFunctionsPicker = true;
        } else {
            this.form.selectedFunctions = [];
            this.showFunctionsPicker = false;
        }
    }

    public handleUsersPositionsClick($event): void {
        if (this.form.personnelSubSectionRadio === 'usersPositions') {
            this.showUsersPositionsPicker = true;
        } else {
            this.form.selectedUsersPositions = [];
            this.showUsersPositionsPicker = false;
        }
    }

    public isAllTeachersRadioChecked(): boolean {
        return this.form.profile === 'Teacher' && 
            this.form.teacherSubSectionRadio != 'HeadTeacher' &&
            this.form.teacherSubSectionRadio != 'disciplines';

    }

    private checkAllChildren(children: Array<StructureListItem>) {
        children.forEach(child => {
            child.check = true;
            if (this.lightboxSubStructureIds.findIndex(subId => subId === child.id) === -1) {
                this.lightboxSubStructureIds.push(child.id);
            }
            this.checkAllChildren(child.children);
        });
    }

    private uncheckAllChildren(children: Array<StructureListItem>) {
        children.forEach(child => {
            child.check = false;
            const index = this.lightboxSubStructureIds.findIndex(subId => subId === child.id);
            if (index !== -1) {
                this.lightboxSubStructureIds = this.lightboxSubStructureIds.slice(0, index).concat(this.lightboxSubStructureIds.slice(index + 1, this.lightboxSubStructureIds.length));
            }
            this.uncheckAllChildren(child.children);
        });
    }

    selectAll(): void {
        this.checkAllChildren(this.structureTreeItems);
    }

    unselectAll(): void {
        this.uncheckAllChildren(this.structureTreeItems);
    }
}