import { platformBrowser }      from '@angular/platform-browser'
import { enableProdMode }       from '@angular/core'
import { AppModuleNgFactory }   from './app.module.ngfactory'

if (process.env.ENV === 'production') {
    enableProdMode()
}

platformBrowser().bootstrapModuleFactory(AppModuleNgFactory)