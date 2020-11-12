import {ConnectorModel,IConnector, MappingModel} from '../models/connector.model';
import {Collection, Mix} from 'entcore-toolkit';
import { getType } from '@angular/flex-layout/extended/typings/style/style-transforms';

export class MappingCollection extends Collection<MappingModel> {
    private static _instance : MappingCollection;
    static async getInstance():Promise<MappingCollection>{
        if(!this._instance){
            MappingCollection._instance = new MappingCollection();
            await MappingCollection._instance.getAll();
        }
        return MappingCollection._instance;
    }

    constructor() {
        super({});
    }

    async getAll(){
        const mappings = await this.http.get(`/cas/configuration/mappings`);
        this.data = Mix.castArrayAs(MappingModel, mappings.data);
        return this.data;
    }

    async createMapping(model:MappingModel){
        const res = await this.http.post(`/cas/configuration/mappings`,{...model})
        this.data = Mix.castArrayAs(MappingModel, res.data);
    }

    async getUsage(mappingId:string, structureId?: string)
    {
        const usage = await this.http.get(`/cas/configuration/mappings/${mappingId}/usage` + (structureId != null ? `/${structureId}` : ""));
        return usage;
    }

    async removeMapping(model:MappingModel)
    {
        await this.http.delete(`/cas/configuration/mappings/${model.type}`);
        await this.getAll();
    }

    getMappingId(casType:string, pattern:string):string{
        pattern = pattern || "";
        const mapping = this.data.find(e=>e.casType==casType && e.pattern == pattern);
        return mapping? mapping.type : undefined;
    }
}  

export class ConnectorCollection extends Collection<ConnectorModel> {

    constructor() {
        super({});
    }

    public structureId: string;

    syncConnectors = async () => {
        const instance = await MappingCollection.getInstance();
        const res = await this.http.get(`/appregistry/external-applications?structureId=${this.structureId}`);
        const connectors = new Array<IConnector>();
        
        res.data.forEach(connector => {
            const mapping = instance.getMappingId(connector.data.casType,connector.data.pattern);
            connectors.push({
                id: connector.data.id,
                name: connector.data.name,
                displayName: connector.data.displayName,
                icon: connector.data.icon,
                url: connector.data.address,
                target: connector.data.target,
                inherits: connector.data.inherits,
                locked: connector.data.locked,
                casTypeId: connector.data.casType,
                casMappingId: mapping,
                casPattern: connector.data.pattern,
                oauthScope: connector.data.scope,
                oauthSecret: connector.data.secret,
                oauthGrantType: connector.data.grantType,
                structureId: connector.data.structureId,
                oauthTransferSession: this.setOauthTransferSession(connector.data.scope)
            });
        });
        this.data = Mix.castArrayAs(ConnectorModel, connectors);
    }

    private setOauthTransferSession(scope: string): boolean {
        return scope && scope.startsWith('userinfo');
    }
}
