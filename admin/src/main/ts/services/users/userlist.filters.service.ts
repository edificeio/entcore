import { Injectable } from '@angular/core'
import { Subject } from 'rxjs'

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

    filterProp?: boolean | string
    display?: string
    order?: string
}
export type UserFilterList<T> = UserFilter<T>[]

class ProfileFilter extends UserFilter<string> {
    type = 'type'
    label = 'profiles.multi.combo.title'
    comboModel = [ 'Student', 'Teacher', 'Relative', 'Personnel', 'Guest' ]

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

@Injectable()
export class UserlistFiltersService {

    constructor(){}

    updateSubject: Subject<any> = new Subject<any>()

    private profileFilter       =  new ProfileFilter(this.updateSubject)
    private activationFilter    =  new ActivationFilter(this.updateSubject)
    private classesFilter       =  new ClassesFilter(this.updateSubject)

    filters : UserFilterList<any> = [
        this.profileFilter,
        this.activationFilter,
        this.classesFilter
    ]

    resetFilters(){
        for(let i = 0; i < this.filters.length; i++){
            this.filters[i].outputModel = []
        }
    }

    setClasses(classes: {id:string, name:string}[]) {
        this.classesFilter.comboModel = classes
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