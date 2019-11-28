import {Component, EventEmitter, Input, OnChanges, OnInit, Output} from '@angular/core';
import {SelectOption} from '../multi-select/multi-select.component';
import {OrderPipe} from '../../pipes';
import { GroupModel, GroupType } from 'src/app/core/store/models/group.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';

const css = {
    filterButton: 'lct-filters'
};

export const groupPickerLocators = {
    filterButton: `.${css.filterButton}`
};

@Component({
    selector: 'ode-group-picker',
    templateUrl: './group-picker.component.html'
})
export class GroupPickerComponent implements OnInit, OnChanges {

    @Input() lightboxTitle: string;
    @Input() list: GroupModel[];
    @Input() types: GroupType[];
    @Input() show: boolean;
    @Input() sort: string;
    @Input() filters: () => boolean;
    @Input() searchPlaceholder: string;
    @Input() noResultsLabel: string;
    @Input() activeStructure: StructureModel;
    @Input() structures: StructureModel[] = [];

    @Output() close: EventEmitter<any> = new EventEmitter();
    @Output() pick: EventEmitter<GroupModel> = new EventEmitter<GroupModel>();
    @Output() inputChange: EventEmitter<string> = new EventEmitter<string>();
    @Output() structureChange: EventEmitter<StructureModel> = new EventEmitter<StructureModel>();

    initialList: GroupModel[];
    groupInputFilter: string;
    visibleGroupType: GroupType[] = [];

    selectedStructure: StructureModel;
    structureOptions: SelectOption<StructureModel>[] = [];

    constructor(private orderPipe: OrderPipe) {
    }

    ngOnInit(): void {
        this.initialList = [...this.list];
        this.selectedStructure = this.activeStructure;
        this.structureOptions = this.orderPipe.transform(this.structures, '+name')
            .map(structure => ({value: structure, label: structure.name}));
    }

    ngOnChanges(): void {
        this.initialList = [...this.list];
    }

    filterByInput = (group: GroupModel) => {
        if (!this.groupInputFilter) { return true; }
        return group.name.toLowerCase().indexOf(this.groupInputFilter.toLowerCase()) >= 0;
    }

    filterByType = (type: GroupType) => {
        if (this.visibleGroupType.includes(type)) {
            this.visibleGroupType.splice(this.visibleGroupType.indexOf(type), 1);
        } else {
            this.visibleGroupType.push(type);
        }

        if (this.visibleGroupType && this.visibleGroupType.length > 0) {
            this.list = this.initialList.filter(g => this.visibleGroupType.includes(g.type));
        } else {
            this.list = this.initialList;
        }
    }

    onClose(): void {
        this.visibleGroupType = [];
        this.list = this.initialList;
        this.close.emit();
    }

    public onStructureChange($event: StructureModel) {
        this.structureChange.emit($event);
        this.visibleGroupType = [];
    }
}
