import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';
import {routing} from '../core/services/routing.service';
import {SpinnerService} from 'ngx-ode-ui';
import {CommunicationRulesService} from '../communication/communication-rules.service';
import {SubjectsStore} from "./subjects.store";
import {StructureModel} from '../core/store/models/structure.model';
import {SubjectModel} from '../core/store/models/subject.model';

@Component({
    selector: 'subjects-root',
    templateUrl: './subjects.component.html',
    providers: [CommunicationRulesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SubjectsComponent implements OnInit, OnDestroy {

    private error: Error;
    private structureSubscriber: Subscription;
    type: string;
    subjectInputFilter: string;
    selectedSubject: SubjectModel;

    constructor(
        private route: ActivatedRoute,
        public router: Router,
        public subjectsStore: SubjectsStore,
        private spinner: SpinnerService) {
    }

    ngOnInit(): void {
        console.log("log ngOnInit subjects component");
        // Watch selected structure
        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if (data['structure']) {
                this.subjectsStore.structure = data['structure'];
            }
        });

    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe()
    }

    onError(error: Error) {
        console.error(error);
        this.error = error;
    }


    isSelected = (subject: SubjectModel) => {
        return this.selectedSubject && subject && this.selectedSubject.id === subject.id;
    };


    filterByInput = (subject: SubjectModel) => {
        if (!this.subjectInputFilter) return true;
        return subject.label.toLowerCase()
            .indexOf(this.subjectInputFilter.toLowerCase()) >= 0;
    };

    closeCompanion() {
        this.router.navigate(['../subjects'], {relativeTo: this.route}).then(() => {
            this.subjectsStore.subject = null;
        });
    }

    openUserDetail(subject) {
        this.subjectsStore.subject = subject;
        this.spinner.perform('portal-content', this.router.navigate([subject.id, 'details'], {relativeTo: this.route}));
    }
}