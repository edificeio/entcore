import { Component, Output, EventEmitter } from "@angular/core";
import { Profile } from "../../../shared/services-types";

@Component({
    selector: 'connector-export',
    template: `
        <div class="connector-export">
            <h4>
                <s5l>services.connector.export.choose.type</s5l>
            </h4>
            <select [(ngModel)]="format" name="format">
                <option *ngFor="let format of formats" [ngValue]="format">
                    <s5l>{{ format.label }}</s5l>
                </option>
            </select>

            <h4>
                <s5l>services.connector.export.choose.profile</s5l>
            </h4>
            
            <div class="connector-export-profile">
                <button *ngFor="let profile of format.profiles"
                        class="connector-export-profile__button"
                        (click)="submit.emit({exportFormat: format, profile: profile})">
                    <s5l>services.connector.export.profile.export</s5l>
                    <s5l>{{ profile }}</s5l>
                    <i class="fa fa-upload is-size-5"></i>
                </button>
                <button class="connector-export-profile__button"
                        *ngIf="isAllProfiles(format.profiles)"
                        (click)="submit.emit({exportFormat: format, profile: 'all'})">
                    <s5l>services.connector.export.profile.all</s5l>
                    <i class="fa fa-upload is-size-5"></i>
                </button>
            </div>
        </div>
    `,
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
export class ConnectorExportComponent {
    @Output()
    submit: EventEmitter<{exportFormat: ExportFormat, profile: string}> = new EventEmitter<{exportFormat: ExportFormat, profile: string}>();
    
    format: ExportFormat;
    formats: ExportFormat[];

    constructor() {
        this.formats = [
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
                profiles: ['Teacher','Personnel','Relative','Student','Guest'],
            }
        ];

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
