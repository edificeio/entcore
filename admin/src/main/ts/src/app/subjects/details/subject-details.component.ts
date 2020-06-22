import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit} from "@angular/core";
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import {SubjectsStore} from "../subjects.store";
import {NotifyService} from '../../core/services/notify.service';
import {trim, SpinnerService} from 'ngx-ode-ui';
import {ActivatedRoute, Router} from "@angular/router";
import {SubjectModel} from "../../core/store/models/subject.model";
import {Observable, Subject, Subscription} from "rxjs";
import {SubjectsService} from "../subjects.service";
import "rxjs-compat/add/operator/first";
import "rxjs-compat/add/operator/filter";

@Component({
    selector: 'subject-details',
    templateUrl: './subject-details.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})

export class SubjectDetails implements OnInit, OnDestroy {

    public subject: SubjectModel;

    public deleteSubscription: Subscription;
    public deleteButtonClicked: Subject<SubjectModel> = new Subject();
    public deleteConfirmationDisplayed: boolean = false;
    public deleteConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public renameSubscription: Subscription;
    public renameButtonClicked: Subject<{}> = new Subject();
    public renameDisplayed: boolean = false;
    public renameConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();
    public subjectNewLabel: string;
    public subjectNewCode: string;

    constructor(private http: HttpClient,
                public subjectsStore: SubjectsStore,
                private notifyService: NotifyService,
                private spinner: SpinnerService,
                private subjectsService: SubjectsService,
                private router: Router,
                private activatedRoute: ActivatedRoute,
                private cdRef: ChangeDetectorRef) {
    }


    ngOnInit(): void {
        console.log("log ngOnInit details");
        this.activatedRoute.params.subscribe(params => {
            this.subjectsStore.subject = null;
            let id = params["subjectId"];
            this.subjectsStore.subject = this.subject = this.subjectsStore.structure.subjects.data.find(sub => sub.id === id);
            this.subjectNewLabel = this.subject.label;
            this.subjectNewCode = this.subject.code;
            this.cdRef.markForCheck();
        });

        this.deleteSubscription = this.deleteButtonClicked
            .mergeMap((subject: SubjectModel) => this.deleteSubject(subject))
            .subscribe();

        this.renameSubscription = this.renameButtonClicked
            .mergeMap(() => this.renameSubject())
            .subscribe();

    }

    ngOnDestroy(): void {
        this.deleteSubscription.unsubscribe();
        this.renameSubscription.unsubscribe();
    }


    public deleteSubject(subject: SubjectModel): Observable<void> {
        this.deleteConfirmationDisplayed = true;
        return this.deleteConfirmationClicked.asObservable()
            .first()
            .do(() => this.deleteConfirmationDisplayed = false)
            .filter(choice => choice === 'confirm')
            .mergeMap(() => this.subjectsService.delete(subject))
            .do(() => {
                this.notifyService.success('subject.rename.notify.success.content'
                    , 'subject.rename.notify.success.title');
                this.router.navigate(['../..'], {relativeTo: this.activatedRoute, replaceUrl: false});
                this.cdRef.markForCheck();
            }, (error: HttpErrorResponse) => {
                this.notifyService.error({
                    key: 'subject.delete.notify.error.content',
                    parameters: {subject: subject.label}
                }, 'subject.delete.notify.error.title');
            });
    }

    public renameSubject(): Observable<void> {
        this.renameDisplayed = true;
        return this.renameConfirmationClicked.asObservable()
            .first()
            .do(() => this.renameDisplayed = false)
            .filter(choice => choice === 'confirm')
            .mergeMap(() => this.subjectsService.update({
                id: this.subjectsStore.subject.id,
                label: this.subjectNewLabel,
                code: this.subjectNewCode
            }))
            .do(() => {
                this.notifyService.success('subject.rename.notify.success.content'
                    , 'subject.rename.notify.success.title');
                this.cdRef.markForCheck();
            }, (error: HttpErrorResponse) => {
                this.notifyService.error('subject.rename.notify.error.content'
                    , 'subject.rename.notify.error.title');
            });
    }

    public onBlurSubjectFields(key: string, stg: string): void {
        this[key] = trim(stg);
    }

    checkDuplicate(label : string, code : string): boolean {
        return this.checkDuplicateLabel(label) || this.checkDuplicateCode(code);
    }
    checkDuplicateLabel(label : string): boolean {
        if(label && this.subject.label.toLowerCase().trim() !== label.toLowerCase().trim())
            return this.subjectsStore.structure.subjects.data
                .find(sub => sub.label.toLowerCase().trim() === label.toLowerCase().trim()) !== undefined;
        else
            return false;
    }
    checkDuplicateCode(code : string): boolean {
        if(code && this.subject.code.toLowerCase().trim() !== code.toLowerCase().trim())
            return this.subjectsStore.structure.subjects.data
                .find(sub => sub.code.toLowerCase().trim() === code.toLowerCase().trim()) !== undefined;
        else
            return false;
    }
}