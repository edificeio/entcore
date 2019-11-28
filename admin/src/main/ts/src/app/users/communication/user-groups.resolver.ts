import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {GroupModel} from '../../core/store/models/group.model';
import {Observable} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {map} from 'rxjs/operators';

@Injectable()
export class UserGroupsResolver implements Resolve<GroupModel[]> {

    constructor(private http: HttpClient) {
    }

    resolve(route: ActivatedRouteSnapshot): Observable<GroupModel[]> {
        const userId = route.paramMap.get('userId');
        return this.http.get<GroupModel[]>(`/directory/user/${userId}/groups`)
            .pipe(
                map(groups => groups.filter(group => unwantedGroupTypes.indexOf(group.type) < 0))
            );
    }
}

const unwantedGroupTypes = ['CommunityGroup'];
