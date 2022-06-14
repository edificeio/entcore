import { Location } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, Injector, Input } from '@angular/core';
import { OdeComponent } from 'ngx-ode-core';
import { SpinnerService, trim } from 'ngx-ode-ui';
import { catchError, flatMap, map, tap } from 'rxjs/operators';
import { NotifyService } from 'src/app/core/services/notify.service';
import { ClassModel } from 'src/app/core/store/models/structure.model';
import { GroupsStore } from '../../groups.store';
import { ClassCreatePayload, ClassesService } from '../classes.service';

@Component({
    selector: 'ode-class-create',
    templateUrl: './class-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ClassCreateComponent extends OdeComponent {
    @Input() label: string;

    newClass: ClassModel = {id:"", name:""};

    constructor(private http: HttpClient,
                private groupsStore: GroupsStore,
                private ns: NotifyService,
                private spinner: SpinnerService,
                private classesService: ClassesService,
                injector: Injector,
                private location: Location) {
      super(injector);
    }

    ngOnInit(): void {
        this.label = "create.class.name";
    }

    createNewClass() {
        const structureId = this.groupsStore.structure.id;
        const body:ClassCreatePayload = {
          name: this.newClass.name
        }

        this.spinner.perform('portal-content', 
          this.classesService.create(structureId, body, true)
          .pipe(
            tap( clazz => {
              this.ns.success({
                key: 'class.create.notify.content',
                parameters: {className: body.name}
              }, 'class.create.notify.title');
  
              this.router.navigate(['..', clazz.id, 'details'], {relativeTo: this.route, replaceUrl: false});
            }, (error: HttpErrorResponse) => {
              this.ns.error({
                  key: 'class.create.notify.error.content',
                  parameters: {className: body.name}
              }, 'class.create.notify.error.title');
            }),
            tap( async () => {
                // sync ProfileGroups
                await this.groupsStore.structure.syncGroups(true);
            })
          )
          .toPromise()
        );
    }

    cancel() {
        this.location.back();
    }

    onNameBlur(name: string): void {
        this.newClass.name = trim(name);
    }
}
