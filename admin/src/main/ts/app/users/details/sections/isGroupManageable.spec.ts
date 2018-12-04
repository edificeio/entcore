import { GroupModel, StructureModel } from '../../../core/store/models';
import { GlobalStore } from '../../../core/store';
import { isGroupManageable } from './isGroupManageable';

describe('isGroupManageable', () => {
    let globalStore: GlobalStore;

    beforeEach(() => {
        globalStore = new GlobalStore();
    });

    it(`should return true when the manual group 'group1' is in the globalStore#structure1`, () => {
        const structure1 = generateStructure('structure1', ['group1', 'group2', 'group3']);
        globalStore.structures.data.push(structure1);
        const myGroup = generateGroup('group1');
        expect(isGroupManageable(myGroup, globalStore)).toBe(true);
    });

    it(`should return false when the manual group 'group1' is not in the globalStore structures`, () => {
        const structure1 = generateStructure('structure1', ['group2', 'group3']);
        globalStore.structures.data.push(structure1);
        const myGroup = generateGroup('group1');
        expect(isGroupManageable(myGroup, globalStore)).toBe(false);
    });

    it(`should return true when the manual group 'group1' is in the globalStore#structure2`, () => {
        const structure1 = generateStructure('structure1', ['group2', 'group3']);
        const structure2 = generateStructure('structure1', ['group1']);
        globalStore.structures.data.push(structure1, structure2);
        const myGroup = generateGroup('group1');
        expect(isGroupManageable(myGroup, globalStore)).toBe(true);
    });
});

function generateStructure(id: string, groupsId: string[]): StructureModel {
    const generatedStructure = new StructureModel();
    generatedStructure.id = id;
    generatedStructure.name = id;
    const groupModels = groupsId.map(id => generateGroup(id));
    generatedStructure.groups.data.push(...groupModels);
    return generatedStructure;
}

function generateGroup(id: string): GroupModel {
    const generatedGroup = new GroupModel();
    generatedGroup.id = id;
    generatedGroup.name = id;
    return generatedGroup;
}
