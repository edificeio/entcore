interface Action {
    name: string;
    displayName: string;
    type: string;
}

interface Application {
    name: string;
    address: string;
    icon: string;
    target: string;
    displayName: string;
    display: boolean;
    prefix: string;
}

interface Widget {
    id: string;
    name: string;
    path: string;
    js: string;
    i18n: string;
    application: string;
    mandatory: boolean;
}

interface Function {
    code: string;
    functioName?: string;
    scope: string[];
    structureExternalIds?: string[];
    subjects?: Map<string, Subject>;
}

interface Subject {
    subjectCode: string;
    subjectName: string;
    scope: string[];
    structureExternalIds: string[];
}

export class Session {

    login: string;
    federated: boolean;

    userId: string;
	externalId: string;

	firstName: string;
	lastName: string;
	username: string;

	birthDate: string;

    classes: string[];
	classNames: string[];
    structures: string[];
	structureNames: string[];
	uai: string[];

    level: string;
	type: string;

	childrenIds: string[];
	groupsIds: string;

	authorizedActions: Action[];
	apps: Application[];
	widgets: Widget[];
    functions: {[key: string]: Function};

    isADMC(): boolean
    {
        return this.functions && this.functions.SUPER_ADMIN != null;
    }

    isADML(): boolean
    {
        return this.functions && this.functions.ADMIN_LOCAL != null;
    }

    hasRight(right: string): boolean
    {
        if(this.authorizedActions != null)
            for(let i = this.authorizedActions.length; i-- > 0;)
                if(this.authorizedActions[i].name == right)
                    return true;
        return false;
    }

}
