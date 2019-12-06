import { Component, EventEmitter, Injector, Output } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { SelectOption } from 'ngx-ode-ui';
import { Profile } from '../../../shared/services-types';
@Component({
    selector: 'ode-connector-export',
    templateUrl: './connector-export.component.html',
    styles: [`
        .connector-export {
            padding: 0 15px 15px 15px;
        }
    `, `
        .connector-export-profile {
            display: flex;
            flex-wrap: wrap;
        }
    `, `
        .connector-export-profile__button {
            margin: 5px 5px;
        }
    `]
})
export class ConnectorExportComponent extends OdeComponent {
    @Output()
    submit: EventEmitter<{ exportFormat: ExportFormat, profile: string }> = new EventEmitter<{ exportFormat: ExportFormat, profile: string }>();

    format: ExportFormat;
    formats: ExportFormat[] = [
        {
            value: '',
            label: 'services.connector.export.type.default',
            format: 'csv',
            profiles: ['Teacher', 'Student', 'Relative', 'Guest', 'Personnel']
        },
        {
            value: 'Esidoc',
            label: 'services.connector.export.type.esidoc',
            profiles: ['Teacher', 'Student', 'Personnel'],
            format: 'xml'
        },
        {
            value: 'Cerise-teacher',
            label: 'services.connector.export.type.cerise.teacher',
            format: 'csv',
            profiles: ['Teacher']
        },
        {
            value: 'Cerise-student',
            label: 'services.connector.export.type.cerise.student',
            format: 'csv',
            profiles: ['Student']
        },
        {
            value: 'Sacoche',
            label: 'services.connector.export.type.sacoche',
            format: 'csv',
            profiles: ['Teacher', 'Student', 'Relative', 'Guest', 'Personnel']
        },
        {
            value: 'Gepi',
            label: 'services.connector.export.type.gepi',
            filename: 'ENT-Identifiants.csv',
            format: 'csv',
            profiles: ['Teacher', 'Student', 'Relative', 'Guest', 'Personnel']
        },
        {
            value: 'ProEPS-student',
            label: 'services.connector.export.type.proeps.student',
            format: 'csv',
            profiles: ['Student']
        },
        {
            value: 'ProEPS-relative',
            label: 'services.connector.export.type.proeps.relative',
            format: 'csv',
            profiles: ['Relative']
        },
        {
            value: 'Transition',
            label: 'services.connector.export.type.transition',
            format: 'csv',
            profiles: ['Teacher', 'Personnel', 'Relative', 'Student', 'Guest'],
        }
    ];

    formatOptions: SelectOption<ExportFormat>[] = this.formats.map(f => ({value: f, label: f.label}));

    constructor(injector: Injector) {
        super(injector);
        this.format = this.formats[0];
    }

    public isAllProfiles(profiles: Profile[]): boolean {
        return profiles.length === 5;
    }
}

export interface ExportFormat {
    value: string;
    label: string;
    format: string;
    profiles: Profile[];
    filename?: string;
}
