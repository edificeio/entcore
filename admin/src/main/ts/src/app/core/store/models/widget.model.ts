import { RoleModel } from "./role.model";

export class WidgetModel {
    application: {
        address: string, 
        id: string, 
        name: string, 
        strongLink: boolean
    };
    i18n: string;
    id: string;
    js: string;
    locked: boolean;
    name: string;
    path: string;
    displayName: string;
    roles: RoleModel[];
    levelsOfEducation: number[];
}