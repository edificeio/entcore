import { Injectable } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';

@Injectable()
export class UserInfoService {
    private userInfoState: Subject<any> = new Subject<any>();

    constructor() {
    }

    setState(state: any) {
        this.userInfoState.next(state);
    }

    getState(): Observable<any> {
        return this.userInfoState.asObservable();
    }
}