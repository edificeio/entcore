import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core'
import { ActivatedRoute, Router, Data } from '@angular/router'
import { routing } from '../../core/services/routing.service'

@Component({
    selector: 'message-flash',
    template: `
        <router-outlet></router-outlet>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageFlashComponent implements OnInit{

    constructor(
        public route: ActivatedRoute,
        public router: Router) {}

    ngOnInit(){
        routing.observe(this.route, "data").subscribe(async (data: Data) => {
            if (data['structure']) {
                let structure = data['structure'];
                if (this.router.isActive('/admin/' + structure.id + '/management/message-flash', true)) {
                    this.router.navigate(['list'], {relativeTo: this.route});
                }
            }
        });
    }

}
