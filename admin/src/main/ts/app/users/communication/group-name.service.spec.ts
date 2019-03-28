import { TestBed } from '@angular/core/testing';
import { generateGroup } from './communication-test-utils';
import { GroupNameService } from './group-name.service';
import { BundlesService, SijilModule } from 'sijil';
import 'rxjs/add/operator/skip';

describe('GroupNameService', () => {
    let groupNameService: GroupNameService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [GroupNameService],
            imports: [SijilModule.forRoot()]
        });
        groupNameService = TestBed.get(GroupNameService);
        const bundlesService = TestBed.get(BundlesService);
        bundlesService.addToBundle({
            "group.card.structure.Personnel": "Personnels de {{name}}",
            "group.card.structure.Relative": "Parents de {{name}}",
            "group.card.structure.Student": "Élèves de {{name}}",
            "group.card.structure.Teacher": "Enseignants de {{name}}",
            "group.card.structure.Guest": "Invités de {{name}}",
            "group.card.class.Personnel": "Personnels de la classe {{name}}",
            "group.card.class.Relative": "Parents de la classe {{name}}",
            "group.card.class.Student": "Élèves de la classe {{name}}",
            "group.card.class.Teacher": "Enseignants de la classe {{name}}",
            "group.card.class.Guest": "Invités de la classe {{name}}"
        });
    });

    describe('getGroupName', () => {
        it(`should return 'test' if the group is a manual group named 'test'`, () => {
            expect(groupNameService.getGroupName(generateGroup('test', 'BOTH', 'ManualGroup'))).toBe('test');
        });

        it('should return a nice label for a ProfileGroup of a class (6emeA)', () => {
            expect(groupNameService.getGroupName(generateGroup('test', 'BOTH',
                'ProfileGroup', null,
                [{
                    id: '6A',
                    name: '6emeA'
                }], null, 'Student'))).toBe('Élèves de la classe 6emeA');
        });

        it('should return a nice label for a ProfileGroup of a structure (Emile Zola)', () => {
            expect(groupNameService.getGroupName(generateGroup('test', 'BOTH',
                'ProfileGroup', 'StructureGroup',
                null, [{
                    id: 'emilezola',
                    name: 'Emile Zola'
                }], 'Student'))).toBe('Élèves de Emile Zola');
        });
    });
});
