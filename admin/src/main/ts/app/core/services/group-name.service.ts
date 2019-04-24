import { GroupModel } from '../store/models';
import { BundlesService } from 'sijil';
import { Injectable } from '@angular/core';

@Injectable()
export class GroupNameService {
    constructor(private bundlesService: BundlesService) {
    }

    getGroupName(group: GroupModel): string {
        if (group.type === 'ManualGroup') {
            return group.name;
        }

        if (group.type === 'ProfileGroup') {
            if (group.filter && group.classes && group.classes.length > 0) {
                return this.bundlesService.translate(`group.card.class.${group.filter}`, {name: group.classes[0].name});
            } else if (group.filter && group.structures && group.structures.length > 0) {
                return this.bundlesService.translate(`group.card.structure.${group.filter}`, {name: group.structures[0].name});
            }
        }

        // Defaulting to the console v1 behaviour
        const indexOfSeparation = group.name.lastIndexOf('-');
        if (indexOfSeparation < 0) {
            return group.name;
        }
        return `${this.bundlesService.translate(group.name.slice(0, indexOfSeparation))}-${this.bundlesService.translate(group.name.slice(indexOfSeparation + 1))}`;
    }
}
