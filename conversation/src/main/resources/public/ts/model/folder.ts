import { notify, toFormData, _ } from 'entcore';
import { Conversation } from './conversation';
import { Mail, Mails } from './mail';
import { quota } from './quota';

import { Mix, Eventer, Selection, Selectable } from 'entcore-toolkit';

import http from 'axios';

export abstract class Folder implements Selectable {
    pageNumber: number;
    mails: Mails;
    nbUnread: number;
    api: { get: string, post: string, put: string, delete: string };
    eventer = new Eventer();
    selected: boolean;
    filter: boolean;
    reverse: boolean;
    searchText: string;

    abstract removeSelection();
    abstract sync();
    abstract selectAll();
    abstract deselectAll();

    constructor(api: { get: string, post: string, put: string, delete: string }){
        this.api = api;
        this.filter = false;
        this.reverse = true;
        this.nbUnread = 0;
    }

    getName() {
        if (this instanceof SystemFolder) {
            return this.folderName.toUpperCase();;
        }
        if (this instanceof UserFolder) {
            return this.id;
        }
        return "";
    }

    async nextPage() {
        if (!this.mails.full) {
            this.pageNumber++;
            await this.mails.sync({ pageNumber: this.pageNumber, searchText: this.searchText, emptyList: false });
        }
    }

    async search(text : string) {
        this.searchText = text;
        await this.mails.sync({ pageNumber: 0, searchText: this.searchText, emptyList: true, filterUnread: this.filter });
    }

    async filterUnread(filter : boolean) {
        this.filter = filter;
        this.pageNumber = 0;
        await this.mails.sync({ pageNumber: this.pageNumber, searchText: this.searchText, emptyList: true, filterUnread: this.filter });
    }

    async countUnread () {
        var name = this.getName();
        var restrain;
        if (this instanceof SystemFolder) {
            restrain = '';
        }
        if (this instanceof UserFolder) {
            restrain = '&restrain=';
        }
        const response = await http.get('/conversation/count/' + name + '?unread=true' + restrain)
        this.nbUnread = parseInt(response.data.count);
    }

    async toggleUnreadSelection(unread) {
        await this.mails.toggleUnread(unread);
        await quota.refresh();
        await this.countUnread();
    }
}

export abstract class SystemFolder extends Folder {
    folderName: string;

    constructor(api) {
        super(api);

        var thatFolder = this
        this.pageNumber = 0;
        this.mails = new Mails(api);
    }
}

export class Trash extends SystemFolder {
    userFolders: Selection<UserFolder> = new Selection<UserFolder>([]);
    constructor() {
        super({
            get: '/conversation/list/trash'
        });

        this.folderName = 'trash';
    }

    selectAll(){
        this.mails.selection.selectAll();
        this.userFolders.selectAll();
    }

    deselectAll(){
        this.mails.selection.deselectAll();
        this.userFolders.deselectAll();
    }

    async sync(){
        await this.mails.sync({searchText: this.searchText});
        await this.syncUsersFolders();
    }

    async syncUsersFolders(){
        this.userFolders.all.splice(0, this.userFolders.all.length);
        const response = await http.get('folders/list?trash=')
        response.data.forEach(f => this.userFolders.all.push(Mix.castAs(UserFolder, f)));
    }

    async removeSelection(){
        if(this.mails.selection.selected.length > 0) {
            await this.removeMails();
            await this.mails.removeSelection();
        }
        for(let folder of this.userFolders.selected){
            await folder.delete();
        }
        await quota.refresh();
    }

    async restore(){
        await this.restoreMails();
        for(let folder of this.userFolders.selected){
            await folder.restore();
        }
        await this.syncUsersFolders();
        await this.countUnread();
    }

    async restoreMails () {
        if(!this.mails.selection.length){
            return;
        }
        await http.put('/conversation/restore?' + toFormData({
            id: _.pluck(this.mails.selection.selected, 'id')
        }));
        this.mails.removeSelection();
        Conversation.instance.folders.inbox.mails.refresh();
    }

    async removeMails () {
        const response = await http.delete('/conversation/delete?' + toFormData({
            id: _.pluck(this.mails.selection.selected, 'id')
        }));
        this.mails.removeSelection();
    }
}

export class Inbox extends SystemFolder {
    constructor() {
        super({
            get: '/conversation/list/inbox'
        });

        this.folderName = 'inbox';
    }

    async sync(){
        await this.mails.sync({ searchText: this.searchText});
    }

    async removeSelection(){
        await this.mails.toTrash();
        await quota.refresh();
    }

    selectAll(){
        this.mails.selection.selectAll();
    }

    deselectAll(){
        this.mails.selection.deselectAll();
    }
}

export class Draft extends SystemFolder {
    constructor() {
        super({
            get: '/conversation/list/draft'
        });

        this.folderName = 'draft';
    }

    selectAll(){
        this.mails.selection.selectAll();
    }

    deselectAll(){
        this.mails.selection.deselectAll();
    }

    async sync(){
        await this.mails.sync({ searchText: this.searchText});
    }

    async removeSelection(){
        await this.mails.toTrash();
        await quota.refresh();
    }

    async saveDraft(draft: Mail): Promise<any> {
        await draft.saveAsDraft();
        this.mails.push(draft);
    }

    async transfer(mail: Mail, newMail: Mail) {
        await this.saveDraft(newMail);
        try{
            await http.put("message/" + newMail.id + "/forward/" + mail.id);
            for (var i = 0; i < mail.attachments.length; i++) {
                newMail.attachments.push(JSON.parse(JSON.stringify(mail.attachments[i])))
            }
            quota.refresh();
        }
        catch(e){
            notify.error(e.data.error)
        }
    }
}

export class Outbox extends SystemFolder {
    constructor() {
        super({
            get: '/conversation/list/outbox'
        });

        this.folderName = 'outbox';
    }

    selectAll(){
        this.mails.selection.selectAll();
    }

    deselectAll(){
        this.mails.selection.deselectAll();
    }

    async sync(){
        await this.mails.sync({ searchText: this.searchText});
    }

    async removeSelection(){
        await this.mails.toTrash();
        await quota.refresh();
    }
}

export class UserFolder extends Folder {
    id: string;
    name: string;
    parentFolderId: string;
    parentFolder: UserFolder;
    userFolders: Selection<UserFolder> = new Selection<UserFolder>([]);

    async removeMailsFromFolder(){
        for(let mail of this.mails.selection.selected){
            await mail.removeFromFolder();
        }
        this.mails.removeSelection();
    }

    async removeSelection(){
        await this.mails.toTrash();
        await quota.refresh();
    }

    async open(){
        this.mails.full = false;
        this.pageNumber = 0;
        this.searchText = null;
        this.filter = false;
        Conversation.instance.currentFolder = this;
        await this.sync();
    }

    async sync(){
        await this.mails.sync({ searchText: this.searchText});
        await this.syncUserFolders();
    }

    selectAll(){
        this.mails.selection.selectAll();
    }

    deselectAll(){
        this.mails.selection.deselectAll();
    }

    async syncUserFolders(){
        const response = await http.get('folders/list?parentId=' + this.id);
        this.userFolders.all.splice(0, this.userFolders.colLength);
        for(let f of response.data){
            const folder: UserFolder = Mix.castAs(UserFolder, f);
            folder.parentFolder = this;
            this.userFolders.push(folder);
            await folder.syncUserFolders();
        }
    }

    constructor(data?) {
        super(data);

        this.mails = new Mails(this);
        var thatFolder = this
        this.pageNumber = 0;
    }

    depth(): number {
        var depth = 1;
        var ancestor = this.parentFolder;
        while (ancestor) {
            ancestor = ancestor.parentFolder;
            depth = depth + 1;
        }
        return depth;
    }
    
    async create() {
        var json = !this.parentFolderId ? {
            name: this.name
        } : {
                name: this.name,
                parentId: this.parentFolderId
            }

        return await http.post('folder', json);
    }

    async update() {
        var json = {
            name: this.name
        }
        return await http.put('folder/' + this.id, json)
    }

    async trash() {
        return http.put('folder/trash/' + this.id)
    }

    async restore() {
        return http.put('folder/restore/' + this.id)
    }

    async delete() {
        return http.delete('folder/' + this.id)
    }
}

export class UserFolders{
    all: UserFolder[];
    selection: Selection<UserFolder>;

    forEach(cb: (item: UserFolder, index: number) => void){
        return this.all.forEach(cb);
    }

    async sync(){
        const response = await http.get('folders/list');
        const data = response.data;
        this.all = Mix.castArrayAs(UserFolder, data);
        this.forEach(function (item) {
            item.syncUserFolders()
        });
    }
}

export class SystemFolders {
    sync: any;
    inbox: Inbox;
    trash: Trash;
    outbox: Outbox;
    draft: Draft;
    systemFolders: string[];

    constructor() {
        this.inbox = new Inbox();
        this.trash = new Trash();
        this.draft = new Draft();
        this.outbox = new Outbox();
    }
    
    async openFolder (folderName) {
        Conversation.instance.currentFolder = this[folderName];
        Conversation.instance.currentFolder.searchText = null;
        Conversation.instance.currentFolder.filter = false;
        await Conversation.instance.currentFolder.sync();
        Conversation.instance.currentFolder.pageNumber = 0;
        Conversation.instance.currentFolder.mails.full = false;
        Conversation.instance.currentFolder.eventer.trigger('change');
    }
}