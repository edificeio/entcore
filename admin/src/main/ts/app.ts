import { AdminModule } from './admin.module'
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic'

const platform = platformBrowserDynamic()
platform.bootstrapModule(AdminModule)