import { idiom as lang, ui, Model, model, moment, _ } from 'entcore';
import http from 'axios';

// === Type definitions
type Hobby = { visibility: string, values: string, category: string, displayName: string };
type Structure = { classes: string[], name: string, id: string };
type StructureV2 = { classes: Array<{ name: string, id: string }>, name: string, id: string };
export type UserTypes = "Student" | "Relative" | "Teacher" | "Personnel";
export type EntityType = "User" | "Group";

// Person API result
export interface PersonApiResult extends Partial<User> {
    relativeList: { relatedName: string, relatedId: string, relatedType: UserTypes }[];
    schools: Structure[];
    hobbies: Hobby[];
    mood: string;
}
export type PersonApiResults = { result: { [key: string]: PersonApiResult } };

// Person API result V2 (see more personnal infos)
export interface PersonApiResultV2 {
    relativeList: { relatedName: string, relatedId: string, relatedType: UserTypes }[];
    schools: StructureV2[];
    hobbies: Hobby[];
    mood: string;
}
export type PersonApiResultsV2 = { result: { [key: string]: PersonApiResultV2 } };

// Structure API result

export interface StructureApiResult {
    classes: Partial<ClassRoom>[];
    users: Partial<User>[];
}

// School Api Result
export interface SchoolApiResult {
    classes: ClassRoom[];
    users: User[];
}
// === private functions
const capitalize = (s) => {
    if (typeof s !== 'string') return ''
    return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}
const capitalizeFully = (str) => {
    return str.replace(/\w\S*/g, function (txt) {
        return capitalize(txt);
    });
}
export const removeSpecialChars = (str) => {
    if (!str) return "";
    const lower = str.toLowerCase();
    const upper = str.toUpperCase();
    let res = "";
    for (let i = 0; i < lower.length; ++i) {
        if (lower[i] != upper[i] || lower[i].trim() === '')
            res += str[i];
    }
    return res;
}
// === Models
export class User extends Model {
    version = 0;
    //
    id: string;
    login: string;
    tempLoginAlias: string;
    originalLogin: string;
    blocked: boolean;
    type: UserTypes;
    profile: UserTypes;
    resetCode: string;
    activationCode: string;
    source: "MANUAL" | "CLASS_PARAM" | "BE1D" | "CSV"
    //mood: "default" = 'default';
    motto: string;
    moodObj: Mood;
    _birthDateISO: string;
    firstName: string;
    lastName: string;
    email: string;
    homePhone: string;
    mobile: string;
    displayName: string;
    _hobbies: Hobby[] = [];
    relatives: User[] = [];
    profiles: UserTypes[] = [];
    attachedStructures: Structure[] = [];
    classIds: string[] = [];
    childrenStructure: Structure[] = []
    selected: boolean;
    schools: Array<{ classes: string[], name: string, id: string }> = []
    //from account/userbook
    health: string;
    picture: string;
    address: string;
    hasEmail: boolean;
    //
    constructor(data?: Partial<User>) {
        super(data);
    }
    get safeHasEmail() {
        return this.hasEmail || !!this.email;
    }
    get firstClassName(): string {
        try {
            const clazz = this.schools[0].classes[0];
            if (typeof clazz == "string") {
                return clazz;
            } else {
                return clazz["name"];
            }
        } catch (e) {
            return "-";
        }
    }
    get classNames(): string[] {
        try {
            return this.schools[0].classes.map(function (clazz) {
                if (typeof clazz == "string") {
                    return clazz;
                } else {
                    return clazz["name"];
                }
            }).filter(function (clazz) {
                return clazz != null && clazz != undefined
            });
        } catch (e) {
            return ["-"];
        }
    }
    get safeLastName() {
        return this.lastName;
    }
    set safeLastName(name: string) {
        this.lastName = name ? removeSpecialChars(name.toUpperCase()) : name;
    }
    get safeFirstName() {
        return this.firstName;
    }
    set safeFirstName(name: string) {
        this.firstName = name ? removeSpecialChars(capitalizeFully(name)) : name;
    }
    get birthDate() {
        return this._birthDateISO;
    }
    set birthDate(b) {
        this._birthDateISO = b ? moment(b).format('YYYY-MM-DD') : undefined;
    }
    get safeDisplayName() {
        return this.displayName || `${this.lastName} ${this.firstName}`;
    }
    set safeDisplayName(name: string) {
        this.displayName = name ? removeSpecialChars(name) : name;
    }
    get resetCodeDate() {
        //TODO 
        return "TODO";
    }
    get hasBirthDate() {
        return this.birthDate !== '';
    }
    get shortBirthDate() {
        return this.hasBirthDate ? moment(this.birthDate).format('DD/MM/YYYY') : '';
    }
    get inverseBirthDate() {
        return this.hasBirthDate ? moment(this.birthDate).format('YYYYMMDD') : '';
    }
    get isMe() {
        return this.id === model.me.userId;
    }
    get safePicture() {
        const uri = this.picture || this.avatar48Uri || "";
        if (uri.indexOf("?") == -1) {
            return `${uri}?version=${this.version}`;
        } else {
            return `${uri}&version=${this.version}`;
        }
    }
    get avatar48Uri() {
        return `/userbook/avatar/${this.id}?thumbnail=48x48`;
    }
    get avatarUri() {
        return `/userbook/avatar/${this.id}`;
    }
    get editUserUri() {
        return `/userbook/mon-compte#edit-user/${this.id}`;
    }
    get editUserInfosUri() {
        return `/userbook/mon-compte#edit-user-infos/${this.id}`;
    }
    get isDisabled() {
        return this.blocked || this.isMe;
    }
    updateData(data: Partial<User>) {
        /*if (data) {
            data.mood = data.mood || 'default'
        }*/
        super.updateData(data);
    }
    async open({ withChildren = false }) {
        const data: PersonApiResult = (await http.get('/directory/class-admin/' + this.id)).data;
        const hobbies = data.hobbies.filter(u => u.category).map(u => {
            const displayName = lang.translate('userBook.hobby.' + u.category) || u.category;
            return { ...u, displayName }
        }).sort((a1, a2) => {
            return a1.displayName.localeCompare(a2.displayName);
        })
        const relatives = data.relativeList.filter(item => !!item.relatedId && item.relatedId != "").map(item => new User({ displayName: item.relatedName, id: item.relatedId, type: item.relatedType }));
        const attachedStructures: Structure[] = data.schools;
        if (!data) {
            this.id = undefined;
            return;
        }
        const classIds: string[] = [];
        for (let school of attachedStructures) {
            for (let clazz of school.classes) {
                if (clazz["id"]) {
                    classIds.push(clazz["id"])
                }
            }
        }
        data.moodObj = new Mood(data.mood);
        //
        if (withChildren && this.type == "Relative") {
            await this.loadChildren();
        }
        //
        this.updateData({ ...data, hobbies, relatives, attachedStructures, classIds });
    }
    get nonEmptyHobbies() { return this._hobbies.filter(hobby => hobby.values) }
    get hobbies() { return this._hobbies; }
    set hobbies(h) { this._hobbies = h }
    async loadChildren() {
        const res = await http.get(`/directory/user/${this.id}/children`);
        const childrenStructure: Structure[] = res.data;
        this.updateData({ childrenStructure });
    }
    get safeMood() { return this.moodObj }
    set safeMood(m) {
        this.moodObj = m;
    }
    getProfileName() {
        return lang.translate("directory." + this.getProfileType());
    }
    getProfile() {
        return ui.profileColors.match(this.getProfileType());
    }
    getProfileType() {
        if (this.profile)
            return this.profile;
        else if (this.type) {
            return this.type;
        }
        else
            this.profiles[0];
    }
    static sortByDisplayName() {
        return (u1: User, u2: User) => u1.displayName > u2.displayName ? 1 : -1;
    }
    static sortByLastname() {
        return (u1: User, u2: User) => u1.lastName > u2.lastName ? 1 : -1;
    }
}

export class Group extends Model {
    groupType: string;
    sortName: string;
    name: string;
    isGroup: boolean = true;
    static sortByGroupName() {
        return (a: Group, b: Group) => (a.sortName || a.name || "").localeCompare(b.sortName || b.name || "");
    }
    static sortByGroupType() {
        return (a: Group, b: Group) => (a.groupType || "").localeCompare(b.groupType || "");
    }
    static sortByGroupTypeThenName() {
        const sort1 = Group.sortByGroupType();
        const sort2 = Group.sortByGroupName();
        return (a: Group, b: Group) => {
            const res = sort1(a, b);
            if (res == 0) {
                return sort2(a, b);
            }
            return res;
        };
    }
}

export class ClassRoom extends Model {
    id: string;
    name: string;
    level: string;
    users: User[] = [];
    externalId: string;
    constructor(data?: Partial<ClassRoom>) {
        super(data);
    }
    addUserIfNotExists(user: User) {
        const founded = this.users.find(u => u.id == user.id);
        if (!founded) {
            this.users.push(user);
        }
    }
}

export class School extends Model {
    id: string;
    name: string;
    users: User[] = [];
    parents: School[] = [];
    children: School[] = [];
    classrooms: ClassRoom[] = [];
    constructor(data?: Partial<School>) {
        super(data);
    }
    updateFromStructureV2(data: StructureV2) {
        this.classrooms = [];
        for (let clazz of (data as StructureV2).classes) {
            if (clazz.id && clazz.name) {
                this.classrooms.push(new ClassRoom(clazz));
            }
        }
        this.id = data.id;
        this.name = data.name;
        return this;
    }
    updateData(data?: Partial<School> | SchoolApiResult) {
        super.updateData(data);
        if (data) {
            if ((data as School).users) {
                this.users = (data as School).users.map(u => new User(u));
            }
            if ((data as SchoolApiResult).classes) {
                this.classrooms = (data as SchoolApiResult).classes.map(u => new ClassRoom(u));
            }
        }
    }
}


export class Network extends Model {
    schools: School[] = [];
    constructor(data?: Partial<Network>) {
        super(data);
    }
    get allClassrooms() {
        let classrooms = [];
        this.schools.forEach((school) => {
            classrooms = classrooms.concat(school.classrooms);
        });
        return classrooms;
    }
    getSchoolByClassId(classId: string): School {
        for (let school of this.schools) {
            for (let classroom of school.classrooms) {
                if (classroom.id === classId) {
                    return school;
                }
            }
        }
        return undefined;
    }

}

export class Mood {
    icon: string;
    text: string;
    id: string;
    constructor(mood: string) {
        if (!mood || mood === 'default') {
            this.icon = 'none';
            this.text = lang.translate('userBook.mood.default');
            this.id = 'default';
        } else {
            this.icon = mood;
            this.text = lang.translate('userBook.mood.' + mood);
            this.id = mood;
        }
    }
    // To move in conf later
    static _moods = ['default', 'happy', 'proud', 'dreamy', 'love', 'tired', 'angry', 'worried', 'sick', 'joker', 'sad'];
    static availableMoods(): Mood[] {
        return _.map(Mood._moods, mood => new Mood(mood));
    }
}