import { HttpClient } from "@angular/common/http";
import { Component, Injector } from "@angular/core";
import { Data, Params } from "@angular/router";
import { OdeComponent } from "ngx-ode-core";
import { SpinnerService } from "ngx-ode-ui";
import { NotifyService } from "src/app/core/services/notify.service";
import { routing } from "src/app/core/services/routing.service";
import { GroupModel } from "src/app/core/store/models/group.model";
import { ServicesStore } from "../../services.store";
import { Assignment } from "../../_shared/services-types";

@Component({
    selector: 'ode-smart-widget',
    templateUrl: 'smart-widget-component.html'
})
export class SmartWidgetComponent extends OdeComponent {
    public currentTab: 'assignment' | 'massAssignment' = 'assignment';
    public assignmentGroupPickerList: Array<GroupModel>;

    constructor(
        injector: Injector,
        public servicesStore: ServicesStore,
        private httpClient: HttpClient,
        private notifyService: NotifyService,
        private spinnerService: SpinnerService) {
        super(injector);
    }

    ngOnInit() {
        super.ngOnInit();
        this.subscriptions.add(this.route.params.subscribe((params: Params) => {
            if (params['widgetId']) {
                this.servicesStore.widget = this.servicesStore.structure.widgets.data.find(a => a.id === params['widgetId']);
            }
        }));

        this.subscriptions.add(routing.observe(this.route, 'data').subscribe((data: Data) => {
            if (data.structure) {
                this.assignmentGroupPickerList = this.servicesStore.structure.groups.data;
            }
        }));
    }

    public async onAddAssignment(assignment: Assignment): Promise<void> {
        this.spinnerService.perform(
            'portal-content', 
            this.httpClient.post(`/appregistry/widget/${this.servicesStore.widget.id}/link/${assignment.group.id}`, {}).
                toPromise().
                then(() => {
                    assignment.role.groups.push(assignment.group);
                    this.notifyService.success(
                        {
                            key: 'notify.services.assignment.role.added.success.content', 
                            parameters: {roleName: assignment.role.name, groupName: assignment.group.name}
                        }, 
                        'notify.services.assignment.role.added.success.title'
                    );
                }).
                catch(err => {
                    this.notifyService.error(
                        {
                            key: 'notify.services.assignment.role.added.error.content',
                            parameters: {roleName: assignment.role.name, groupName: assignment.group.name}
                        }, 
                        'notify.services.assignment.role.added.error.title',
                        err
                    );
                }));
    }

    public async onRemoveAssignment(assignment: Assignment): Promise<void> {
        this.spinnerService.perform(
            'portal-content', 
            this.httpClient.delete(`/appregistry/widget/${this.servicesStore.widget.id}/link/${assignment.group.id}`).
                toPromise().
                then(() => {
                    const groupIndex = assignment.role.groups.findIndex(g => g.id == assignment.group.id);
                    assignment.role.groups.splice(groupIndex, 1);
                    this.notifyService.success(
                        {
                            key: 'notify.services.assignment.role.deleted.success.content', 
                            parameters: {roleName: assignment.role.name, groupName: assignment.group.name}
                        }, 
                        'notify.services.assignment.role.deleted.success.title'
                    );
                }).
                catch(err => {
                    this.notifyService.error(
                        {
                            key: 'notify.services.assignment.role.deleted.error.content',
                            parameters: {roleName: assignment.role.name, groupName: assignment.group.name}
                        }, 
                        'notify.services.assignment.role.deleted.error.title',
                        err
                    );
                }));
    }
}