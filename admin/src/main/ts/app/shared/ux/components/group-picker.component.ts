import { Component, Input, Output, EventEmitter, OnInit, OnChanges } from "@angular/core";
import { GroupModel, GroupType, StructureModel } from "../../../core/store";
import { SelectOption } from "./multi-select.component";
import { OrderPipe } from "../pipes";

const css = {
    filterButton: 'lct-filters'
}

export const groupPickerLocators = {
    filterButton: `.${css.filterButton}`
}

@Component({
    selector: 'group-picker',
    template: `
        <lightbox [show]="show" (onClose)="onClose()" class="inner-list">
            <div class="padded">
                <h3>{{ lightboxTitle | translate }}</h3>

                <div *ngIf="structureOptions && structureOptions.length > 1" class="structures has-vertical-margin-10">
                    <mono-select name="structure"
                                 [ngModel]="selectedStructure" 
                                 [options]="structureOptions"
                                 (ngModelChange)="onStructureChange($event)">
                    </mono-select>
                </div>

                <div class="filters">
                    <button *ngFor="let type of types; let last = last"
                        (click)="filterByType(type)"
                        class="${css.filterButton}"
                        [class.selected]="visibleGroupType.includes(type)"
                        [class.has-right-margin-5]="!last">
                        {{ type | translate }} <i class="fa fa-filter is-size-5"></i>
                    </button>
                </div>

                <form>
                    <list
                        [model]="list"
                        [sort]="sort"
                        [filters]="filters"
                        [inputFilter]="filterByInput"
                        [searchPlaceholder]="searchPlaceholder"
                        [noResultsLabel]="noResultsLabel"
                        (inputChange)="groupInputFilter = $event"
                        (onSelect)="pick.emit($event)">
                        <ng-template let-item>
                            <div>{{ item.name }}</div>
                        </ng-template>
                    </list>
                </form>
            </div>
        </lightbox>
    `
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
        if (!this.groupInputFilter) return true;
        return group.name.toLowerCase().indexOf(this.groupInputFilter.toLowerCase()) >= 0;
    };

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
    };

    onClose(): void {
        this.visibleGroupType = [];
        this.list = this.initialList;
        this.close.emit();
    };

    public onStructureChange($event: StructureModel) {
        this.structureChange.emit($event);
        this.visibleGroupType = [];
    }
}
