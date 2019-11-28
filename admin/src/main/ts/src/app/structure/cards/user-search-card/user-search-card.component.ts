import {AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, Input} from '@angular/core';
import {StructureModel} from '../../../core/store/models/structure.model';

@Component({
    selector: 'ode-user-search-card',
    templateUrl: './user-search-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserSearchCardComponent implements AfterViewInit {

    loading = false;
    foundUsers: Array<{id: string, firstName: string, lastName: string}> = [];

    constructor(private cdRef: ChangeDetectorRef) {}

    @Input() structure: StructureModel;
    private _inputValue: string;
    get inputValue() { return this._inputValue; }
    set inputValue(value) {
        this._inputValue = value;
        if (this._inputValue && !this.loading) {
            this.loading = true;
            this.structure.quickSearchUsers(this._inputValue).then(res => {
                this.foundUsers = res.data;
            }).catch(err => {
                console.error(err);
            }).then(() => {
                this.loading = false;
                this.cdRef.markForCheck();
            });
        } else {
            this.foundUsers = [];
        }
        this.cdRef.markForCheck();
    }

    ngAfterViewInit() {
        this.cdRef.markForCheck();
        this.cdRef.detectChanges();
    }
}
