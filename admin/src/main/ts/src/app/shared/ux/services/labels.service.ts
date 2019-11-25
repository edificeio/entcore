import {Injectable} from '@angular/core';

@Injectable()
export class LabelsService {

    private labels = {
        'select.all': 'Select all',
        'deselect.all': 'Deselect all',
        search: 'Search'
    };

    static withLabels(labels: Object) {
        const newService = new LabelsService();
        for (const prop in labels) {
            newService.labels[prop] = labels[prop];
        }
        return newService;
    }

    getLabel(label: string) {
        return this.labels[label] || label;
    }

    mixin(labels: Object) {
        for (const property in labels) {
            this.labels[property] = labels[property];
        }
    }
}
