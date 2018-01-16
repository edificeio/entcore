import * as timelineControllers from './controller';
import { ng, model } from 'entcore';
import { Timeline, build } from '../model/timeline';

for(let controller in timelineControllers){
    ng.controllers.push(timelineControllers[controller]);
}
model.build = build;