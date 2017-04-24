import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core'
import { ActivatedRoute, Data, Router } from '@angular/router'

import { Subscription } from 'rxjs/Subscription'

import { routing } from '../../../../routing/routing.utils'

import { StructureModel } from '../../../../store'
import { UserModel } from '../../../../store'
import { UsersStore } from '../../store'

import { LoadingService, NotifyService, UserListService } from '../../../../services'

@Component({
    selector: 'user-create',
    templateUrl: './user-create.component.html',
    providers: [ UserListService ]
})
export class UserCreate implements OnInit, OnDestroy {

    private newUser: UserModel = new UserModel()
    private noClasses : Array<any> = []
    private structureSubscriber : Subscription
    private searchChildrenTerm: string
    
    constructor(
        public usersStore: UsersStore,
        private ns: NotifyService,
        private ls: LoadingService,
        private router: Router,
        private route: ActivatedRoute,
        private userListService: UserListService) {}

    ngOnInit(): void {
        this.newUser.classes = this.noClasses
        this.newUser.type = 'Personnel'
        let {id, name} = this.usersStore.structure
        this.newUser.structures = [{id: id, name:name}]

        this.structureSubscriber = routing.observe(this.route, "data").subscribe((data: Data) => {
            if(data['structure']) {
                this.newUser.structures = [data['structure']]
            }
        })
        this.newUser.userDetails.children = []
    }

    ngOnDestroy(): void {
        this.structureSubscriber.unsubscribe()
    }

    private createNewUser() {
        this.ls.perform('portal-content', this.newUser.createNewUser(this.usersStore.structure.id)
            .then(res => {
                this.newUser.id = res.data.id
                this.ns.success({
                        key: 'notify.user.create.content',
                        parameters: {
                            user: this.newUser.firstName + ' ' + this.newUser.lastName}
                        }
                    , 'notify.user.create.title')

                this.usersStore.structure.users.data.push(this.newUser)

                this.router.navigate(['..', res.data.id], {relativeTo: this.route, replaceUrl: false})
            }).catch(err => {
                this.ns.error({
                        key: 'notify.user.create.error.content',
                        parameters: {
                            user: this.newUser.firstName + ' ' + this.newUser.lastName}
                        }
                    , 'notify.user.create.error.title', err)
            })
        )
    }

    addChild(child) {
        if (this.newUser.userDetails.children.indexOf(child) < 0) { 
            this.newUser.userDetails.children.push(child)
        }
    }

    removeChild(child) {
        const index = this.newUser.userDetails.children.indexOf(child);
        this.newUser.userDetails.children.splice(index, 1)
    }
}
