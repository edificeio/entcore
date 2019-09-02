import { workspace, Document } from "entcore";


const workspaceService = workspace.v2.service;
import WorkspacePreferenceView = workspace.v2.WorkspacePreferenceView;
import models = workspace.v2.models;
export { workspaceService, models, WorkspacePreferenceView, Document }

export type CursorUpdate = (args: { all: Document[], diff: Document[] }) => void;

export interface DocumentCursorParams {
    params: workspace.v2.ElementQuery;
    sort?: "name" | "created";
    args?: {
        directlyShared: boolean;
    }
}
export class DocumentCursor {
    _last: Array<Document> = [];
    private _lastCount = -1;
    private _skip: number = 0;
    private _fetching = false;
    private _all: Array<Document> = [];
    get all() { return this._all }
    get last() { return this._last }
    constructor(private params: DocumentCursorParams, private _onUpdate: CursorUpdate, private _limit: number = 100) { }
    hasNext(): boolean {
        return (this._lastCount != 0);
    }
    async next(): Promise<void> {
        try {
            if (this._fetching) return;
            if (this.hasNext()) {
                this._fetching = true;
                this._last = await workspaceService.fetchDocuments({ ...this.params.params, skip: this._skip, limit: this._limit }, this.params.sort, this.params.args);
                this._lastCount = this._last.length;
                this._skip += this._lastCount;
                this._all = [...this.all, ...this._last];
                this._onUpdate({ all: this.all, diff: this._last });
            }
        } finally {
            this._fetching = false;
        }
    }
}