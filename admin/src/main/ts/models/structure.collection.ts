import { StructureModel } from './structure.model'
import { Collection } from 'toolkit'
import { UserCollection } from './user.collection'

export class StructureCollection extends Collection<StructureModel>{

    constructor(){
        super({
            sync:   '/directory/structure/admin/list',
            create: '/directory/school',
            update: '/directory/structure/:id'
        }, StructureModel)
    }

    public asTree() {
        let childrenMap = new Map<string, StructureModel[]>()
        let referenceSet = new Set<string>(this.data.map(s => s.id))
        this.data.forEach(structure => {
            structure.parents && structure.parents.forEach(parent => {
                childrenMap.has(parent.id) ?
                    childrenMap.get(parent.id).push(structure) :
                    childrenMap.set(parent.id, [structure])
            })
        })
        this.data.forEach(structure => {
            if(childrenMap.has(structure.id))
                structure.children = childrenMap.get(structure.id)
        })
        let result = this.data.filter(structure => {
            return !structure.parents ||
                    structure.parents.length === 0 ||
                    structure.parents.every(p => !referenceSet.has(p.id))
        })
        return result
    }

}

export let structureCollection = new StructureCollection()