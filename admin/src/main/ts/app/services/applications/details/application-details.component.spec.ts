import { RoleModel } from '../../../core/store/models';
import { filterRolesByDistributions } from './application-details.component';

describe('filterRolesByDistributions', () => {
    let roles: RoleModel[];

    beforeEach(() => {
        roles = [
            generateRoleForDistributions('role1', ['distribution1']),
            generateRoleForDistributions('role2', ['distribution1', 'distribution2']),
            generateRoleForDistributions('role3', ['distribution2']),
            generateRoleForDistributions('role4', [])
        ];
    });

    it('should keep all roles compatible with distributions [distribution1]', () => {
        expect(
            filterRolesByDistributions(roles, ['distribution1'])
                .map(role => role.name)
        ).toEqual(['role1', 'role2', 'role4']);
    });

    it('should keep all roles compatible with distributions [distribution1, distribution2]', () => {
        expect(
            filterRolesByDistributions(roles, ['distribution1', 'distribution2'])
                .map(role => role.name)
        ).toEqual(['role1', 'role2', 'role3', 'role4']);
    });

    it('should keep all roles compatible with no distribution', () => {
        expect(
            filterRolesByDistributions(roles, [])
                .map(role => role.name)
        ).toEqual(['role4']);
    });
});

function generateRoleForDistributions(name: string, distributions: string[]): RoleModel {
    return {name, distributions} as RoleModel;
}
