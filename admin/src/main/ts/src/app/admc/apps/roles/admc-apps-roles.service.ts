import { Injectable } from '@angular/core';
import http from 'axios';
import { NotifyService } from 'src/app/core/services/notify.service';
import { RoleActionModel } from 'src/app/core/store/models/role.model';

export type Role = {
    id: string;             // "c20bdfe5-2cf2-4e1d-9406-82f03c6978ac"
    name: string;           // "Exercices et évaluations - Exercizer - tous"
    distributions?:Array<string>;
    actions:Array<RoleActionModel>;  // ["fr.openent.exercizer.controllers.SubjectController|persist","exercizer.subject.persist","SECURED_ACTION_WORKFLOW"]
    isNew?:boolean;
}

type PFConfigDistribModel = {distributions:Array<string>};
type AppTemplate = {
    id: string;     // app id
    name: string;   // app name
    actions:Array<RoleActionModel>;
}

@Injectable()
export class AdmcAppsRolesService {
    constructor( private notify:NotifyService ) {
    }

// NOTE JCBE-ODE : Uncomment commented lines below if you want to "cache" the available roles.
// But beware, you will new to manually keep this cached data synchronized (new roles...)
//    private static roles: Array<Role>;

    private static PFConfig:PFConfigDistribModel;
    private static roleTemplates: Array<AppTemplate>;

    public getRoles(): Promise<Array<Role>> {
// NOTE JCBE-ODE : Uncomment commented lines below if you want to "cache" the available roles.
//        if (!AdmcAppsRolesService.roles || AdmcAppsRolesService.roles.length===0) {
            return new Promise((resolve, reject) => {
                http.get<Array<Role>>('/appregistry/roles/actions?structureId=0')
                .then(res => {
                    const /*AdmcAppsRolesService.*/roles = res.data.map( role => {
                        // Convert the strings array to an RoleActionModel
                        role.actions = role.actions.map( action => {
                            return {
                                name: action[0] as string,
                                displayName: action[1] as string,
                                type: action[2] as string
                            };
                        });

                        return role;
                    }) as Array<Role>;
                    resolve(/*AdmcAppsRolesService.*/roles);
                }, err => {
                    resolve([]);
                });
            });
        // NOTE JCBE-ODE : Uncomment commented lines below if you want to "cache" the available roles.
        // } else {
        //     return Promise.resolve( RolesService.roles );
        // }
    }

    public getDistributionTemplates():Promise<Array<string>> {
        if (!AdmcAppsRolesService.PFConfig) {
            return http
            .get<PFConfigDistribModel>('/admin/api/platform/config')
            .then(res => {
                if( res.status!==200 || !res || !res.data || !res.data.distributions ) 
                    throw 'Distributions not found';
                AdmcAppsRolesService.PFConfig = res.data;
                return AdmcAppsRolesService.PFConfig.distributions;
            })
            .catch( err => [] );
        } else {
            return Promise.resolve( AdmcAppsRolesService.PFConfig.distributions );
        }
    }

    public getApps():Promise<Array<AppTemplate>> {
        if (!AdmcAppsRolesService.roleTemplates) {
            return http
            .get<Array<Role>>('/appregistry/applications/actions?actionType=WORKFLOW&structureId=0')
            .then(res => {
                AdmcAppsRolesService.roleTemplates = res.data.map( app => {
                    // Convert the strings array to an RoleActionModel
                    app.actions = app.actions.map( action => {
                        return {
                            name: action[0] as string,
                            displayName: action[1] as string,
                            type: action[2] as string
                        };
                    });
                    return app;
                }) as Array<AppTemplate>;
                return AdmcAppsRolesService.roleTemplates;
            })
            .catch( err => [] );
        } else {
            return Promise.resolve( AdmcAppsRolesService.roleTemplates );
        }
    }

    public saveRole( role:Role ) {
        return (role.isNew 
            ? this.saveNewRole(role) 
            : http.put<{id:string}>(`/appregistry/role/${role.id}`, {
                role: role.name, 
                actions: role.actions.map( r=>r.name )
            })
        )
        .then( res => {
            if( res.status>=400 ) {
                throw res.statusText;
            }
            if( res.status===201 && role.isNew ) {
                role.id = res.data.id;
                delete role.isNew;
            }
            this.notify.info('services.application.roles.save.success');
            return true;
        })
        .catch( err => {
            this.notify.error('services.application.roles.save.error');
            return false;
        });
    }

    private saveNewRole( role:Role ) {
        return http.post<{id:string}>('/appregistry/role', {
            role: role.name, 
            actions: role.actions.map( r=>r.name )
        });
    }

    public removeRole( role:Role ) {
        return http
        .delete(`/appregistry/role/${role.id}`)
        .then( res => {
            if( res.status>=400 )
                throw res.statusText;
            this.notify.info('services.application.roles.delete.success');
            return true;
        })
        .catch( err => {
            this.notify.error('services.application.roles.delete.error');
            return false;
        });
    }

    public changeDistributions(role:Role, distributions) {
        return http
        .put(`/appregistry/role/${role.id}/distributions`, {
            distributions: distributions
        })
        .then( res => {
            if( res.status>=400 )
                throw res.statusText;
            this.notify.info('services.application.roles.distribution.update.success');
            return true;
        })
        .catch( err => {
            this.notify.error('services.application.roles.distribution.update.error');
            return false;
        });
    };
}
