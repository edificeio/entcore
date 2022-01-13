import { Component, Injector, OnInit } from '@angular/core';
import { Data } from '@angular/router';
import { OdeComponent } from 'ngx-ode-core';
import { AdmcAppsRolesService, Role } from '../admc-apps-roles.service';
import { routing } from 'src/app/core/services/routing.service';
import { ApplicationModel } from 'src/app/core/store/models/application.model';
import { RoleActionModel, RoleModel } from 'src/app/core/store/models/role.model';

const NEW_ROLE = "_NEW_ROLE";

@Component({
    selector: 'ode-application-roles',
    templateUrl: './application-roles.component.html',
    styleUrls: ['./application-roles.component.scss'],
})
export class ApplicationRolesComponent extends OdeComponent implements OnInit {
    public application: ApplicationModel;
    public roles:{[name:string]:Role};
    public actions: Array<RoleActionModel>;
    public distributions: Array<string>;

    constructor(
            injector: Injector,
            private roleSvc:AdmcAppsRolesService) {
        super(injector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        // Get the apps from the route resolver.
        this.application = this.route.snapshot.data.app;
        // Get all existing roles, and index them by their name.
        this.roles = {};
        this.route.snapshot.data.roles.forEach( (role:Role) => {
            this.roles[role.id] = role;
        });
        // Get all existing actions for this app.
        this.actions = this.route.snapshot.data.actions || [];
        // Get all existing distributions
        this.distributions = this.route.snapshot.data.distributions || [];

        this.subscriptions.add( routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.app) {
                this.application = data.app;
            }
            if (data.actions) {
                this.actions = data.actions;
            }
            // distributions never change
        }));
    }

    newRole() {
        const newRole:Role = {
            id: NEW_ROLE,
            name: "Nom du nouveau rôle",
            distributions: [].concat(this.distributions),
            actions:[],
            isNew: true
        };

        this.application.roles.unshift( newRole as unknown as RoleModel);
        this.roles[NEW_ROLE] = newRole;
    }

    public actionsOfRole(roleModel:RoleModel):Array<RoleActionModel> {
        return this.roles[roleModel.id].actions || [];
    }

    public distributionsOfRole(roleModel:RoleModel):Array<string> {
        return roleModel.distributions || [];
    }

    /**
     * Add/remove an action from a role in the application managed by this component.
     * But does not save the change.
     * 
     * @param roleModel model of the impacted role 
     * @param action model of the action to to add/remove
     * @param active truthy when action must be added, falsy otherwise
     * @returns void
     */
    public toggleAction(roleModel:RoleModel, action:RoleActionModel, active:boolean) {
        if( !roleModel || !action ) return;
        const role:Role = this.roles[roleModel.id];
        if( ! role) return;
        const index = role.actions.findIndex( ram => ram.name===action.name );
        if( active ) {
            // Add this action to the role, if not already present.
            if( index===-1 ) {
                role.actions.push( action );
            }
        } else {
            // Remove this action from the role.
            if( 0 <= index && index < role.actions.length ) {
                role.actions.splice( index, 1 );
            }
        }
    }

    public async save( roleModel:RoleModel, withDistributions:boolean ) {
        if( !roleModel ) return;
        const role:Role = this.roles[roleModel.id];
        // Name may have changed
        role.name = roleModel.name;
        
        // Actions may have changed, in-place => no need to update them here ( @see actionOfRole() method )
        // void

        const isNew = role.isNew || false;

        // Finally, save the updated role.
        const saved = await this.roleSvc.saveRole( role );
        if( saved ) {
            if( withDistributions ) {
                // Distributions may have changed.
                await this.roleSvc.changeDistributions( role, this.distributionsOfRole(roleModel) );
            }

            if( isNew ) {
                // Update internal data model
                this.roles[role.id] = this.roles[NEW_ROLE];
                delete this.roles[NEW_ROLE];
            }
        }
    }

    public delete( roleModel:RoleModel ) {
        if( !roleModel ) return;
        const role = this.roles[roleModel.id];
        return this.roleSvc.removeRole( role )
        .then( success => {
            const removeAtIdx = this.application.roles.findIndex( r=>r.id===roleModel.id );
            if( success && removeAtIdx>0 ) {
                // cleanup data model
                delete this.roles[roleModel.id];
                this.application.roles.splice( removeAtIdx, 1 );
            }
        } );
    }
}
