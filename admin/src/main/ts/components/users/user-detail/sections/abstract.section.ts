import { Input, ViewChild, ChangeDetectorRef } from '@angular/core'
import { UserModel } from '../../../../models'
import { UserDetailsModel, StructureModel, structureCollection } from '../../../../models'
import { LoadingService } from '../../../../services'

export abstract class AbstractSection {

    constructor(protected ls: LoadingService,
        protected cdRef: ChangeDetectorRef){}

    get user(){ return this._user }
    set user(u: UserModel){
        this.onUserChange()
        this._user = u
        this.details = u.userDetails
    }
    protected _user : UserModel
    protected details: UserDetailsModel
    structure: StructureModel

    protected now : string = `${new Date().getFullYear()}-${new Date().getMonth() + 1}-${new Date().getDate()}`
    protected emailPattern = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/

    protected getStructure(id: string) {
        return structureCollection.data.find(s => s.id === id)
    }

    protected wrap = (func, label, delay = 0, ...args) => {
        return this.ls.wrap(func, label, {delay: delay, cdRef: this.cdRef, binding: this.details}, ...args)
    }

    protected abstract onUserChange()

}