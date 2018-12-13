import { AbstractStore } from './abstract.store';
import { StructureCollection } from './collections';
import { ValueProvider } from '@angular/core';

export class GlobalStore extends AbstractStore {
    constructor() {
        super('structures');
    }

    structures : StructureCollection = new StructureCollection();
}

export let globalStore = new GlobalStore();

export const globalStoreProvider: ValueProvider = {provide: GlobalStore, useValue: globalStore};
