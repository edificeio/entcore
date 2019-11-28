import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges, OnInit} from '@angular/core';

import {AbstractSection} from '../abstract.section';
import { SpinnerService } from 'src/app/core/services/spinner.service';
import { NotifyService } from 'src/app/core/services/notify.service';
import { Classe } from 'src/app/core/store/models/user.model';

@Component({
    selector: 'ode-user-classes-section',
    templateUrl: './user-classes-section.component.html',
    inputs: ['user', 'structure'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserClassesSectionComponent extends AbstractSection implements OnInit, OnChanges {
    public lightboxClasses: { id: string, name: string }[] = [];
    public showClassesLightbox = false;
    public inputFilter = '';
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
            c => this.user.classes && !this.user.classes.find(uc => uc.id === c.id)
        );
    }

    filterByInput = (classe: { id: string, name: string }): boolean => {
        if (!this.inputFilter) {
            return true;
        }
        return `${classe.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    }

    filterClasses = (classe: { id: string, name: string }): boolean => {
        return !this.user.classes.find(userClasse => classe.id === userClasse.id);
    }


    /**
     * Ajout du droit de professeur principal
     */
    addHeadTeacherManual(structureId: string, externalId: string, classe: any) {
        this.spinner.perform('portal-content', this.details.addHeadTeacherManual(structureId, externalId, classe))
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
            }, 'notify.user.add.head.teacher.error.title', err);
        });
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
            }, 'notify.user.remove.head.teacher.error.title', err);
        });
    }

    disableClass = (classe) => {
        return this.spinner.isLoading(classe.id);
    }

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
    }

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
    }

    protected onUserChange() {
    }
}
