import { Collection, Mix } from "entcore-toolkit";
import { WidgetModel } from "../models/widget.model";

interface WidgetApiResponse {
    data: {
        widgets: Array<WidgetModel>
    };
}

export class WidgetCollection extends Collection<WidgetModel> {

    constructor() {
        super({});
    }

    syncWidgets = async () => {
        return this.http.get(`/appregistry/widgets`).
            then((res: WidgetApiResponse) => {
                if (res && res.data && Array.isArray(res.data.widgets)) {
                    res.data.widgets.forEach(w => w.displayName = w.name);
                    this.data = Mix.castArrayAs(WidgetModel, res.data.widgets);
                    console.log(this.data);
                } else {
                    this.data = [];
                }
            });
    }
}