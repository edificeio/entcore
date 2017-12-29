import { Component, AfterViewInit, ElementRef } from '@angular/core'

import http from 'axios'
@Component({
    selector: 'admin-app',
    template: '<router-outlet></router-outlet>'
})
export class AppComponent implements AfterViewInit {
    constructor(private elementRef:ElementRef) {}

    ngAfterViewInit() {
        this.appendHotjarScript();
    }

    private appendHotjarScript() {
        http.get('admin/conf/public').then(res => {
            if (res.data.hotjarId) {
                var s = document.createElement("script");
                s.type = "text/javascript";
                s.innerHTML = `
                    (function(h,o,t,j,a,r){
                        h.hj=h.hj||function(){(h.hj.q=h.hj.q||[]).push(arguments)};
                        h._hjSettings={hjid:${res.data.hotjarId},hjsv:6};
                        a=o.getElementsByTagName('head')[0];
                        r=o.createElement('script');r.async=1;
                        r.src=t+h._hjSettings.hjid+j+h._hjSettings.hjsv;
                        a.appendChild(r);
                    })(window,document,'https://static.hotjar.com/c/hotjar-','.js?sv=');
                `;
                this.elementRef.nativeElement.appendChild(s);
            } else {
                console.info('hotjar not configured on this plateform');
            }
        });
    }
}