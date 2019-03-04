import { model, idiom as lang, notify } from "entcore";
import http from 'axios';
import { ClassRoom, User, UserTypes, Network, School, SchoolApiResult, Group, EntityType, PersonApiResult, PersonApiResults, PersonApiResultsV2, PersonApiResultV2, removeSpecialChars } from "./model";
export type ClassAdminPreferences = { selectedClassId?: string }

let preferences: ClassAdminPreferences = null;

const cleanObject = (obj) => {
    for (let i in obj) {
        if (obj[i] == null || obj[i] == undefined) {
            delete obj[i];
        }
    }
    return obj;
}
export const directoryService = {
    async getSchoolsForUser(userId: string): Promise<School[]> {
        const res = await http.get('/directory/class-admin/' + userId);
        const personRes: PersonApiResultV2 = res.data;
        return personRes.schools.map(sc => new School().updateFromStructureV2(sc));
    },
    async getCurrentUserDetail(): Promise<PersonApiResult> {
        const res = await http.get("/userbook/api/person");
        const results: PersonApiResults = res.data;
        return results.result[0];
    },
    async updateUser(user: User, { withUserBook, withInfos }: { withUserBook: boolean, withInfos: boolean }) {
        if (withUserBook) {
            for (let i = 0; i < user.hobbies.length; i++)
                if (user.hobbies[i].values === undefined)
                    user.hobbies[i].values = ""
            await http.put('/directory/userbook/' + user.id, cleanObject({
                health: user.health || "",
                hobbies: user.hobbies || [],
                picture: user.picture || "",
                motto: user.motto || "",
                mood: user.moodObj ? user.moodObj.id : "" || ""
            }));
        }
        if (withInfos) {
            await http.put('/directory/user/' + user.id, cleanObject({
                displayName: user.displayName,
                firstName: user.firstName,
                lastName: user.lastName,
                address: user.address,
                email: user.email,
                homePhone: user.homePhone,
                mobile: user.mobile,
                birthDate: user.birthDate,
                childrenIds: user.type == "Relative" ? user.relatives.map(u => u.id) : []
            }));
        }
        user.version++;//increment version (used to update avatar)
    },
    async updateUserLoginAlias(user: User) {
        return await http.put('/directory/user/' + user.id, { loginAlias: user.tempLoginAlias });
    },
    async getPreference(): Promise<ClassAdminPreferences> {
        if (preferences) {
            return preferences;
        }
        try {
            const temp = await http.get("/userbook/preference/classadmin");
            preferences = JSON.parse(temp.data.preference) || {};
        } catch (e) {
            preferences = {};
        }
        preferences = { ...preferences };
        return preferences;
    },
    async savePreference(pref: ClassAdminPreferences) {
        const previous = await directoryService.getPreference();
        const current = { ...previous, ...pref };
        await http.put("/userbook/preference/classadmin", current);
        preferences = current;
        return preferences;
    },
    //birthdate is YYYY-MM-DD
    async saveUserForClass(classId: string, user: { lastName: string, firstName: string, type: UserTypes, birthDate: string, childrenIds?: string[] }) {
        const res = await http.post(`/directory/class/${classId}/user`, user);
        const resUser = new User({ ...user, ...res.data });
        return resUser;
    },
    async searchInDirectory(search: string,
        filters: { structures?: string[], classes?: string[], profiles?: UserTypes[], functions?: string[], types?: EntityType[] } = {},
        allTypes: boolean = false) {
        if (!search)
            search = "";

        const body = {
            ...filters,
            search: search.toLowerCase(),
            types: allTypes ? filters.types : ["User"],
        };
        const response = await http.post('/communication/visible', body);
        const resUsers: User[] = response.data.users;
        const users = resUsers.map((user) => {
            return user;
        }).map(u => new User(u)).sort(User.sortByDisplayName());
        if (allTypes) {
            const resGroups: Group[] = response.data.groups;
            const groups: Group[] = resGroups.map(group => {
                group.isGroup = true;
                return group;
            }).sort(Group.sortByGroupTypeThenName())
            return [...groups, ...users];
        } else {
            return users;
        }
    },
    async fetchNetwork({ withSchools = false } = {}) {
        const network = new Network;
        const res = await http.get('/userbook/structures');
        const schools = (res.data as School[]).map(s => new School(s));
        schools.forEach(school => {
            school.parents = school.parents.filter(parent => {
                const realParent = schools.find(s => s.id == parent.id);
                if (realParent) {
                    realParent.children = realParent.children ? realParent.children : []
                    realParent.children.push(school)
                    return true
                } else
                    return false
            });
            if (school.parents.length === 0)
                delete school.parents
        });
        network.schools = schools;
        if (withSchools) {
            const promises = network.schools.map(school => directoryService.fetchSchool(school.id, { forSchool: school }))
            await Promise.all(promises);
        }
        return network;
    },
    async fetchSchool(id: string, args: { forSchool?: School } = {}) {
        const res = await http.get('/userbook/structure/' + id);
        const resBody: SchoolApiResult = res.data;
        const school = args.forSchool || new School();
        school.updateData({ id, ...resBody });
        return school;
    },
    async fetchClassById(id: string, { withUsers = false } = {}): Promise<ClassRoom> {
        const resHttp = await http.get(`/directory/class/${id}`);
        const schoolClass = new ClassRoom({ ...resHttp.data, id })
        if (withUsers) {
            schoolClass.users = await directoryService.fetchUsersForClass(id);
        }
        return schoolClass;
    },
    async fetchUsersForClass(classId: string): Promise<User[]> {
        const resHttp = await http.get(`/directory/class/${classId}/users`);
        const res: User[] = resHttp.data;
        const sorted = res.map(r => new User(r)).sort(User.sortByLastname());
        return sorted;
    },
    async saveClassInfos(classroom: ClassRoom) {
        await http.put('/directory/class/' + classroom.id, { name: classroom.name, level: classroom.level });
        return classroom;
    },
    async removeUsers(users: User[]) {
        await http.post('/directory/user/delete', { users: users.map(u => u.id) });
        return users;
    },
    findUsers(search: string, users: User[]) {
        const transform = (str: string) => {
            return lang.removeAccents(removeSpecialChars(str) //
                .toLowerCase()) //case insensitive
                .replace(/\s/g, ''); //remove white space
        }
        const searchTerm = transform(search);
        if (!searchTerm) {
            return users;
        }
        return users.filter((user) => {
            let testDisplayName = '', testNameReversed = '', testFullName = '', testFullNameReversed = '';
            if (user.displayName) {
                testDisplayName = transform(user.displayName);
                if (user.displayName.split(' ').length > 0) {
                    testNameReversed = transform(user.displayName.split(' ')[1] + ' '
                        + user.displayName.split(' ')[0]);
                }
            }
            if (user.firstName && user.lastName) {
                testFullName = transform(user.firstName + ' ' + user.lastName);
                testFullNameReversed = transform(user.lastName + ' ' + user.firstName);
            }
            return testDisplayName.indexOf(searchTerm) !== -1 || testNameReversed.indexOf(searchTerm) !== -1
                || testFullName.indexOf(searchTerm) !== -1 || testFullNameReversed.indexOf(searchTerm) !== -1;
        });
    },
    async importFile(file: File, oType: UserTypes, classId: string): Promise<ClassRoom> {
        const type = oType.toLowerCase()
        //
        const form = new FormData();
        form.append(type.replace(/(\w)(\w*)/g, function (g0, g1, g2) { return g1.toUpperCase() + g2.toLowerCase(); }), file);
        form.append('classExternalId', this.externalId);
        try {
            await http.post(`/directory/import/${type}/class/${classId}`, form, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            })
        } catch (e) {
            if (e.response && e.response.data) {
                const error = JSON.parse(e.response.data).message;
                if (error) {
                    const errWithIdx = error.split(/\s/);
                    if (errWithIdx.length === 2) {
                        notify.error(lang.translate(errWithIdx[0]) + errWithIdx[1]);
                    } else {
                        if (error.indexOf('already exists') !== -1) {
                            notify.error('directory.import.already.exists');
                        }
                        else {
                            notify.error(error);
                        }
                    }
                }
            }
        }
        return null;
        //TODO on import finish=> resync class and send an event
        //return directoryService.fetchClass({ withUsers: true });
    },
    async addExistingUserToClass(classId: string, user: User) {
        const res = await http.put(`/directory/class/${classId}/add/${user.id}`);
        return res.data;
    },
    async changeUserClass(user: User, args: { fromClasses: string[], toClass: string }) {
        //LINK before unlink
        await http.put(`/directory/class/${args.toClass}/link/${user.id}`);
        const promises: Promise<any>[] = [];
        for (let from of args.fromClasses) {
            //DONT Unlink previously linked
            if (from != args.toClass)
                promises.push(http.delete(`/directory/class/${from}/unlink/${user.id}`))
        }
        await Promise.all(promises);
        return user;
    },
    async blockUsers(value: boolean, users: User[]) {
        return await http.put('/auth/users/block', { users: users.map(u => u.id), block: value });
    },
    async resetPassword(users: User[]) {
        //TODO do it in single request
        const promises = users.map((user) => {
            return http.post('/auth/sendResetPassword', {
                login: user.login,
                email: model.me.email //SEND TO TEACHER EMAIL
            });
        });
        return Promise.all(promises);
    }
}