import { AppModule }              from './app/app.module'
import { enableProdMode }           from '@angular/core'
import { platformBrowserDynamic }   from '@angular/platform-browser-dynamic'

if (process.env.ENV === 'production') {
    enableProdMode()
}

const platform = platformBrowserDynamic()
platform.bootstrapModule(AppModule)