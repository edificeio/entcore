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

    createUser() {
        let payload = new window['URLSearchParams']();

        payload.append('firstName', this.newUser.firstName)
        payload.append('lastName', this.newUser.lastName)
        payload.append('type', this.newUser.type)
        if (this.newUser.classes && this.newUser.classes.length > 0) {
            payload.append('classId', this.newUser.classes[0].id)
        }
        payload.append('structureId', this.usersStore.structure.id)
        payload.append('birthDate', this.newUser.userDetails.birthDate)
        this.newUser.userDetails.children.forEach(child => payload.append('childrenIds', child.id))

        this.ls.perform('portal-content', this.newUser.create(payload, {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            }
        })).then(res => {
            this.newUser.id = res.data.id
            this.ns.success(
                {
                    key: 'notify.user.create.content',
                    parameters: {
                        user: this.newUser.firstName + ' ' + this.newUser.lastName}
                    }
                , 'notify.user.create.title')

            this.usersStore.structure.users.data.push(this.newUser)

            this.router.navigate(['..', res.data.id], {relativeTo: this.route, replaceUrl: false})
        }).catch(err => {
            this.ns.error(
                {
                    key: 'notify.user.create.error.content',
                    parameters: {
                        user: this.newUser.firstName + ' ' + this.newUser.lastName}
                    }
                , 'notify.user.create.error.title', err)
        });
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
