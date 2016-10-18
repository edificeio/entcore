import { idiom as lang } from 'entcore/entcore';
import { _ } from 'entcore/libs/underscore/underscore';

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

    async findData(): Promise<void> {
        var that = this;
        const response = await http.get('/userbook/api/person?id=' + this.id);
        const userData = response.data;
        Mix.extend(this, { id: that.id, displayName: userData.result[0].displayName });
    }

    mapUser(displayNames, id) {
        return _.map(_.filter(displayNames, function (user) {
            return user[0] === id;
        }), function (user) {
            return new User(user[0], user[1]);
        })[0];
    }
}

export class Users {
    selection: Selection<User> = new Selection<User>([]);
    eventer = new Eventer();

    get all(): User[]{
        return this.selection.all;
    }

    async sync(){
        const response = await http.get('/conversation/visible');
        response.data.groups.forEach(group => {
            group.isGroup = true;
            this.selection.push(Mix.castAs(User, group));
        });

        this.selection.all = this.selection.all.concat(Mix.castArrayAs(User, response.data.users));
        this.eventer.trigger('sync');
    }

    findUser (search, include, exclude): User[] {
        var searchTerm = lang.removeAccents(search).toLowerCase();
        if (!searchTerm) {
            return [];
        }
        var found = _.filter(
                this.all.filter(function (user) {
                    return _.findWhere(include, { id: user.id }) === undefined
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

    isGroup (id) {
        return this.all.find(u => u.isGroup && u.id === id);
    }
}