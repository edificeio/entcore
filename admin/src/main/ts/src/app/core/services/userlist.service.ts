import {UserModel} from '../store/models/user.model';
import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';

@Injectable()
export class UserListService {
    set inputFilter(filter: string | string[]) {
        if(filter instanceof String) {
            this._inputFilter = this.normalize(filter as string);
        } else {
            this._inputFilter = (filter as string[])
                                    .map( s => this.normalize(s));
        }         
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
    private _inputFilter: string | string[] = '';

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
        if(this.inputFilter instanceof String) {
            return `${user.displayName}`.toLowerCase()
                .indexOf((this.inputFilter as String).trim().toLowerCase()) >= 0;
        } else {
            return this.normalize(`${user.firstName}`).indexOf((this.inputFilter as string[])[0]) >= 0
                && this.normalize(`${user.lastName}`).indexOf((this.inputFilter as string[])[1]) >= 0;
        }        
    }

    resetLimit() {
        this.limit = this.DEFAULT_INCREMENT;
    }

    addPageDown() {
        this.limit = this.limit + this.DEFAULT_INCREMENT;
    }

    private normalize(s: string): string {
        return s.normalize("NFD")
                      .replace(/[\u0300-\u036f-]/g, "")                      
                      .toLowerCase();
    }
}

@Injectable()
export class UserChildrenListService extends UserListService {
}
