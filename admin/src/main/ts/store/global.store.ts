import { AbstractStore } from './abstract.store'
import { StructureCollection } from './collections'

class GlobalStore extends AbstractStore {

    constructor() { super('structures') }

    structures : StructureCollection = new StructureCollection()

}

export let globalStore = new GlobalStore()