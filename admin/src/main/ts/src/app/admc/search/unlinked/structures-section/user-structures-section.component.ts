import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit} from '@angular/core';

import { StructureModel } from 'src/app/core/store/models/structure.model';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { globalStore } from 'src/app/core/store/global.store';
import { UnlinkedUserDetails, UnlinkedUserService } from '../unlinked.service';

@Component({
    selector: 'ode-unlinked-user-structures-section',
    templateUrl: './user-structures-section.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UnlinkedUserStructuresSectionComponent implements OnInit, OnChanges {
    @Input() protected user: UnlinkedUserDetails;

    lightboxStructures: StructureModel[];

    showStructuresLightbox = false;

    // Unlinked users have no structures by definition.
    public selectedStructures: StructureModel[] = [];

    private _inputFilter = '';
    set inputFilter(filter: string) {
        this._inputFilter = filter;
    }
    get inputFilter() {
        return this._inputFilter;
    }

    constructor(public spinner: SpinnerService,
                private ns: NotifyService,
                private svc:UnlinkedUserService,
                protected cdRef: ChangeDetectorRef) {
        //super();
    }

    ngOnInit() {
        this.updateLightboxStructures();
    }

    ngOnChanges() {
        this.updateLightboxStructures();
    }

    private updateLightboxStructures() {
        this.lightboxStructures = globalStore.structures.data.filter(
            s => !this.selectedStructures.find(us => us.id == s.id)
        );
    }

    disableStructure = (s) => {
        return this.spinner.isLoading(s.id);
    }

    filterByInput = (s: {id: string, name: string}) => {
        if (!this.inputFilter) { return true; }
        return `${s.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    }

    addStructure = (structure) => {
        this.spinner.perform('portal-content', this.svc.addStructure(this.user, structure.id)
            .then(() => {
                this.selectedStructures.push( structure );
                this.ns.success({
                    key: 'notify.user.add.structure.content',
                    parameters: {structure:  structure.name}
                }, 'notify.user.add.structure.title');

                this.updateLightboxStructures();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error({
                    key: 'notify.user.add.structure.error.content',
                    parameters: {structure:  structure.name}
                }, 'notify.user.add.structure.error.title', err);
            })
        );
    }

    removeStructure = (structure) => {
        this.spinner.perform('portal-content', this.svc.removeStructure(this.user, structure.id)
            .then(() => {
                const idx = this.selectedStructures.indexOf( structure );
                if( idx >= 0 ) {
                    this.selectedStructures.splice( idx, 1 );
                }
                this.ns.success({
                    key: 'notify.user.remove.structure.content',
                    parameters: {structure:  structure.name}
                }, 'notify.user.remove.structure.title');
                this.updateLightboxStructures();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error({
                    key: 'notify.user.remove.structure.error.content',
                    parameters: {structure:  structure.name}
                }, 'notify.user.remove.structure.error.title', err);
            })
        );
    }

    /**
     * Ajout du droit de direction
     */
    addDirectionManual(structureId: string, externalId: string) {
/* TODO ?
        this.spinner.perform('portal-content', this.details.addDirectionManual(structureId, externalId))
            .then(() => {
                this.ns.success({
                    key: 'notify.user.add.direction.content',
                    parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                }, 'notify.user.add.direction.title');
                this.cdRef.markForCheck();
            }).catch(err => {
            this.ns.error({
                key: 'notify.user.add.direction.error.content',
                parameters: {user: this.user.firstName + ' ' + this.user.lastName}
            }, 'notify.user.add.direction.error.title', err);
        });
*/
    }

    /**
     * Suppression du droit de direction
     */
    removeDirectionManual(structureId: string, externalId: string) {
/* TODO ?
        this.spinner.perform('portal-content', this.details.removeDirectionManual(structureId, externalId))
            .then(() => {
                this.ns.success({
                    key: 'notify.user.remove.direction.content',
                    parameters: {user: this.user.firstName + ' ' + this.user.lastName}
                }, 'notify.user.remove.direction.title');
                this.cdRef.markForCheck();
            }).catch(err => {
            this.ns.error({
                key: 'notify.user.remove.direction.error.content',
                parameters: {user: this.user.firstName + ' ' + this.user.lastName}
            }, 'notify.user.remove.direction.error.title', err);
        });
*/
    }
}
