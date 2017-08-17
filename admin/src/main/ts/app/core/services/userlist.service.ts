import { UserModel } from '../store'
import { Injectable, ElementRef, Renderer } from '@angular/core'
import { BundlesService } from 'sijil'

@Injectable()
export class UserListService {

    constructor(private renderer: Renderer,
        private bundlesService: BundlesService){}

    // Sorts
    sortsMap = {
        alphabetical: {
            sort: '+',
            orderedValue: 'lastName',
            staticValues: ['+firstName'],
            selected: true },
        profile: {
            sort: '+',
            orderedValue: 'type',
            selected: false }
    }
    sorts : Array<string> =  ['+lastName', '+firstName', '+type']
    changeSorts = function(target) {
        this.resetLimit()
        this.sortsMap[target].selected = true
        this.sortsMap[target].sort = this.sortsMap[target].sort === '+' ? '-' : '+'
        this.sorts = [
            this.sortsMap[target].sort + this.sortsMap[target].orderedValue,
            ...(this.sortsMap[target].staticValues || []) ]

        for(let prop in this.sortsMap) {
            if(prop !== target) {
                this.sorts = this.sorts.concat([
                    this.sortsMap[prop].sort + this.sortsMap[prop].orderedValue,
                    ...(this.sortsMap[prop].staticValues || []) ])
                this.sortsMap[prop].selected = false
            }
        }
    }

    // Filters
    private _inputFilter = ""
    set inputFilter(filter: string) {
        this._inputFilter = filter
        this.resetLimit()
    }
    get inputFilter() {
        return this._inputFilter
    }
    filterByInput = (user: UserModel) => {
        if(!this.inputFilter) return true
        return `${user.displayName}`.toLowerCase()
            .indexOf(this.inputFilter.trim().toLowerCase()) >= 0
    }

    // Limit
    DEFAULT_INCREMENT: number = 100
    limit = this.DEFAULT_INCREMENT
    resetLimit() {
        this.limit = this.DEFAULT_INCREMENT
    }
    addPage(max?: number) {
        if(max){
            this.limit = Math.min(this.limit + this.DEFAULT_INCREMENT, max)
        } else {
            this.limit = this.limit + this.DEFAULT_INCREMENT
        }
    }

    // Scroll
    private ticking = false
    listScroll = (event, list, cdRef) => {
        let divElem = event.target

        if (!this.ticking) {
            window.requestAnimationFrame(() => {
                 if ((divElem.offsetHeight + divElem.scrollTop) >= divElem.scrollHeight) {
                    this.addPage(list.length)
                    cdRef.markForCheck()
                }
                this.ticking = false;
            });
        }
        this.ticking = true;
    }
}