import { platformBrowser }      from '@angular/platform-browser'
import { enableProdMode }       from '@angular/core'
import { AdminModuleNgFactory } from './aot/admin.module.ngfactory'

if (process.env.ENV === 'production') {
    enableProdMode()
}

platformBrowser().bootstrapModuleFactory(AdminModuleNgFactory)