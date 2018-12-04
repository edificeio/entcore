import { GroupModel } from '../../../core/store/models';
import { GlobalStore } from '../../../core/store';

export function isGroupManageable(group: {id?: string}, store: GlobalStore): boolean {
    return !!store.structures.data
        .find(structure => !!structure.groups.data.find(g => g.id === group.id));
}
