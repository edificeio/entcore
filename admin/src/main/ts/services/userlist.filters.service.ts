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

class MatieresFilter extends UserFilter<string> {
    type = 'aafFunctions'
    label = 'matieres.multi.combo.title'
    comboModel = []
    order = '+'
    filterProp = 'this'

    filter = (matieres: string[]) => {
        let outputModel = this.outputModel
        return outputModel.length === 0 ||
            matieres && matieres.length > 0 &&
            matieres.some(m => {
                return outputModel.some(o => o === m)
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

@Injectable()
export class UserlistFiltersService {

    constructor(){}

    updateSubject: Subject<any> = new Subject<any>()

    private profileFilter           = new ProfileFilter(this.updateSubject)
    private classesFilter           = new ClassesFilter(this.updateSubject)
    private functionalGroupsFilter  = new FunctionalGroupsFilter(this.updateSubject)
    private manualGroupsFilter      = new ManualGroupsFilter(this.updateSubject)
    private activationFilter        = new ActivationFilter(this.updateSubject)
    private functionsFilter         = new FunctionsFilter(this.updateSubject)
    // FIXME when user model updated
    // private matieresFilter          = new MatieresFilter(this.updateSubject)
    private sourcesFilter           = new SourcesFilter(this.updateSubject)

    filters : UserFilterList<any> = [
        this.profileFilter,
        this.classesFilter,
        this.functionalGroupsFilter,
        this.manualGroupsFilter,
        this.activationFilter,
        this.functionsFilter,
        // FIXME when user model updated
        // this.matieresFilter,
        this.sourcesFilter
    ]

    resetFilters(){
        for(let i = 0; i < this.filters.length; i++){
            this.filters[i].outputModel = []
        }
    }

    setProfiles(profiles: string[]) {
        this.profileFilter.comboModel = profiles
    }

    setClasses(classes: {id:string, name:string}[]) {
        this.classesFilter.comboModel = classes
    }

    setSources(sources: string[]) {
        this.sourcesFilter.comboModel = sources
    }

    setFunctions(functions: string[]) {
        this.functionsFilter.comboModel = functions
    }

    // FIXME when user model updated
    // setMatieres(matieres: string[]) {
    //     this.matieresFilter.comboModel = matieres
    // }

    setFunctionalGroups(fgroups: string[]) {
        this.functionalGroupsFilter.comboModel = fgroups
    }

    setManualGroups(mgroups: string[]) {
        this.manualGroupsFilter.comboModel = mgroups
    }

    getFormattedFilters() : Object {
        let formattedFilters = {}
        for(let i = 0; i < this.filters.length; i++) {
            let filter = this.filters[i]
            formattedFilters[filter.type] = filter.filter
        }
        return formattedFilters
    }
}