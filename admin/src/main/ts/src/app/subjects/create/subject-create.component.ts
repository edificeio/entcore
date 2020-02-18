import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {Location} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {HttpClient} from '@angular/common/http';

import {SubjectsStore} from '../subjects.store';
import {SubjectModel} from '../../core/store/models/subject.model';
import {NotifyService} from '../../core/services/notify.service';
import {SpinnerService} from 'ngx-ode-ui';


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
                private location: Location,
                private cdRef: ChangeDetectorRef) {
    }

    ngOnInit(): void {
        console.log("log ngOnInit create");
        // Watch selected structure


    }

    createNewSubject() {
        this.newSubject.structureId = this.subjectsStore.structure.id;

        this.spinner.perform('portal-content',
            this.http.post<SubjectModel>('/directory/subject', this.newSubject)
                .do(result => {
                    this.subjectsStore.structure.subjects.data.unshift(result);
                    this.routeToSubject(result);
                    this.ns.success({
                        key: 'notify.subject.create.content',
                        parameters: {subject: this.newSubject.label}
                    }, 'notify.subject.create.title');
                    this.cdRef.markForCheck();

                }).catch(err => {
                this.ns.error({
                    key: 'notify.subject.create.error.content',
                    parameters: {subject: this.newSubject.label}
                }, 'notify.subject.create.error.title', err);
                throw err;
            }).toPromise()
        )

    }

    routeToSubject(s: SubjectModel) {
        this.router.navigate([s.id, 'details'], {relativeTo: this.route.parent});
    }

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