import { Injectable } from '@angular/core'
import { Subject } from 'rxjs/Subject';

@Injectable()
export class UserInfoService {
    private userInfoState: Subject<any> = new Subject<any>();

    constructor() {}

    setState(state: any) {
        this.userInfoState.next(state);
    }

    getState() {
        return this.userInfoState.asObservable();
    }
}