import {Model} from 'entcore-toolkit';
import { UserCollection } from '../collections/user.collection';
import { GroupCollection } from '../collections/group.collection';
import { SubjectCollection } from '../collections/subject.collection';
import { ApplicationCollection } from '../collections/application.collection';
import { ConnectorCollection } from '../collections/connector.collection';
import { WidgetCollection } from '../collections/widget.collection';

export type ClassModel = { id: string, name: string };

export class StructureModel extends Model<StructureModel> {
    UAI?: string;
    externalId?: string;
    name?: string;
    parents?: Array<{ id: string, name: string }>;
    children?: StructureModel[];
    users: UserCollection;
    removedUsers: UserCollection;
    classes: Array<ClassModel> = [];
    groups: GroupCollection;
    subjects: SubjectCollection;
    applications: ApplicationCollection;
    connectors: ConnectorCollection;
    widgets: WidgetCollection;
    userSources: string[] = [];
    source?: string;
    profiles: { name: string, blocked: any }[] = [];
    aafFunctions: Array<Array<Array<string>>> = [];
    levelsOfEducation: number[] = [];
    distributions: string[];
    timetable: string;
    punctualTimetable?: string;
    hasApp?: boolean;
    manualName?: boolean;
    feederName?: string;
    ignoreMFA?: boolean;
    joinKey?: string[];
    exports?: string[];

    constructor() {
        super({});
        this.users = new UserCollection();
        this.removedUsers = new UserCollection("/directory/structure/:structureId/removedUsers");
        this.groups = new GroupCollection();
        this.subjects = new SubjectCollection();
        this.applications = new ApplicationCollection();
        this.connectors = new ConnectorCollection();
        this.widgets = new WidgetCollection();
    }

    _id?: string;
    set id(id: string) {
        this.users.structureId = id;
        this.removedUsers.structureId = id;
        this.groups.structureId = id;
        this.subjects.structureId = id;
        this.applications.structureId = id;
        this.connectors.structureId = id;
        this._id = id;
    }

    get id() {
        return this._id;
    }

    static AUTOMATIC_SOURCES_REGEX = /AAF/;
    get isSourceAutomatic() {
        return this.source && StructureModel.AUTOMATIC_SOURCES_REGEX.test(this.source);
    }

    quickSearchUsers(input: string) {
        return this.http.get(`/directory/structure/${this.id}/quicksearch/users`, {
            params: {input}
        });
    }

    syncClasses(force?: boolean) {
        if (this.classes.length < 1 || force === true) {
            return this.http.get('/directory/class/admin/list', {params: {structureId: this.id}})
                .then(res => this.classes = res.data);
        }
        return Promise.resolve();
    }

    syncGroups(force?: boolean) {
        if (this.groups.data.length < 1 || force === true) {
            return this.groups.sync().then(() => Promise.resolve(this.groups));
        }
        return Promise.resolve();
    }

    syncSubjects(force?: boolean) {
        if (this.subjects.data.length < 1 || force === true) {
            return this.subjects.sync().then(() => Promise.resolve(this.subjects));
        }
        return Promise.resolve();
    }

    syncSources(force?: boolean) {
        if (this.userSources.length < 1 || force === true) {
            return this.http.get(`/directory/structure/${this.id}/sources`)
                .then(res => {
                    if (res.data && res.data.length > 0) {
                        this.userSources = res.data[0].sources;
                    }
                });
        }
        return Promise.resolve();
    }

    syncAafFunctions(force?: boolean) {
        if (this.aafFunctions.length < 1 || force === true) {
            return this.http.get(`/directory/structure/${this.id}/aaffunctions`)
                .then(res => {
                    if (res.data && res.data.length > 0
                        && res.data[0].aafFunctions && res.data[0].aafFunctions.length > 0) {
                        this.aafFunctions = res.data[0].aafFunctions;
                    }
                });
        }
        return Promise.resolve();
    }

    is1D(): boolean
    {
        return this.levelsOfEducation.indexOf(1) != -1;
    }

    is2D(): boolean
    {
        return this.levelsOfEducation.indexOf(2) != -1;
    }
}