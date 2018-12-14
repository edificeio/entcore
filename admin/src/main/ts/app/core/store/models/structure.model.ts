import { UserCollection, GroupCollection, ApplicationCollection, ConnectorCollection } from '..';
import { Model } from 'entcore-toolkit'

export class StructureModel extends Model<StructureModel> {

    constructor() {
        super({})
        this.users = new UserCollection()
        this.groups = new GroupCollection()
        this.applications = new ApplicationCollection()
        this.connectors = new ConnectorCollection()
    }

    _id?: string
    set id(id: string) {
        this.users.structureId = id
        this.groups.structureId = id
        this.applications.structureId = id
        this.connectors.structureId = id
        this._id = id
    }
    get id() { return this._id }

    UAI?: string
    externalId?: string
    name?: string
    parents?: [{ id: string, name: string }]
    children?: StructureModel[]
    users: UserCollection
    classes: Array<{id: string, name: string}> = []
    groups: GroupCollection
    applications: ApplicationCollection
    connectors: ConnectorCollection
    sources: string[] = []
    profiles: {name: string, blocked: any}[] = []
    aafFunctions: string[] = []

    quickSearchUsers(input: string) {
        return this.http.get(`/directory/structure/${this.id}/quicksearch/users`, {
            params: { input: input }
        })
    }

    syncClasses(force?: boolean) {
        if (this.classes.length < 1 || force === true) {
            return this.http.get('/directory/class/admin/list', { params: { structureId: this.id }})
                .then(res => this.classes = res.data)
        }
        return Promise.resolve()
    }

    syncGroups(force?: boolean) {
        if (this.groups.data.length < 1 || force === true) {
            return this.groups.sync().then(() => Promise.resolve(this.groups))
        }
        return Promise.resolve()
    }

    syncSources(force?: boolean) {
        if (this.sources.length < 1 || force === true) {
            return this.http.get(`/directory/structure/${this.id}/sources`)
                .then(res => {
                    if (res.data && res.data.length > 0) {
                        this.sources = res.data[0]['sources']
                    }
                })
        }
        return Promise.resolve()
    }

    syncAafFunctions(force?: boolean) {
        if (this.aafFunctions.length < 1 || force === true) {
            return this.http.get(`/directory/structure/${this.id}/aaffunctions`)
                .then(res => {
                    if (res.data && res.data.length > 0 
                        && res.data[0]['aafFunctions'] && res.data[0]['aafFunctions'].length > 0) {
                        this.aafFunctions = res.data[0]['aafFunctions'];
                    }
                })
        }
        return Promise.resolve()
    }
}