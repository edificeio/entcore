import { ng } from 'entcore'
import { archiveController } from './controllers/controller'
import { exportController } from './controllers/export/controller'
import { importController } from './controllers/import/controller'

ng.controllers.push(archiveController);
ng.controllers.push(exportController);
ng.controllers.push(importController);