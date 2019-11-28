import {DebugElement} from '@angular/core';
import { InternalCommunicationRule, GroupModel } from 'src/app/core/store/models/group.model';

export function getText(el: DebugElement): string {
    return el.nativeElement.textContent.trim();
}

export function clickOn(el: DebugElement): void {
    return el.triggerEventHandler('click', null);
}
export function generateGroup(name: string,
                              internalCommunicationRule: InternalCommunicationRule = 'BOTH',
                              type: string = null, subType: string = null,
                              classes: { id: string, name: string }[] = null,
                              structures: { id: string, name: string }[] = [{id: 'structureId', name: 'structureName'}],
                              filter: string = null): GroupModel {
    return {name, id: name, internalCommunicationRule, type, subType, classes, structures, filter} as GroupModel;
}

export function generateMockGroupModel(id: string, type: string): GroupModel {
    const groupModel: GroupModel = {id, type} as GroupModel;
    groupModel.users = [];
    return groupModel;
}
