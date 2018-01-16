import { Injectable } from '@angular/core'
import { Resolve } from '@angular/router'

import { Session, SessionModel } from '../store'

@Injectable()
export class SessionResolver implements Resolve<Session> {

    constructor(){}

    resolve(): Promise<Session> {
        return SessionModel.getSession()
    }
}