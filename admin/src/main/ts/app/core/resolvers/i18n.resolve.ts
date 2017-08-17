import { Injectable } from '@angular/core'
import { Resolve } from '@angular/router'

import { BundlesService } from 'sijil'

@Injectable()
export class I18nResolve implements Resolve<void> {

    constructor(private bundles : BundlesService){}

    resolve(): Promise<void> {
        return this.bundles.loadBundle('/admin/i18n')
    }
}