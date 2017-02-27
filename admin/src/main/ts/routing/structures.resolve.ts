import { Injectable } from '@angular/core'
import { Resolve } from '@angular/router'

import { structureCollection } from '../store'

@Injectable()
export class StructuresResolve implements Resolve<void> {

    constructor(){}

    resolve(): Promise<any> {
        return structureCollection.sync()
    }
}