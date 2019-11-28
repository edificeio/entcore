import {StructureModel} from '../models/structure.model';
import {Collection} from 'entcore-toolkit';

export class StructureCollection extends Collection<StructureModel> {

    constructor() {
        super({
            sync:   '/directory/structure/admin/list',
            create: '/directory/school',
            update: '/directory/structure/:id'
        }, StructureModel);
    }

    public asTree() {
        const childrenMap = new Map<string, StructureModel[]>();
        const referenceSet = new Set<string>(this.data.map(s => s.id));
        this.data.forEach(structure => {
            structure.parents && structure.parents.forEach(parent => {
                childrenMap.has(parent.id) ?
                    childrenMap.get(parent.id).push(structure) :
                    childrenMap.set(parent.id, [structure]);
            });
        });
        this.data.forEach(structure => {
            if (childrenMap.has(structure.id)) {
                structure.children = childrenMap.get(structure.id);
            }
        });
        const result = this.data.filter(structure => {
            return !structure.parents ||
                    structure.parents.length <= 1 ||
                    structure.parents.every(p => !referenceSet.has(p.id));
        });
        return result;
    }

}
