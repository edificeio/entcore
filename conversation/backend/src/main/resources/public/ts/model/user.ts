﻿import { idiom as lang, _, model, } from 'entcore';

import { Mix, Selection, Selectable, Eventer } from 'entcore-toolkit';

import http from 'axios';

export class User implements Selectable {
    displayName: string;
    name: string;
    profile: string;
    id: string;
    selected: boolean;
    isGroup: boolean;

    constructor(id?: string, displayName?: string, profile?: string, isGroup?: boolean){
        this.id = id;
        this.profile = profile;
        this.isGroup = isGroup;
        if (this.isGroup) {
            this.name = displayName;
        }
        this.displayName = displayName;
    }

    toString() {
        return (this.displayName || '') + (this.name || '');
    }

    async findData(): Promise<boolean> {
        var that = this;
        const response = await http.get('/userbook/api/person?id=' + this.id);
        const userData = response.data;
        if (!userData.result[0]) // If group
            return true;
        // If deleted ??
        var result = userData.result[0];
        Mix.extend(this, { id: that.id, displayName: result.displayName, profile: result.type[0] });

        return true;
    }

    async findGroupData(): Promise<any> {
        const response = await http.get('/directory/group/' + this.id);
        Mix.extend(this, { id: this.id, name: response.data.name, isGroup: true });
    }

    mapUser(displayNames, id) {
        return _.map(_.filter(displayNames, function (user) {
            return user[0] === id;
        }), function (user) {
            return new User(user[0], user[1], null, user[2]);
        })[0];
    }

    isMe() {
        return model.me.userId == this.id;
    }

    isAGroup() {
        if (!this.id)
            return false;
        return this.isGroup;
    }
}

export class Users {
    eventer = new Eventer();
    searchCachedMap = {};

    async sync(search: string){
        let newArr = [];
        var response = await http.get('/directory/sharebookmark/all');
        var bookmarks = _.map(response.data, function(bookmark) {
            bookmark.type = 'sharebookmark';
            return bookmark;
        });
        newArr = Mix.castArrayAs(User, bookmarks);
        response = await http.get('/conversation/visible?search=' + search);
        response.data.groups.forEach(group => {
            group.isGroup = true;
            newArr.push(Mix.castAs(User, group));
        });
        newArr = newArr.concat(Mix.castArrayAs(User, response.data.users));

        return newArr;
    }

    async findUser (search, include, exclude, restriction?: boolean): Promise<User[]> {
        const startText = restriction ? search.text.substr(0, 3) : '';
        if(!this.searchCachedMap[startText]){
            this.searchCachedMap[startText] = [];
            this.searchCachedMap[startText] = await this.sync(startText);
        }
        var searchTerm = lang.removeAccents(search.text).toLowerCase();
        var found = _.filter(
            this.searchCachedMap[startText].filter(function (user) {
                var includeUser = _.findWhere(include, { id: user.id });
                if(includeUser !== undefined)
                    includeUser.profile = user.profile;
                return includeUser === undefined;
            })
            .concat(include), function (user) {
                var testDisplayName = '', testNameReversed = '';
                if (user.displayName) {
                    testDisplayName = lang.removeAccents(user.displayName).toLowerCase();
                    testNameReversed = lang.removeAccents(user.displayName.split(' ')[1] + ' '
                        + user.displayName.split(' ')[0]).toLowerCase();
                }
                var testName = '';
                if (user.name) {
                    testName = lang.removeAccents(user.name).toLowerCase();
                }

                return testDisplayName.indexOf(searchTerm) !== -1 ||
                    testNameReversed.indexOf(searchTerm) !== -1 ||
                    testName.indexOf(searchTerm) !== -1;
            }
        );

        return _.reject(found, function (element) {
            return _.findWhere(exclude, { id: element.id });
        });
    }
}