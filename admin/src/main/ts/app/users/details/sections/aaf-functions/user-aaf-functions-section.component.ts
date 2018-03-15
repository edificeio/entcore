import { Component, Input, OnInit } from '@angular/core';

import { AbstractSection } from '../abstract.section'
import { UserModel, StructureModel } from '../../../../core/store';

// aafFunctions exemple: [[1051,ENS,ENSEIGNEMENT,L0201,LETTRES CLASSIQUES]]
@Component({
    selector: 'user-aaf-functions-section',
    template: `
        <panel-section 
            [section-title]="sectionTitle" 
            [folded]="true" *ngIf="!user.deleteDate">
            <ul>
                <li *ngFor="let f of user?.aafFunctions">
                    <span *ngIf="f[0] == structure.externalId">{{ f[2] }} / {{ f[4] }}</span>
                </li>
            </ul>
        </panel-section>
    `
})
export class UserAafFunctionsComponent extends AbstractSection implements OnInit {
    @Input() user: UserModel;
    @Input() structure: StructureModel;

    sectionTitle: string = '';

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