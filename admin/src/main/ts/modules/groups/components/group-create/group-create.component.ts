import { Component, ChangeDetectionStrategy } from '@angular/core'
import { ActivatedRoute, Router } from '@angular/router'
import { Location } from '@angular/common'
import { GroupModel } from '../../../../store/models'
import { GroupsStore } from '../../store'
import { LoadingService, NotifyService } from '../../../../services'

@Component({
    selector: 'group-create',
    template: `
        <div class="panel-header">
            <span><s5l>new.group.creation</s5l></span>
        </div>

        <panel-section class="thin">
            <form #createForm="ngForm" (ngSubmit)="createNewGroup()">
                <form-field label="create.group.name">
                    <input type="text" [(ngModel)]="newGroup.name" name="name"
                        required pattern=".*\\S+.*" #nameInput="ngModel"
                        (blur)="newGroup.name = trim(newGroup.name)">
                    <form-errors [control]="nameInput"></form-errors>
                </form-field>

                <div class="action">
                    <button type="button" class="cancel" (click)="cancel()">
                        <s5l>create.group.cancel</s5l>
                    </button>
                    <button class="create" 
                        [disabled]="createForm.pristine || createForm.invalid">
                        <s5l>create.group.submit</s5l>
                    </button>
                </div>
            </form>
        </panel-section>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class GroupCreate {

    private newGroup: GroupModel = new GroupModel()

    constructor(private groupsStore: GroupsStore,
        private ns: NotifyService,
        private ls: LoadingService,
        private router: Router,
        private route: ActivatedRoute,
        private location: Location) {}

    createNewGroup() {
        this.newGroup.structureId = this.groupsStore.structure.id

        this.ls.perform('portal-content', this.newGroup.create()
            .then(res => {
                this.newGroup.id = res.data.id
                this.newGroup.type = 'ManualGroup'
                this.groupsStore.structure.groups.data.push(this.newGroup)

                this.ns.success({
                        key: 'notify.group.create.content',
                        parameters: { group: this.newGroup.name }
                    } , 'notify.group.create.title')

                this.router.navigate(['..', res.data.id], 
                    {relativeTo: this.route, replaceUrl: false})
            }).catch(err => {
                this.ns.error({
                        key: 'notify.group.create.error.content',
                        parameters: { group: this.newGroup.name }
                    }, 'notify.group.create.error.title', err)
            })
        )
    }

    cancel() {
        this.location.back();
    }

    trim(input:string) {
        if (input && input.length > 0) {
            return input.trim()
        }
        return input
    }
}