import { Injectable } from '@angular/core'
import { Subject } from 'rxjs/Subject'

export abstract class UserFilter<T> {

    constructor(protected observable: Subject<void>){}

    abstract type: string
    abstract label: string
    abstract comboModel: Array<T>
    abstract filter: Array<T> | ((item: any) => boolean)

    protected _outputModel : Array<T> = []
    set outputModel(model: Array<T>) {
        this._outputModel = model
        this.observable.next()
    }
    get outputModel(){
        return this._outputModel
    }

    display?: string
    order?: string
    filterProp?: string
}

export type UserFilterList<T> = UserFilter<T>[]

class ProfileFilter extends UserFilter<string> {
    type = 'type'
    label = 'profiles.multi.combo.title'
    comboModel = []

    filter = (type: string) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 || outputModel.indexOf(type) >= 0
    }
}

class ActivationFilter extends UserFilter<string> {
    type = 'code'
    label = 'code.multi.combo.title'
    comboModel = [ 'users.activated', 'users.not.activated' ]

    filter = (code: string) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            outputModel.indexOf('users.activated') >= 0 && !code ||
            outputModel.indexOf('users.not.activated') >= 0 && !(!code)
    }
}

class ClassesFilter extends UserFilter<{id: string, name: string}> {
    type = 'classes'
    label = 'classes.multi.combo.title'
    comboModel = []
    display = 'name'
    order = '+name'
    filterProp = 'name'

    filter = (classes: {id: string, name: string}[]) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            classes && classes.length > 0 &&
            classes.some(c => {
                return outputModel.some(o => o.id === c.id)
            })
    }
}

class SourcesFilter extends UserFilter<string> {
    type = 'source'
    label = 'sources.multi.combo.title'
    comboModel = []
    order = '+'
    filterProp = 'this'

    filter = (source: string) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 || outputModel.indexOf(source) >= 0
    }
}

class FunctionsFilter extends UserFilter<string> {
    type = 'aafFunctions'
    label = 'functions.multi.combo.title'
    comboModel = []
    order = '+'
    filterProp = 'this'

    filter = (functions: string[]) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 || 
            functions && functions.length > 0 &&
            functions.some(f => {
                return outputModel.some(o => o === f)
            })
    }
}

class FunctionalGroupsFilter extends UserFilter<string> {
    type = 'functionalGroups'
    label = 'functionalGroups.multi.combo.title'
    comboModel = []
    order = '+'
    filterProp = 'this'

    filter = (fgroups: string[]) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            fgroups && fgroups.length > 0 &&
            fgroups.some(f => {
                return outputModel.some(o => o === f)
            })
    }
}

class ManualGroupsFilter extends UserFilter<string> {
    type = 'manualGroups'
    label = 'manualGroups.multi.combo.title'
    comboModel = []
    order = '+'
    filterProp = 'this'

    filter = (mgroups: string[]) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            mgroups && mgroups.length > 0 &&
            mgroups.some(g => {
                return outputModel.some(o => o === g)
            })
    }
}

class DuplicatesFilter extends UserFilter<string> {
    type = 'duplicates'
    label = 'duplicates.multi.combo.title'
    comboModel = [ 'users.duplicated', 'users.not.duplicated' ]
    order = ''
    filterProp = 'this'

    filter = (duplicates: {}[]) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            outputModel.indexOf('users.duplicated') >= 0 && duplicates.length > 0 ||
            outputModel.indexOf('users.not.duplicated') >= 0 && duplicates.length == 0
    }
}

class MailFilter extends UserFilter<string> {
    type = 'email'
    label = 'email'
    comboModel = ['users.with.mail', 'users.without.mail']

    filter = (mail: string) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            outputModel.indexOf('users.without.mail') >= 0 && !mail ||
            outputModel.indexOf('users.with.mail') >= 0 && !(!mail)
    }
}

class DateFilter extends UserFilter<{date: Date, comparison: string}> {
    type = 'creationDate'
    label = 'creation.date'
    comboModel = [{date: undefined, comparison:'users.before'}, {date: undefined, comparison:'users.since'}]
    display = 'comparison'
    order = '+date'
    filterProp = 'date'
    datepicker = true

    filter = (date: string) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
        outputModel.some(o => (typeof o.date === "undefined") || isNaN(o.date.valueOf()) ||
        (o.comparison === 'users.before' ? Date.parse(o.date.toISOString()) >= Date.parse(date)
        : Date.parse(o.date.toISOString()) <= Date.parse(date)))
    }
}

class AdmlFilter extends UserFilter<string> {
    type = 'functions';
    label = 'adml.multi.combo.title';
    comboModel = ['users.adml', 'users.not.adml'];
    order = '+'
    filterProp = 'this'

    filter = (functions: [string, Array<string>]) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 
            || outputModel.indexOf('users.adml') >= 0 
                && functions.findIndex((f) => f[0] == "ADMIN_LOCAL") >= 0
            || outputModel.indexOf('users.not.adml') >= 0
                && functions.findIndex((f) => f[0] == "ADMIN_LOCAL") == -1;
    }
}

@Injectable()
export class UserlistFiltersService {

    constructor(){}

    updateSubject: Subject<any> = new Subject<any>()

    private profileFilter = new ProfileFilter(this.updateSubject)
    private classesFilter = new ClassesFilter(this.updateSubject)
    private functionalGroupsFilter = new FunctionalGroupsFilter(this.updateSubject)
    private manualGroupsFilter = new ManualGroupsFilter(this.updateSubject)
    private activationFilter = new ActivationFilter(this.updateSubject)
    private functionsFilter = new FunctionsFilter(this.updateSubject)
    private sourcesFilter = new SourcesFilter(this.updateSubject)
    private duplicatesFilter = new DuplicatesFilter(this.updateSubject)
    private mailFilter = new MailFilter(this.updateSubject);
    private admlFilter = new AdmlFilter(this.updateSubject);
    private dateFilter = new DateFilter(this.updateSubject);

    filters : UserFilterList<any> = [
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
        this.dateFilter
    ]

    resetFilters(){
        for(let i = 0; i < this.filters.length; i++){
            this.filters[i].outputModel = []
        }
    }

    setProfilesComboModel(combos: string[]) {
        this.profileFilter.comboModel = combos
    }

    setClassesComboModel(combos: {id:string, name:string}[]) {
        this.classesFilter.comboModel = combos
    }

    setSourcesComboModel(combos: string[]) {
        this.sourcesFilter.comboModel = combos
    }

    setFunctionsComboModel(combos: string[]) {
        this.functionsFilter.comboModel = combos
    }

    setFunctionalGroupsComboModel(combos: string[]) {
        this.functionalGroupsFilter.comboModel = combos
    }

    setManualGroupsComboModel(combos: string[]) {
        this.manualGroupsFilter.comboModel = combos
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

    getFormattedFilters() : Object {
        let formattedFilters = {}
        for(let i = 0; i < this.filters.length; i++) {
            let filter = this.filters[i]
            formattedFilters[filter.type] = filter.filter
        }
        return formattedFilters
    }

    getFormattedOutputModels(): Object{
        let outputModels = {}
        for(let i = 0; i < this.filters.length; i++) {
            let filter = this.filters[i]
            outputModels[filter.type] = filter.outputModel;
        }
        return outputModels
    }
}