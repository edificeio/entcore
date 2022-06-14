import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Injector, OnDestroy, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { trim } from 'ngx-ode-ui';
import { Observable, Subject } from 'rxjs';
import { filter, first, mergeMap, tap } from 'rxjs/operators';
import { NotifyService } from 'src/app/core/services/notify.service';
import { ClassModel } from 'src/app/core/store/models/structure.model';
import { ClassesService } from '../classes.service';
import { GroupsStore } from '../../groups.store';

@Component({
    selector: 'ode-class-detail',
    templateUrl: './class-details.component.html',
    styleUrls: ['./class-details.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ClassDetailsComponent extends OdeComponent implements OnInit, OnDestroy {
    public confirmationDisplayed = false;
    public $confirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public $deleteButtonClicked: Subject<ClassModel> = new Subject();
    public deleteConfirmationDisplayed = false;
    public $deleteConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public $renameButtonClicked: Subject<{}> = new Subject();
    public renameLightboxDisplayed = false;
    public renameConfirmationClicked: Subject<'confirm' | 'cancel'> = new Subject<'confirm' | 'cancel'>();

    public classNewName: string;

    constructor(public groupsStore: GroupsStore,
                private notifyService: NotifyService,
                private classesService: ClassesService,
                injector: Injector) {
                super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.subscriptions.add(
            this.route.params
            .pipe(
                filter(params => params.classId),
                tap(params => {
                    this.groupsStore.class = this.groupsStore.structure.classes.find(c => c.id === params.classId);
                    this.classNewName = this.groupsStore.class.name;
                })
            )
            .subscribe(() => this.changeDetector.markForCheck())
        );

        this.subscriptions.add(this.$deleteButtonClicked
        .pipe(
            mergeMap((c: ClassModel) => this.deleteClass(c))
        )
        .subscribe());

        this.subscriptions.add(this.$renameButtonClicked
        .pipe(
            mergeMap(() => this.renameClass())
        )
        .subscribe());

        this.classNewName = this.groupsStore.class.name;
    }

    public deleteClass(c: ClassModel): Observable<void> {
        this.deleteConfirmationDisplayed = true;
        return this.$deleteConfirmationClicked.asObservable()
        .pipe(
            first(),
            tap(() => this.deleteConfirmationDisplayed = false),
            filter(choice => choice === 'confirm'),
            mergeMap(() => this.classesService.delete(this.groupsStore.structure.id, c.id)),
            tap(() => {
                this.notifyService.success({
                    key: 'class.delete.notify.success.content',
                    parameters: {className: c.name}
                }, 'class.delete.notify.success.title');
                this.router.navigate(['../..'], {relativeTo: this.route, replaceUrl: false});
                this.changeDetector.markForCheck();
            }, (error: HttpErrorResponse) => {
                this.notifyService.error({
                    key: 'class.delete.notify.error.content',
                    parameters: {className: c.name}
                }, 'class.delete.notify.error.title');
            }),
            tap( async () => {
                // sync ProfileGroups
                await this.groupsStore.structure.syncGroups(true);
            })
        );
    }

    public renameClass(): Observable<void> {
        this.renameLightboxDisplayed = true;
        return this.renameConfirmationClicked.asObservable()
        .pipe(
            first(),
            tap(() => this.renameLightboxDisplayed = false),
            filter(choice => choice === 'confirm'),
            mergeMap(() => this.classesService.update(this.groupsStore.class.id, {name: this.classNewName})),
            tap(() => {
                this.notifyService.success('class.rename.notify.success.content'
                    , 'class.rename.notify.success.title');
            }, (error: HttpErrorResponse) => {
                this.notifyService.error('class.rename.notify.error.content'
                    , 'class.rename.notify.error.title');
            }),
            tap( async () => {
                // sync ProfileGroups
                await this.groupsStore.structure.syncGroups(true);
            })
        );
    }

    public onNameBlur(name: string): void {
        this.classNewName = trim(name);
    }
}
