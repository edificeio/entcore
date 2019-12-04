import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges, OnInit} from '@angular/core';

import {AbstractSection} from '../abstract.section';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { SpinnerService } from 'ngx-ode-ui';
import { NotifyService } from 'src/app/core/services/notify.service';
import { globalStore } from 'src/app/core/store/global.store';

@Component({
    selector: 'ode-user-structures-section',
    templateUrl: './user-structures-section.component.html',
    inputs: ['user', 'structure'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserStructuresSectionComponent extends AbstractSection implements OnInit, OnChanges {
    lightboxStructures: StructureModel[];

    showStructuresLightbox = false;

    private _inputFilter = '';
    set inputFilter(filter: string) {
        this._inputFilter = filter;
    }
    get inputFilter() {
        return this._inputFilter;
    }

    constructor(public spinner: SpinnerService,
                private ns: NotifyService,
                protected cdRef: ChangeDetectorRef) {
        super();
    }

    ngOnInit() {
        this.updateLightboxStructures();
    }

    ngOnChanges() {
        this.updateLightboxStructures();
    }

    private updateLightboxStructures() {
        this.lightboxStructures = globalStore.structures.data.filter(
            s => !this.user.structures.find(us => us.id == s.id)
        );
    }

    protected onUserChange() {}

    disableStructure = (s) => {
        return this.spinner.isLoading(s.id);
    }

    filterByInput = (s: {id: string, name: string}) => {
        if (!this.inputFilter) { return true; }
        return `${s.name}`.toLowerCase().indexOf(this.inputFilter.toLowerCase()) >= 0;
    }

    addStructure = (structure) => {
        this.spinner.perform('portal-content', this.user.addStructure(structure.id)
            .then(() => {
                this.ns.success(
                    {
                        key: 'notify.user.add.structure.content',
                        parameters: {
                            structure:  structure.name
                        }
                    }, 'notify.user.add.structure.title');

                this.updateLightboxStructures();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.add.structure.error.content',
                        parameters: {
                            structure:  structure.name
                        }
                    }, 'notify.user.add.structure.error.title', err);
            })
        );
    }

    removeStructure = (structure) => {
        this.spinner.perform('portal-content', this.user.removeStructure(structure.id)
            .then(() => {
                this.ns.success(
                    {
                        key: 'notify.user.remove.structure.content',
                        parameters: {
                            structure:  structure.name
                        }
                    }, 'notify.user.remove.structure.title');

                this.updateLightboxStructures();
                this.cdRef.markForCheck();
            })
            .catch(err => {
                this.ns.error(
                    {
                        key: 'notify.user.remove.structure.error.content',
                        parameters: {
                            structure:  structure.name
                        }
                    }, 'notify.user.remove.structure.error.title', err);
            })
        );
    }
}
