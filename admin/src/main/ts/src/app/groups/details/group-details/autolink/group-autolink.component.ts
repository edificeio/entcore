import { HttpErrorResponse } from "@angular/common/http";
import { Component, Injector, Input } from "@angular/core";
import { OdeComponent } from "ngx-ode-core";
import { NotifyService } from "src/app/core/services/notify.service";
import { GroupModel } from "src/app/core/store/models/group.model";
import { StructureModel } from "src/app/core/store/models/structure.model";
import { GroupsService, GroupUpdatePayload } from "src/app/groups/groups.service";

type AutolinkFormModel = {
    autolinkStructures: string;
    autolinkProfile: string;
    autolinkAdml: boolean;
    autolinkTeachers: string;
    autolinkPersonnel: string;
    disciplines: Array<string>;
    selectedDisciplines: Array<string>;
    functions: Array<string>;
    selectedFunctions: Array<string>;
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

    public autolinkFormModel: AutolinkFormModel;

    public autolinkSubStructureIds: Array<string>;
    public lightboxSubStructureIds: Array<string> = [];
    public showSubStructutresPickerButton: boolean;
    public showSubStructuresLightbox: boolean;
    public showTeachersSubSection: boolean;
    public showPersonnelSubSection: boolean;
    public showDisciplinesPicker: boolean;
    public showFunctionsPicker: boolean;
    // hack for AOT build (used for this.checked on radio onclick)
    public checked: boolean;

    constructor(
        private notifyService: NotifyService,
        private groupsService: GroupsService,
        injector: Injector
    ) {
        super(injector);
    }

    ngOnInit(): void {
        this.autolinkFormModel = {
            autolinkStructures: '',
            autolinkProfile: '',
            autolinkAdml: false,
            autolinkTeachers: '',
            autolinkPersonnel: '',
            disciplines: this.structure.groups.data.filter(g => g.labels && g.labels.includes('DisciplineGroup')).map(g => g.filter),
            selectedDisciplines: [],
            functions: this.structure.groups.data.filter(g => g.labels && g.labels.includes('FuncGroup')).map(g => g.filter),
            selectedFunctions: []
        }
    }


    public onAutolinkSubmit() {
        const groupUpdatePayload: GroupUpdatePayload = {
            name: this.group.name,
            autolinkTargetAllStructs: this.autolinkFormModel.autolinkStructures === 'autolinkSubStructures'? true: false,
            autolinkTargetStructs: this.autolinkSubStructureIds || [],
            autolinkUsersFromGroups: new Array(this.autolinkFormModel.autolinkProfile) || []
        };

        // populate autolinkUsersFromGroups from profiles selection
        if (this.autolinkFormModel.autolinkAdml) {
            groupUpdatePayload.autolinkUsersFromGroups.push('AdminLocal');
        }
        if (this.autolinkFormModel.autolinkProfile === 'Teacher' && 
            this.autolinkFormModel.autolinkTeachers === 'autolinkHeadTeachers') {
            groupUpdatePayload.autolinkUsersFromGroups.push('HeadTeacher');
        }
        if (this.autolinkFormModel.autolinkProfile === 'Teacher' && 
            this.autolinkFormModel.autolinkTeachers === 'autolinkDisciplines' &&
            this.autolinkFormModel.selectedDisciplines && 
            this.autolinkFormModel.selectedDisciplines.length > 0) {
            groupUpdatePayload.autolinkUsersFromGroups = groupUpdatePayload.autolinkUsersFromGroups.concat(this.autolinkFormModel.selectedDisciplines);
        }
        if (this.autolinkFormModel.autolinkProfile === 'Personnel' &&
            this.autolinkFormModel.autolinkPersonnel === 'autolinkFunctions' &&
            this.autolinkFormModel.selectedFunctions && 
            this.autolinkFormModel.selectedFunctions.length > 0) {
            groupUpdatePayload.autolinkUsersFromGroups = groupUpdatePayload.autolinkUsersFromGroups.concat(this.autolinkFormModel.selectedFunctions);
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
        this.lightboxSubStructureIds = Object.assign([], this.autolinkSubStructureIds);
        this.showSubStructuresLightbox = true;
    }

    public getSubStructuresTreeItems = (): Array<StructureListItem> => {
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
    }

    public saveAndClose(): void {
        this.autolinkSubStructureIds = this.lightboxSubStructureIds;
        this.showSubStructuresLightbox = false;
    }

    public unselectDiscipline(item: string): void {
        this.autolinkFormModel.selectedDisciplines.splice(this.autolinkFormModel.selectedDisciplines.indexOf(item), 1);
    }

    public unselectFunction(item: string): void {
        this.autolinkFormModel.selectedFunctions.splice(this.autolinkFormModel.selectedFunctions.indexOf(item), 1);
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
}