import {Component, Input, OnInit} from '@angular/core';

import {AbstractSection} from '../abstract.section';
import { UserModel } from 'src/app/core/store/models/user.model';
import { StructureModel } from 'src/app/core/store/models/structure.model';

// aafFunctions exemple: [[1051,ENS,ENSEIGNEMENT,L0201,LETTRES CLASSIQUES]]
@Component({
    selector: 'user-aaf-functions-section',
    templateUrl: './user-aaf-functions-section.component.html'
})
export class UserAafFunctionsComponent extends AbstractSection implements OnInit {
    @Input() user: UserModel;
    @Input() structure: StructureModel;

    sectionTitle = '';

    ngOnInit() {
        this.initSectionTitle();
    }

    onUserChange() {
        this.initSectionTitle();
    }

    private initSectionTitle(): void {
        if (this.user.type == 'Teacher') {
            this.sectionTitle = 'users.details.section.aaffunctions.teacher';
        } else if (this.user.type == 'Personnel') {
            this.sectionTitle = 'users.details.section.aaffunctions.personnel';
        }
    }
}
