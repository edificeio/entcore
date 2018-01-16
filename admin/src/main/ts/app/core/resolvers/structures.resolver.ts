import { Injectable } from '@angular/core'
import { Resolve } from '@angular/router'

import { globalStore } from '../store'

@Injectable()
export class StructuresResolver implements Resolve<void> {

    resolve(): Promise<any> {
        return globalStore.structures.sync()
    }
}