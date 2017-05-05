import { Injectable } from '@angular/core'
import { Resolve } from '@angular/router'

import { SessionModel } from '../models'
import { Session } from '../models/mappings'

@Injectable()
export class SessionResolve implements Resolve<Session> {

    constructor(){}

    resolve(): Promise<Session> {
        return SessionModel.getSession()
    }
}