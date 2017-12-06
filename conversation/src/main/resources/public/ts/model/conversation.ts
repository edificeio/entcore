// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

import { idiom as lang, model, Model, notify, Collection, _, moment } from 'entcore';

import { Folder, UserFolder, UserFolders, SystemFolder, SystemFolders } from './folder';
import { User, Users } from './user';
import { quota } from './quota';

import { Eventer } from 'entcore-toolkit';

import http from 'axios';

export const sorts = {
    name: function (mail) {
        var systemFolder = mail.getSystemFolder();
        if (systemFolder === 'INBOX') {
            if (mail.fromName)
                return mail.fromName;
            else
                return mail.sender().displayName;
        }
        return _.chain(mail.displayNames)
            .filter(function (u) { return mail.to.indexOf(u[0]) >= 0 })
            .map(function (u) { return u[1] }).value().sort();
    },
    subject: function (mail) {
        return mail.subject;
    },
    date: function (mail) {
        return mail.date;
    },
    systemFolder: function (mail) {
        var systemFolder = mail.getSystemFolder();
        if (systemFolder === "INBOX")
            return 1
        if (systemFolder === "OUTBOX")
            return 2
        if (systemFolder === "DRAFT")
            return 3
        return 0
    },
    unread: function(mail){
        return mail.unread;
    }
}

export class Conversation {
    folders: SystemFolders;
    userFolders: UserFolders;
    users: Users;
    systemFolders: string[];
    currentFolder: Folder;
    maxFolderDepth: number;
    eventer = new Eventer();
    preference = {useSignature: false, signature: ""};

    static _instance: Conversation;
    static get instance(): Conversation{
        if(!this._instance){
            this._instance = new Conversation();
        }
        return this._instance;
    }

    constructor() {
        this.users = new Users();
        this.folders = new SystemFolders();
        this.userFolders = new UserFolders();

        this.folders.inbox.countUnread();
    }

    async sync() {
        let response = await http.get('max-depth')
        this.maxFolderDepth = parseInt(response.data['max-depth']);
        this.eventer.trigger('change');
        await this.getPreference();
        await this.userFolders.sync();
        await quota.refresh();
    }

    async getPreference() {
        try{
            let response = await http.get('/userbook/preference/conversation')
            if(response.data.preference)
                this.preference = JSON.parse(response.data.preference)
        }
        catch(e){
            notify.error(e.response.data.error);
        }
    }

    async putPreference() {
        await http.put('/userbook/preference/conversation', this.preference);
    }
}