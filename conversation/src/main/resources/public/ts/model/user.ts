import { idiom as lang, _, model, } from 'entcore';

import { Mix, Selection, Selectable, Eventer } from 'entcore-toolkit';

import http from 'axios';

export class User implements Selectable {
    displayName: string;
    name: string;
    profile: string;
    id: string;
    selected: boolean;
    isGroup: boolean;

    constructor(id?: string, displayName?: string){
        this.displayName = displayName;
        this.id = id;
    }

    toString() {
        return (this.displayName || '') + (this.name || '') + (this.profile ? ' (' + lang.translate(this.profile) + ')' : '')
    }

    async findData(): Promise<boolean> {
        var that = this;
        const response = await http.get('/userbook/api/person?id=' + this.id);
        const userData = response.data;
        if (!userData.result[0]) // If group
            return false;
        Mix.extend(this, { id: that.id, displayName: userData.result[0].displayName });

        return true;
    }

    mapUser(displayNames, id) {
        return _.map(_.filter(displayNames, function (user) {
            return user[0] === id;
        }), function (user) {
            return new User(user[0], user[1]);
        })[0];
    }

    isMe() {
        return model.me.userId == this.id;
    }

    isAGroup() {
        if (!this.id)
            return false;
        return this.id.length < 36;
    }
}

export class Users {
    eventer = new Eventer();
    searchCachedMap = {};

    async sync(search: string){
        let newArr = [];
        const response = await http.get('/conversation/visible?search=' + search);
        response.data.groups.forEach(group => {
            group.isGroup = true;
            newArr.push(Mix.castAs(User, group));
        });

        newArr = newArr.concat(Mix.castArrayAs(User, response.data.users));
        return newArr;
    }

    async findUser (search, include, exclude): Promise<User[]> {
        const startText = search.substr(0, 3);
        if(!this.searchCachedMap[startText]){
            this.searchCachedMap[startText] = await this.sync(startText);
        }
        var searchTerm = lang.removeAccents(search).toLowerCase();
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