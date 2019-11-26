import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core'

import { AbstractSection } from '../abstract.section'
import { NotifyService, SpinnerService } from '../../../../core/services'
import { OnChanges, OnInit } from '@angular/core/src/metadata/lifecycle_hooks';
import { Classe } from '../../../../core/store/models';

@Component({
    selector: 'user-classes-section',
    template: `
        <panel-section section-title="users.details.section.classes" [folded]="true">
            <button (click)="showClassesLightbox = true">
                <s5l>add.class</s5l>
                <i class="fa fa-plus-circle"></i>
            </button>
            <lightbox class="inner-list" [show]="showClassesLightbox"
                      (onClose)="showClassesLightbox = false">
                <div class="padded">
                    <h3>
                        <s5l>add.class</s5l>
                    </h3>
                    <list class="inner-list"
                          [model]="lightboxClasses"
                          [inputFilter]="filterByInput"
                          searchPlaceholder="search.class"
                          sort="name"
                          (inputChange)="inputFilter = $event"
                          [isDisabled]="disableClass"
                          (onSelect)="addClass($event)">
                        <ng-template let-item>
                            <span class="display-name">
                                {{ item?.name }}
                            </span>
                        </ng-template>
                    </list>
                </div>
            </lightbox>

            <ul class="actions-list">
                <li *ngFor="let classe of filteredClasses">
                    <span>{{ classe.name }}</span>
                    <i class="fa fa-times action"
                       (click)="removeClass(classe)"
                       [title]="'delete.this.class' | translate"
                       [ngClass]="{ disabled: spinner.isLoading('portal-content')}">
                    </i>
                    <span class="headteacher-buttons"
                          *ngIf="!details.isNotTeacherOrHeadTeacher(this.structure.externalId, classe)">
                        <button class="noflex"
                                *ngIf="!details.isHeadTeacherManual(this.structure.externalId, classe)"
                                (click)="addHeadTeacherManual(this.structure.id, this.structure.externalId, this.structure.name,
                                 classe)">
                            <s5l>headTeacherManual.add</s5l>
                        </button>
                        <button *ngIf="details.isHeadTeacherManual(this.structure.externalId, classe)"
                                (click)="updateHeadTeacherManual(this.structure.id, this.structure.externalId, classe)">
                            <s5l>headTeacherManual.remove</s5l>
                        </button>
                    </span>
                    <span class="headteacher-buttons"
                          *ngIf="details.isTeacherAndHeadTeacherFromAAF(this.structure.externalId, classe)"
                          [tooltip]="'headTeacher.aaf.detail' | translate">
                         <button disabled>
                            <s5l>headTeacherManual.remove</s5l>
                        </button>
                    </span>
                </li>
            </ul>
        </panel-section>
    `,
    inputs: ['user', 'structure'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserClassesSection extends AbstractSection implements OnInit, OnChanges {
    public lightboxClasses: { id: string, name: string }[] = [];
    public showClassesLightbox = false;
    public inputFilter = "";
    public filteredClasses: Classe[] = [];

    constructor(
        public spinner: SpinnerService,
        private ns: NotifyService,
        private cdRef: ChangeDetectorRef) {
        super();
    }

    ngOnInit() {
        this.updateLightboxClasses();
        this.filterManageableGroups();
    }

    ngOnChanges() {
        this.updateLightboxClasses();
        this.filterManageableGroups();
    }

    private filterManageableGroups() {
        this.filteredClasses = !this.user ? [] :
            !this.user.classes ? [] :
                this.user.classes
                    .filter(group => !!this.structure.classes
                        .find(manageableClasse => manageableClasse.id === group.id));
    }

    private updateLightboxClasses() {
        this.lightboxClasses = this.structure.classes.filter(
            c => this.user.classes && !this.user.classes.find(uc => uc.id == c.id)
        );
    }

    filterByInput = (classe: { id: string, name: string }): boolean => {
        if (!this.inputFilter) {
            return true;
        }
        return `${classe.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    };

    filterClasses = (classe: { id: string, name: string }): boolean => {
        return !this.user.classes.find(userClasse => classe.id === userClasse.id);
    };


    /**
     * Ajout du droit de professeur principal
     */
    addHeadTeacherManual(structureId: string, externalId: string, structureName :string, classe: any) {
        this.spinner.perform('portal-content', this.details.addHeadTeacherManual(structureId, externalId, structureName, classe))
            .then(() => {
                this.ns.success({
                    key: 'notify.user.add.head.teacher.content',
                    parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                }, 'notify.user.add.head.teacher.title');
                this.cdRef.markForCheck();
            }).catch(err => {
            this.ns.error({
                key: 'notify.user.add.head.teacher.error.content',
                parameters: {user: this.user.firstName + ' ' + this.user.lastName}
            }, 'notify.user.add.head.teacher.error.title', err)
        })
    }

    /**
     * Suppression du droit de professeur principal
     */
    updateHeadTeacherManual(structureId: string, externalId: string, classe: any) {
        this.spinner.perform('portal-content', this.details.updateHeadTeacherManual(structureId, externalId, classe))
            .then(() => {
                this.ns.success({
                    key: 'notify.user.remove.head.teacher.content',
                    parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                }, 'notify.user.remove.head.teacher.title');
                this.cdRef.markForCheck();
            }).catch(err => {
            this.ns.error({
                key: 'notify.user.remove.head.teacher.error.content',
                parameters: {user: this.user.firstName + ' ' + this.user.lastName}
            }, 'notify.user.remove.head.teacher.error.title', err)
        })
    }

    disableClass = (classe) => {
        return this.spinner.isLoading(classe.id)
    };

    addClass = (event) => {
        this.spinner.perform('portal-content', this.user.addClass(event))
            .then(() => {
                this.ns.success(
                    {
                        key: 'notify.user.add.class.content',
                        parameters: {
                            classe: event.name
                        }
                    }, 'notify.user.add.class.title');

                this.updateLightboxClasses();
                this.filterManageableGroups();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.add.class.error.content',
                        parameters: {
                            classe: event.name
                        }
                    }, 'notify.user.add.class.error.title', err);
            });
    };

    removeClass = (classe) => {
        this.spinner.perform('portal-content', this.user.removeClass(classe.id, classe.externalId))
            .then(() => {
                this.ns.success(
                    {
                        key: 'notify.user.remove.class.content',
                        parameters: {
                            classe: classe.name
                        }
                    }, 'notify.user.remove.class.title');

                this.updateLightboxClasses();
                this.filterManageableGroups();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.remove.class.error.content',
                        parameters: {
                            classe: classe.name
                        }
                    }, 'notify.user.remove.class.error.title', err);
            });
    };

    protected onUserChange() {
    }
}
