import { Mix, Model } from "entcore-toolkit";
import { BundlesService } from "ngx-ode-sijil";
import { RoleModel } from "./role.model";

export class WidgetModel extends Model<WidgetModel> {
    id: string;
    name: string;
    displayName: string;
    application: {
        address: string, 
        name: string,
        id: string, 
        strongLink: boolean
    };
    i18n: string;
    js: string;
    locked: boolean;
    path: string;

    roles: Array<RoleModel>;
    levelsOfEducation: Array<number>;

    constructor() {
        super({});
    }

    syncRoles = (structureId: string, widgetId: string): Promise<void> => {
        return this.http.get(`/appregistry/widget/${widgetId}?structureId=${structureId}`)
            .then(res => {
                this.roles = new Array(Mix.castAs(RoleModel, res.data));
            });
    }
}