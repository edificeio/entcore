﻿import { model, notify, idiom as lang, toFormData, moment, _, $ } from 'entcore';

import { User } from './user';
import { Conversation } from './conversation';
import { quota } from './quota';
import { SystemFolder, UserFolder } from './folder';

import { Mix, Eventer, Selection, Selectable } from 'entcore-toolkit';

import http from 'axios';

export class Attachment {
    file: File;
    progress: {
        total: number,
        completion: number
    };
    id: string;
    filename: string;
    size: number;
    contentType: string;

    constructor(file: File) {
        this.file = file;
        this.progress = {
            total: 100,
            completion: 0
        }
    }
}

export class Mail implements Selectable {
    id: string;
    date: string;
    displayNames: string[];
    from: string;
    subject: string;
    body: string;
    to: User[];
    cc: User[];
    unread: boolean;
    state: string;
    parentConversation: Mail;
    newAttachments: FileList;
    loadingAttachments: Attachment[];
    attachments: Attachment[];
    eventer = new Eventer();
    selected: boolean;

    constructor(id?: string) {
        this.id = id;
        this.loadingAttachments = [];
        this.attachments = [];
    }

    getSystemFolder(): string{
        if (this.from !== model.me.userId && this.state === "SENT")
            return 'INBOX';
        if (this.from === model.me.userId && this.state === "SENT")
            return 'OUTBOX';
        if (this.from === model.me.userId && this.state === "DRAFT")
            return 'DRAFT';
        return '';
    }

    matchSystemIcon(): string{
        const systemFolder = this.getSystemFolder();
        if (systemFolder === "INBOX")
            return 'mail-in';
        if (systemFolder === "OUTBOX")
            return 'mail-out';
        if (systemFolder === "DRAFT")
            return 'mail-new';
        return '';
    }

    setMailContent(origin: Mail, mailType: string, compile, sanitize, $scope, copyReceivers?: boolean): Promise<any> {
        if (origin.subject.indexOf(format[mailType].prefix) === -1) {
            this.subject = lang.translate(format[mailType].prefix) + origin.subject;
        }
        else {
            this.subject = origin.subject;
        }

        if (copyReceivers) {
            this.cc = origin.cc;
            this.to = origin.to;
        }

        return new Promise((resolve, reject) => {
            this.body = format[mailType].content + '<blockquote>' + origin.body + '</blockquote>';
            const tempElement = compile(format[mailType].content)($scope);
            setTimeout(function(){
                this.body = $(document.createElement('div')).append(tempElement)[0].outerHTML + '<blockquote>' + this.body + '</blockquote>';
                tempElement.remove()
                resolve();
            }, 0)
        });
    }

    sentDate() {
        return moment(parseInt(this.date)).calendar();
    };

    longDate() {
        return moment(parseInt(this.date)).format('dddd DD MMMM YYYY')
    };

    sender() {
        var that = this;
        return User.prototype.mapUser(this.displayNames, this.from);
    };

    map(id) {
        if (id instanceof User) {
            return id;
        }
        return User.prototype.mapUser(this.displayNames, id);
    };

    async saveAsDraft(): Promise<any> {
            var that = this;
            var data: any = { subject: this.subject, body: this.body };
            data.to = _.pluck(this.to, 'id');
            data.cc = _.pluck(this.cc, 'id');
            if (!data.subject) {
                data.subject = lang.translate('nosubject');
            }
            var path = '/conversation/draft';
            if (this.id) {
                const response = await http.put(path + '/' + this.id, data);
                Mix.extend(this, response.data);
                Conversation.instance.folders.draft.mails.refresh();
            }
            else {
                if (this.parentConversation) {
                    path += '?In-Reply-To=' + this.parentConversation.id;
                }
                let response = await http.post(path, data)
                Mix.extend(this, response.data);
                Conversation.instance.folders.draft.mails.refresh();
            }
    };

    async send() {
        var data: any = { subject: this.subject, body: this.body };
        data.to = _.pluck(this.to, 'id');
        data.cc = _.pluck(this.cc, 'id');
        if (data.to.indexOf(model.me.userId) !== -1) {
            Conversation.instance.folders['inbox'].nbUnread++;
        }
        if (data.cc.indexOf(model.me.userId) !== -1) {
            Conversation.instance.folders['inbox'].nbUnread++;
        }
        var path = '/conversation/send?';
        if (!data.subject) {
            data.subject = lang.translate('nosubject');
        }
        if (this.id) {
            path += 'id=' + this.id + '&';
        }
        if (this.parentConversation) {
            path += 'In-Reply-To=' + this.parentConversation.id;
        }

        try{
            const response = await http.post(path, data);
            const result = response.data;
            Conversation.instance.folders['outbox'].mails.refresh();
            Conversation.instance.folders['draft'].mails.refresh();

            if (parseInt(result.sent) > 0) {
                notify.info('mail.sent');
            }
            var inactives = '';
            result.inactive.forEach(function (name) {
                inactives += name + ' ' + lang.translate('invalid') + '<br />';
            });
            if (result.inactive.length > 0) {
                notify.info(inactives);
            }
            var undelivered = result.undelivered.join(', ');
            if (result.undelivered.length > 0) {
                notify.error(undelivered + lang.translate('undelivered'));
            }
        }
        catch(e){
            notify.error(e.response.data.error);
        }
    };

    async open() {
        if(this.unread){
            Conversation.instance.currentFolder.nbUnread --;
        }
        this.unread = false;
        let response = await http.get('/conversation/message/' + this.id)
        Mix.extend(this, response.data);
        this.to = this.to.map(user => (
            Mix.castAs(User, {
                id: user,
                displayName: this.displayNames.find(name => name[0] === user as any)[1]
            })
        ));

        this.cc = this.cc.map(user => (
            Mix.castAs(User, {
                id: user,
                displayName: this.displayNames.find(name => name[0] === user as any)[1]
            })
        ));

        Conversation.instance.folders['inbox'].countUnread();
        Conversation.instance.currentFolder.mails.refresh();
    };

    async remove() {
        if ((Conversation.instance.currentFolder as SystemFolder).folderName !== 'trash') {
            await http.put('/conversation/trash?id=' + this.id);
            Conversation.instance.currentFolder.mails.refresh();
            Conversation.instance.folders['trash'].mails.refresh();
        }
        else {
            await http.delete('/conversation/delete?id=' + this.id);
            Conversation.instance.folders['trash'].mails.refresh();
        }
    };

    async removeFromFolder() {
        return http.put('move/root?id=' + this.id)
    }

    async move(destinationFolder) {
        await http.put('move/userfolder/' + destinationFolder.id + '?id=' + this.id);
        await Conversation.instance.currentFolder.mails.refresh();
    }

    async trash() {
        await http.put('/conversation/trash?id=' + this.id);
        await Conversation.instance.currentFolder.mails.refresh();
    }

    postAttachments($scope) {
        const promises: Promise<any>[] = [];
        for(let i = 0; i < this.newAttachments.length; i++){
            const targetAttachment = this.newAttachments[i];
            const attachmentObj = new Attachment(targetAttachment);
            this.loadingAttachments.push(attachmentObj)

            const formData = new FormData()
            formData.append('file', attachmentObj.file)

            const promise = http.post("message/" + this.id + "/attachment", formData, {
                onUploadProgress: (e: ProgressEvent) => {
                    if (e.lengthComputable) {
                        var percentage = Math.round((e.loaded * 100) / e.total);
                        attachmentObj.progress.completion = percentage;
                        $scope.$apply();
                    }
                }
            })
            .then(response => {
                this.loadingAttachments.splice(this.loadingAttachments.indexOf(attachmentObj), 1);
                attachmentObj.id = response.data.id;
                attachmentObj.filename = attachmentObj.file.name;
                attachmentObj.size = attachmentObj.file.size;
                attachmentObj.contentType = attachmentObj.file.type;
                this.attachments.push(attachmentObj);
                quota.refresh();
                $scope.$apply();
            })
            .catch(e => {
                this.loadingAttachments.splice(this.loadingAttachments.indexOf(attachmentObj), 1);
                notify.error(e.response.data.error);
            });

            promises.push(promise)
        }

        return Promise.all(promises);
    }
    

    async deleteAttachment(attachment) {
        this.attachments.splice(this.attachments.indexOf(attachment), 1);
        await http.delete("message/" + this.id + "/attachment/" + attachment.id);
        quota.refresh();
    }
}

export class Mails {
    pageNumber: number;
    api: { get: string, put: string, post: string, delete: string };
    full: boolean;
    selection: Selection<Mail>;
    userFolder: UserFolder;

    push(item: Mail){
        this.all.push(item);
    }

    get all(): Mail[]{
        return this.selection.all;
    }

    constructor(api: { get: string, put: string, post: string, delete: string } | UserFolder) {
        if(api instanceof UserFolder){
            this.userFolder = api;
        }
        else{
            this.api = api;
        }
        
        this.selection = new Selection<Mail>([]);
    }

    async removeFromFolder(){
        await http.put('move/root?' + toFormData({ id: _.pluck(this.selection.selected, 'id') }))
    }

    addRange(arr: Mail[]){
        if(!(arr[0] instanceof Mail)){
            arr.forEach(d => this.all.push(Mix.castAs(Mail, d)));
        }
        else{
            arr.forEach(d => this.all.push(d));
        }
    }

    async sync(data?: { pageNumber?: number, emptyList?: boolean }){
        if(this.userFolder){
            await this.userFolderSync(data);
        }
        else{
            await this.apiSync(data);
        }
    }

    async userFolderSync(data?: { pageNumber?: number, emptyList?: boolean }){
        if(!data){
            data = {};
        }
        if (!data.pageNumber) {
            data.pageNumber = 0;
        }
        const response = await http.get('/conversation/list/' + this.userFolder.id + '?restrain=&page=' + data.pageNumber);
        if(data.emptyList !== false){
            this.all.splice(0, this.all.length);
        }
        response.data.forEach(m => this.all.push(Mix.castAs(Mail, m)));
        if (response.data.length === 0) {
            this.full = true;
        }
    }

    async apiSync(data?: { pageNumber?: number, emptyList?: boolean }): Promise<void>{
        if (!data) {
            data = {};
        }
        if (!data.pageNumber) {
            data.pageNumber = 0;
        }

        let response = await http.get(this.api.get + '?page=' + data.pageNumber);
        if(data.emptyList !== false){
            this.all.splice(0, this.all.length);
        }

        this.addRange(response.data);
        if (response.data.length === 0) {
            this.full = true;
        }
    }

    refresh() {
        this.pageNumber = 0;
        return this.sync();
    }

    async toTrash() {
        await http.put('/conversation/trash?' + toFormData({ id: _.pluck(this.selection.selected, 'id') }));
        Conversation.instance.folders.trash.mails.refresh();
        quota.refresh();
        this.selection.removeSelection();
    }

    removeSelection(){
        this.selection.removeSelection();
    }

    async moveSelection(destinationFolder) {
        await http.put('move/userfolder/' + destinationFolder.id + '?' + toFormData({ id: _.pluck(this.selection.selected, 'id') }));
        Conversation.instance.currentFolder.mails.refresh();
    }
}

let mailFormat = {
    reply: {
        prefix: 'reply.re',
        content: ''
    },
    transfer: {
        prefix: 'reply.fw',
        content: ''
    }
};

http.get('/conversation/public/template/mail-content/transfer.html').then(response => {
    format.transfer.content = response.data;
});

http.get('/conversation/public/template/mail-content/reply.html').then(response => {
    format.reply.content = response.data;
});

export const format = mailFormat;