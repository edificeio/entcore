import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Injector } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { SpinnerService, trim } from 'ngx-ode-ui';
import { catchError, flatMap, map, tap } from 'rxjs/operators';
import { NotifyService } from 'src/app/core/services/notify.service';
import { GroupModel } from '../../../core/store/models/group.model';
import { GroupsStore } from '../../groups.store';



@Component({
    selector: 'ode-list-group-create',
    templateUrl: './group-broadcast-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupBroadcastCreateComponent extends OdeComponent {
    newGroup: GroupModel = new GroupModel();

    constructor(private http: HttpClient,
                private groupsStore: GroupsStore,
                private ns: NotifyService,
                private spinner: SpinnerService,
                injector: Injector,
                private location: Location) {
                  super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();
    }
}
