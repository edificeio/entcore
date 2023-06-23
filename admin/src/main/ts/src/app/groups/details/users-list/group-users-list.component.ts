import { ChangeDetectionStrategy, Component, Injector, Input, OnInit } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { UserListService } from '../../../core/services/userlist.service';
import { UserModel } from '../../../core/store/models/user.model';
import { Session } from "src/app/core/store/mappings/session";
import { SessionModel } from "src/app/core/store/models/session.model";


@Component({
    selector: 'ode-group-users-list',
    templateUrl: './group-users-list.component.html',
    styleUrls: ['./group-users-list.component.scss'],
    providers: [UserListService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupUsersListComponent extends OdeComponent implements OnInit  {
    constructor(injector: Injector, public userLS: UserListService) {
        super(injector);
    }

    @Input()
    users: UserModel[];

    isADML:boolean = false;
    isADMC:boolean = false;
    scope:Array<string> = [];

    ngOnInit(): void {
        this.initContext();
    }

    private initContext = async () => {
        const session: Session = await SessionModel.getSession();

        this.isADMC = session.isADMC();

        if (session.functions && session.functions["ADMIN_LOCAL"]) {
            const { code, scope } = session.functions["ADMIN_LOCAL"];
            this.isADML = code === "ADMIN_LOCAL";
            if( scope?.length > 0 ) {
                this.scope = scope;
            }
        }
        this.changeDetector.markForCheck();
    }

    areScopeDisjoint(ids:Array<string>|null) {
        if( this.isADMC ) return false;
        if( !ids ) return true;
        for( const id in ids ) {
            if( this.scope.indexOf(id)>=0 ) return false;
        }
        return true;
    }

    selectUser(user: UserModel) {
        if (user.structures.length > 0) {
            this.router.navigate(['admin', user.structures[0].id, 'users', user.id, 'details']);
        }
    }
}
