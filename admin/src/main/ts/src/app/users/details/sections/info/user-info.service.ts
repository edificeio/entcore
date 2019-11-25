import {Injectable} from '@angular/core';
import {Observable, Subject} from 'rxjs';

@Injectable()
export class UserInfoService {
    private $userInfoState: Subject<any> = new Subject<any>();

    constructor() {
    }

    setState(state: any) {
        this.$userInfoState.next(state);
    }

    getState(): Observable<any> {
        return this.$userInfoState.asObservable();
    }
}
