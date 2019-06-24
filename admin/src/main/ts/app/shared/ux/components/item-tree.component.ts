import { Component, Input, Output, EventEmitter,
    ViewChild, OnInit, ChangeDetectorRef } from '@angular/core'

@Component({
    selector: 'item-tree',
    template: `
    <ul [ngClass]="{ flattened: isFlattened() }">
        <li *ngFor="let item of (items | flattenObjArray: flatten | filter: filter | orderBy: order:reverse)"
            [ngClass]="{ selected: isSelected(item), unfolded: !isFolded(item), parent: hasChildren(item), root: _depth === 0 }">
            <a href="javascript:void(0)" (click)="selectItem(item)" *ngIf="!checkboxMode">
                <i class="opener" (click)="toggleFold($event, item)"
                    *ngIf="!isFlattened() && hasChildren(item) && !disableOpener"></i>
                {{ display(item) }}
            </a>
            <div class="checkbox__item" *ngIf="checkboxMode">
                <input id="all" type="checkbox" [(ngModel)]="item.check" ngDefaultControl>
                <label for="all" (click)="checkItem(item)">{{display(item)}}</label>
            </div>
            <item-tree
                [items]="getChildren(item)"
                [children]="childrenProperty"
                [display]="displayProperty"
                [filter]="filter"
                [order]="order"
                [reverse]="reverse"
                [lastSelected]="_lastSelectedItem"
                [depth]="depth + 1"
                [disableOpener]="disableOpener"
                [checkboxMode]="checkboxMode"
                (onSelect)="bubbleSelect($event)"
                (onCheck)="checkItem($event)"
                *ngIf="(checkboxMode && hasChildren(item)) || (!isFlattened() && hasChildren(item) && !isFolded(item))">
            </item-tree>
        </li>
    </ul>
    `,
    styles: []
})
export class ItemTreeComponent<T> implements OnInit {

    constructor(private _changeRef: ChangeDetectorRef){}

    /**** Inputs ****/

    //Items
    @Input() items: Array<any> = []

    // Property containing the list of child objects.
    @Input("children") childrenProperty: string = "children"
    // Property to display in the list
    @Input("display") displayProperty: string = "label"
    // Filter pipe argument
    @Input() filter: (Object | string | Function) = ""
    // OrderBy pipe argument
    @Input() order: (Array<any> | string | Function) = []
    // Reverse the order pipe
    @Input() reverse: any = false
    // Flatten the tree on the specified properties
    @Input()
    set flatten(flattenProps: Array<String>) {
        if(!this.isFlattened() && flattenProps.length > 0) {
            this._selectedItem = this._lastSelectedItem
        } else if(this.isFlattened && flattenProps.length < 1) {
            this.flagIfParent()
        }
        this._flattenProps = flattenProps
    }
    get flatten() : Array<String> {
        return this._flattenProps
    }
    private _flattenProps : Array<String> = []

    // Disable the opener icon
    @Input() disableOpener : boolean = false

    // Use checkboxes instead of selecting items
    @Input() checkboxMode : boolean = false

    /**** Outputs ****/

    @Output() onSelect: EventEmitter<T> = new EventEmitter<T>()

    @Output() onCheck: EventEmitter<T> = new EventEmitter<T>()

    /**** View ****/

    @ViewChild(ItemTreeComponent) childItemTree : ItemTreeComponent<T>

    /**** Internal Logic ****/

    @Input("lastSelected") _lastSelectedItem : T
    private _selectedItem : T
    @Input("depth") _depth : number = 0
    private unfolded: T[] = []

    ngOnInit(){
        this.flagIfParent()
    }

    private selectItem(item: T) {
        this._selectedItem = item
        if(this.childItemTree)
            delete this.childItemTree._selectedItem
        let idx = this.unfolded.indexOf(item)
        if(!this.disableOpener && idx < 0) {
            this.unfolded.push(item)
        }
        this.bubbleSelect(item)
    }

    private checkItem(item: T) {
        this.onCheck.emit(item);
    }

    private bubbleSelect(item: T) {
        this._lastSelectedItem = item
        this.onSelect.emit(item)
    }

    private isSelected(item) {
        return (this.disableOpener ? this._selectedItem : this._lastSelectedItem) === item
    }

    private toggleFold(event: Event, item: T) {
        event.stopPropagation()
        let idx = this.unfolded.indexOf(item)
        if(idx < 0){
            this.unfolded.push(item)
        } else {
            this.unfolded.splice(idx, 1)
        }
    }

    private isFolded(item: T) {
        return this.disableOpener ? !this.isSelected(item) : this.unfolded.indexOf(item) < 0
    }

    private display(item) {
        return item[this.displayProperty]
    }

    private getChildren(item) {
        return item[this.childrenProperty] || []
    }

    private hasChildren(item) {
        return this.getChildren(item).length > 0
    }

    isFlattened() {
        return this._flattenProps.length
    }

    private flagIfParent() {
        if(!this._lastSelectedItem){
            return
        }

        let findSubItem = (item, target) => {
            if(item === target)
                return true
            if(this.hasChildren(item)){
                return this.getChildren(item).find(subItem => {
                     return findSubItem(subItem, target)
                 })
            }
            return false
        }

        for(let i in this.items) {
            let item = this.items[i]

            if(item === this._lastSelectedItem ||
                    findSubItem(item, this._lastSelectedItem)){
                this._selectedItem = item
                this.unfolded = [item]
                break;
            }
        }

        this._changeRef.markForCheck()
    }

}
