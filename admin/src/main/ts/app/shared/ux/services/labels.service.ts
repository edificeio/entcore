import { Injectable } from '@angular/core'

@Injectable()
export class LabelsService {

    private labels = {
        "select.all": "Select all",
        "deselect.all": "Deselect all",
        "search": "Search"
    }

    getLabel(label: string){
        return this.labels[label] || label
    }

    mixin(labels: Object){
        for(let property in labels){
            this.labels[property] = labels[property]
        }
    }

    static withLabels(labels: Object){
        let newService = new LabelsService()
        for(let prop in labels){
            newService.labels[prop] = labels[prop]
        }
        return newService
    }
}