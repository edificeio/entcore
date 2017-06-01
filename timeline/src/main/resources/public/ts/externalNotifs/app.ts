import { ExternalNotifs } from '../model/externalNotifs';
import { mainController } from './controller';
import { ng, model } from 'entcore';
import { build } from '../model/externalNotifs';

ng.controllers.push(mainController);
model.build = build;