import { historyController } from './controller';
import { ng, model } from 'entcore';
import { Timeline, build } from '../model/timeline';

ng.controllers.push(historyController);
model.build = build;
(model as any).notifications 	= { mine: true };
(model as any).notificationTypes = { mine: true };