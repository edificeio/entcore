import { Collection } from "entcore-toolkit";
import { WidgetModel } from "../models/widget.model";

export class WidgetCollection extends Collection<WidgetModel> {
    public structureId: string;

    constructor() {
        super({});
    }

    syncWidgets = () => {
        return this.http.
            get('/appregistry/widgets').
            then(res => {
                const widgets: Array<WidgetModel> = [];
                if (res && res.data && res.data.widgets) {
                    res.data.widgets.forEach(w => {
                        let widget = new WidgetModel();
                        widget = w;
                        widget.displayName = w.name;
                        widgets.push(widget);
                    });
                }
                return this.data = widgets;
            });
    }
}