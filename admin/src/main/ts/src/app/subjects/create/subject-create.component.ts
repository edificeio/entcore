import {ChangeDetectionStrategy, Component} from '@angular/core';
import {Location} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {HttpClient} from '@angular/common/http';

import {SubjectsStore} from '../subjects.store';
import {GroupModel, SubjectModel} from '../../core/store/models';
import {NotifyService, SpinnerService} from '../../core/services';

import {trim} from '../../shared/utils/string';

import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/mergeMap';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/do';
import 'rxjs/add/operator/toPromise';

@Component({
    selector: 'subject-create',
    templateUrl: './subject-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubjectCreate {

    newSubject: SubjectModel = new SubjectModel();
    subjectInputFilter: string;
    selectedSubject: SubjectModel;

    constructor(private http: HttpClient,
                public subjectsStore: SubjectsStore,
                private ns: NotifyService,
                private spinner: SpinnerService,
                private router: Router,
                private route: ActivatedRoute,
                private location: Location) {
    }

    ngOnInit(): void {
        console.log("log ngOnInit create");
        // Watch selected structure


    }

    createNewSubject() {
        this.newSubject.structureId = this.subjectsStore.structure.id;

    }


    isSelected = (subject: SubjectModel) => {
        return this.selectedSubject && subject && this.selectedSubject.id === subject.id;
    };

    filterByInput = (subject: SubjectModel) => {
        if (!this.subjectInputFilter) return true;
        return subject.label.toLowerCase()
            .indexOf(this.subjectInputFilter.toLowerCase()) >= 0;
    };

    cancel() {
        this.location.back();
    }

    onSubjectNameBlur(label: string): void {
    }

    closePanel() {
        console.log("close panel");
    };

    showCompanion(): boolean {
        return true;
    }
}