import { AfterViewInit, ChangeDetectionStrategy, Component, Injector, Input } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { UserModel } from 'src/app/core/store/models/user.model';
import { StructureModel } from '../../../core/store/models/structure.model';

@Component({
    selector: 'ode-user-search-card',
    templateUrl: './user-search-card.component.html',
    styleUrls: ['./user-search-card.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserSearchCardComponent extends OdeComponent implements AfterViewInit {

    loading = false;
    foundUsers: Array<{id: string, firstName: string, lastName: string}> = [];

    constructor(injector: Injector) {
        super(injector);
    }

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
                this.changeDetector.markForCheck();
            });
        } else {
            this.foundUsers = [];
        }
        this.changeDetector.markForCheck();
    }

    isDisplayNameDifferent(user: UserModel): boolean {
        return (user.lastName + ' ' + user.firstName) !== user.displayName;
    }
}
