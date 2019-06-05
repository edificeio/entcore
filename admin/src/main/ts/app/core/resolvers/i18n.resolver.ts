import { Injectable } from '@angular/core'
import { Resolve } from '@angular/router'

import { BundlesService } from 'sijil'

@Injectable()
export class I18nResolver implements Resolve<void> {

    constructor(private bundles: BundlesService) {
    }

    resolve(): Promise<void> {
        return this.bundles.loadBundles([
            {where: '/admin/i18n', lang: null},
            {where: '/i18n', lang: null}])
            .then(() => void 0)
    }
}