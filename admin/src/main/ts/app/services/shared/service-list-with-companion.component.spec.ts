import { ApplicationModel } from '../../core/store/models';
import { filterApplicationsByLevelsOfEducation } from './services-list-with-companion.component';

describe('filterApplicationsByLevelsOfEducation', () => {
    let apps: ApplicationModel[];

    beforeEach(() => {
        apps = [
            generateAppForLevels('app1', [1]),
            generateAppForLevels('app2', [1, 2]),
            generateAppForLevels('app3', [2]),
            generateAppForLevels('app4', [])
        ];
    });

    it('should keep all apps compatible with levels [1]', () => {
        expect(
            filterApplicationsByLevelsOfEducation(apps, [1])
                .map(app => app.name)
        ).toEqual(['app1', 'app2']);
    });

    it('should keep all apps compatible with levels [1, 2]', () => {
        expect(
            filterApplicationsByLevelsOfEducation(apps, [1, 2])
                .map(app => app.name)
        ).toEqual(['app1', 'app2', 'app3']);
    });

    it('should drop all apps when given levels is empty', () => {
        expect(
            filterApplicationsByLevelsOfEducation(apps, [])
                .map(app => app.name))
            .toEqual([]);
    });
});

function generateAppForLevels(name: string, levelsOfEducation: number[]): ApplicationModel {
    return {name, levelsOfEducation} as ApplicationModel;
}
