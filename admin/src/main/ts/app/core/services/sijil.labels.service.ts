import { Injectable } from '@angular/core'
import { BundlesService } from 'sijil'

@Injectable()
export class SijilLabelsService {

    constructor(private bundles : BundlesService){}

    getLabel(label: string){
        return this.bundles.translate(label)
    }

}