import {Injectable} from '@angular/core';
import {Resolve} from '@angular/router';
import { Session } from '../store/mappings/session';
import { SessionModel } from '../store/models/session.model';

@Injectable()
export class SessionResolver implements Resolve<Session> {

    constructor() {}

    resolve(): Promise<Session> {
        return SessionModel.getSession();
    }
}
