import {UserModel} from '../store';
import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';

@Injectable()
export class UserListService {
    set inputFilter(filter: string) {
        this._inputFilter = filter;
        this.resetLimit();
    }

    get inputFilter() {
        return this._inputFilter;
    }
    DEFAULT_INCREMENT = 100;
    limit = this.DEFAULT_INCREMENT;

    // Subject: used to notify user list to refresh when a user is updated
    $updateSubject: Subject<any> = new Subject<any>();

    // Sorts
    sortsMap = {
        alphabetical: {
            sort: '+',
            orderedValue: 'lastName',
            staticValues: ['+firstName'],
            selected: true
        },
        profile: {
            sort: '+',
            orderedValue: 'type',
            selected: false
        }
    };

    sorts: Array<string> = ['+lastName', '+firstName', '+type'];

    // Filters
    private _inputFilter = '';

    changeSorts(target) {
        this.resetLimit();
        this.sortsMap[target].selected = true;
        this.sortsMap[target].sort = this.sortsMap[target].sort === '+' ? '-' : '+';
        this.sorts = [
            this.sortsMap[target].sort + this.sortsMap[target].orderedValue,
            ...(this.sortsMap[target].staticValues || [])];

        for (const prop in this.sortsMap) {
            if (prop !== target) {
                this.sorts = this.sorts.concat([
                    this.sortsMap[prop].sort + this.sortsMap[prop].orderedValue,
                    ...(this.sortsMap[prop].staticValues || [])]);
                this.sortsMap[prop].selected = false;
            }
        }
    }

    filterByInput = (user: UserModel) => {
        if (!this.inputFilter) { return true; }
        return `${user.displayName}`.toLowerCase()
            .indexOf(this.inputFilter.trim().toLowerCase()) >= 0;
    }

    resetLimit() {
        this.limit = this.DEFAULT_INCREMENT;
    }

    addPageDown() {
        this.limit = this.limit + this.DEFAULT_INCREMENT;
    }
}

@Injectable()
export class UserChildrenListService extends UserListService {
}
