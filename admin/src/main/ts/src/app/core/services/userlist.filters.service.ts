import {Injectable} from '@angular/core';
import {Subject} from 'rxjs';
import { UserPosition } from '../store/models/userPosition.model';

export abstract class UserFilter<T> {

    constructor(protected observable: Subject<void>) {
    }

    abstract type: string;
    abstract label: string;
    abstract comboModel: Array<T>;
    abstract filter: Array<T> | ((...items: any[]) => boolean);

    protected _outputModel: Array<T> = [];
    set outputModel(model: Array<T>) {
        this._outputModel = model;
        this.observable.next();
    }
    get outputModel() {
        return this._outputModel;
    }
    protected defaultModel: Array<T> = [];
    public reset() {
        this.outputModel = this.defaultModel;
    }

    display?: string;
    order?: string;
    filterProp?: string;

    datepicker?: boolean;
}

export type UserFilterList<T> = UserFilter<T>[];

class ProfileFilter extends UserFilter<string> {
    type = 'type';
    label = 'profiles.multi.combo.title';
    comboModel = [];
    order = '-';

    filter = (type: string) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 || outputModel.indexOf(type) >= 0;
    }
}

class ActivationFilter extends UserFilter<string> {
    type = 'code';
    label = 'code.multi.combo.title';
    comboModel = [ 'users.activated', 'users.not.activated' ];

    filter = (code: string) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 ||
            outputModel.indexOf('users.activated') >= 0 && !code ||
            outputModel.indexOf('users.not.activated') >= 0 && !(!code);
    }
}

class ClassesFilter extends UserFilter<{id: string, name: string}> {
    type = 'classes';
    label = 'classes.multi.combo.title';
    comboModel = [];
    display = 'name';
    order = '+name';
    filterProp = 'name';

    filter = (classes: {id: string, name: string}[]) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 ||
            classes && classes.length > 0 &&
            classes.some(c => {
                return outputModel.some(o => o.id === c.id);
            });
    }
}

class SourcesFilter extends UserFilter<string> {
    type = 'source';
    label = 'sources.multi.combo.title';
    comboModel = [];
    order = '+';
    filterProp = 'this';

    filter = (source: string) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 || outputModel.indexOf(source) >= 0;
    }
}

class FunctionsFilter extends UserFilter<Array<string>> {
    type = 'aafFunctions';
    label = 'functions.multi.combo.title';
    comboModel: Array<Array<string>> = [];
    order = '+';
    filterProp = 'this';

    filter = (functions: Array<Array<string>>) => {
        const outputModel = this.outputModel;
        let res = false;

        if (outputModel.length === 0) {
            return true;
        } else if (functions && functions.length > 0 && outputModel && outputModel.length > 0) {
            functions.forEach(f => {
                outputModel.forEach(o => {
                    if (o.includes(f[2]) && o.includes(f[4])) {
                        res = true;
                    }
                });
            });
        }

        return res;
    }
}

class FunctionalGroupsFilter extends UserFilter<string> {
    type = 'functionalGroups';
    label = 'functionalGroups.multi.combo.title';
    comboModel = [];
    order = '+';
    filterProp = 'this';

    filter = (fgroups: string[]) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 ||
            fgroups && fgroups.length > 0 &&
            fgroups.some(f => {
                return outputModel.some(o => o === f);
            });
    }
}

class ManualGroupsFilter extends UserFilter<string> {
    type = 'manualGroups';
    label = 'ManualGroup';
    comboModel = [];
    order = '+';
    filterProp = 'this';

    filter = (mgroups: string[]) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 ||
            mgroups && mgroups.length > 0 &&
            mgroups.some(g => {
                return outputModel.some(o => o === g);
            });
    }
}

class DuplicatesFilter extends UserFilter<string> {
    type = 'duplicates';
    label = 'duplicates.multi.combo.title';
    comboModel = [ 'users.duplicated', 'users.not.duplicated' ];
    order = '';
    filterProp = 'this';

    filter = (duplicates: {}[]) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 ||
            outputModel.indexOf('users.duplicated') >= 0 && duplicates.length > 0 ||
            outputModel.indexOf('users.not.duplicated') >= 0 && duplicates.length === 0;
    }
}

class MailFilter extends UserFilter<string> {
    type = 'email';
    label = 'email';
    comboModel = ['users.with.mail', 'users.without.mail'];

    filter = (mail: string) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 ||
            outputModel.indexOf('users.without.mail') >= 0 && !mail ||
            outputModel.indexOf('users.with.mail') >= 0 && !(!mail);
    }
}

class DateFilter extends UserFilter<{date: Date, comparison: string}> {
    type = 'creationDate';
    label = 'creation.date';
    comboModel = [{date: undefined, comparison: 'users.before'}, {date: undefined, comparison: 'users.since'}];
    display = 'comparison';
    order = '+date';
    filterProp = 'date';
    datepicker = true;

    filter = (date: string) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0 ||
        outputModel.some(o => (typeof o.date === 'undefined') || isNaN(o.date.valueOf()) ||
        (o.comparison === 'users.before' ? Date.parse(o.date.toISOString()) >= Date.parse(date)
        : Date.parse(o.date.toISOString()) <= Date.parse(date)));
    }
}

class AdmlFilter extends UserFilter<string> {
    type = 'functions';
    label = 'adml.multi.combo.title';
    comboModel = ['users.adml', 'users.not.adml'];
    order = '+';
    filterProp = 'this';

    filter = (functions: [string, Array<string>]) => {
        const outputModel = this.outputModel;
        const currentStructureId = window.location.href.split('/')[4];
        return outputModel.length === 0
            || outputModel.indexOf('users.adml') >= 0
                && functions.findIndex((f) => f[0] === 'ADMIN_LOCAL') >= 0
                && functions.findIndex((f) => f[1].includes(currentStructureId)) >= 0
            || outputModel.indexOf('users.not.adml') >= 0
                && functions.findIndex((f) => f[0] === 'ADMIN_LOCAL') === -1;
    }
}

export class DeleteFilter extends UserFilter<string> {
    type = "deleteDate, disappearanceDate";
    label = "delete.multi.combo.title";
    comboModel = ["users.deleted", "users.waiting.deleted", "users.not.deleted"];
    defaultModel = ["users.waiting.deleted", "users.not.deleted"];
    order = "+";
    filterProp = "this";

    filter = (deleteDate: string, disappearanceDate: string) => {
        const outputModel = this.outputModel;
        return outputModel.length == 0
            || outputModel.indexOf("users.deleted") != -1 && deleteDate != null
            || outputModel.indexOf("users.waiting.deleted") != -1 && disappearanceDate != null && deleteDate == null
            || outputModel.indexOf("users.not.deleted") != -1 && deleteDate == null && disappearanceDate == null;
    };
};

class BlockedFilter extends UserFilter<string> {
    type = 'blocked';
    label = 'blocked.multi.combo.title';
    comboModel = [ 'users.blocked', 'users.not.blocked' ];

    filter = (blocked: boolean) => {
        const outputModel = this.outputModel;
        return outputModel.length === 0
            || outputModel.indexOf('users.blocked') >= 0 && blocked
            || outputModel.indexOf('users.not.blocked') >= 0 && !blocked;
    }
}

class PositionFilter extends UserFilter<{id:string}> {
    type = 'userPositions';
    label = 'userPositions.multi.combo.title';
    display = 'name';
    comboModel = [];
    order = '+name';
    filterProp = 'name';

    filter = (userPositions: Array<UserPosition>) => {
        const outputModel = this.outputModel;

        return outputModel.length === 0 ||
            userPositions && userPositions.length > 0 &&
            userPositions.some(f => {
                return outputModel.some(o => o.id === f.id);
            });
    }
}

@Injectable()
export class UserlistFiltersService {

    constructor() {}

    $updateSubject: Subject<any> = new Subject<any>();

    private profileFilter = new ProfileFilter(this.$updateSubject);
    private classesFilter = new ClassesFilter(this.$updateSubject);
    private functionalGroupsFilter = new FunctionalGroupsFilter(this.$updateSubject);
    private manualGroupsFilter = new ManualGroupsFilter(this.$updateSubject);
    private activationFilter = new ActivationFilter(this.$updateSubject);
    private functionsFilter = new FunctionsFilter(this.$updateSubject);
    private sourcesFilter = new SourcesFilter(this.$updateSubject);
    private duplicatesFilter = new DuplicatesFilter(this.$updateSubject);
    private mailFilter = new MailFilter(this.$updateSubject);
    private admlFilter = new AdmlFilter(this.$updateSubject);
    private dateFilter = new DateFilter(this.$updateSubject);
    private deleteFilter = new DeleteFilter(this.$updateSubject);
    private blockedFilter = new BlockedFilter(this.$updateSubject);
    private positionFilter = new PositionFilter(this.$updateSubject);

    filters: UserFilterList<any> = [
        this.profileFilter,
        this.classesFilter,
        this.functionalGroupsFilter,
        this.manualGroupsFilter,
        this.activationFilter,
        this.functionsFilter,
        this.sourcesFilter,
        this.duplicatesFilter,
        this.mailFilter,
        this.admlFilter,
        this.dateFilter,
        this.deleteFilter,
        this.blockedFilter,
        this.positionFilter
    ];

    resetFilters() {
        for (const filter of this.filters) {
            filter.reset();
        }
    }

    setProfilesComboModel(combos: string[]) {
        this.profileFilter.comboModel = combos;
    }

    setClassesComboModel(combos: {id: string, name: string}[]) {
        this.classesFilter.comboModel = combos;
    }

    setSourcesComboModel(combos: string[]) {
        this.sourcesFilter.comboModel = combos;
    }

    setFunctionsComboModel(combos: Array<Array<string>>) {
        this.functionsFilter.comboModel = combos;
    }

    setFunctionalGroupsComboModel(combos: string[]) {
        this.functionalGroupsFilter.comboModel = combos;
    }

    setManualGroupsComboModel(combos: string[]) {
        this.manualGroupsFilter.comboModel = combos;
    }

    setDuplicatesComboModel(combos: string[]) {
        this.duplicatesFilter.comboModel = combos;
    }

    setMailsComboModel(combos: string[]) {
        this.mailFilter.comboModel = combos;
    }

    setAdmlComboModel(combos: string[]) {
        this.admlFilter.comboModel = combos;
    }

    setDateComboModel(combos: {date: Date, comparison: string}[]) {
        this.dateFilter.comboModel = combos;
    }

    setBlockedComboModel(combos: string[]) {
        this.blockedFilter.comboModel = combos;
    }

    setPositionComboModel(combos: UserPosition[]) {
        this.positionFilter.comboModel = combos;
    }

    getFormattedFilters(customFilters?: UserFilter<any> | UserFilterList<any>): any {
        const formattedFilters = {};
        for (const filter of this.filters) {
            formattedFilters[filter.type] = filter.filter;
        }
        if (customFilters != null) {
            if (Array.isArray(customFilters) === false) {
                customFilters = [ customFilters as UserFilter<any> ];
            }

            customFilters = customFilters as UserFilterList<any>;
            for (const filter of customFilters) {
                formattedFilters[filter.type] = filter.filter;
            }
        }
        return formattedFilters;
    }

    getFormattedOutputModels(customFilters?: UserFilter<any> | UserFilterList<any>): any {
        const outputModels = {};
        for (const filter of this.filters) {
            outputModels[filter.type] = filter.outputModel;
        }
        if (customFilters != null) {
            if (Array.isArray(customFilters) === false) {
                customFilters = [ customFilters as UserFilter<any> ];
            }

            customFilters = customFilters as UserFilterList<any>;
            for (const filter of customFilters) {
                outputModels[filter.type] = filter.outputModel;
            }
        }
        return outputModels;
    }
}
