import { NgModule, Provider } from '@angular/core'
import { BrowserModule } from '@angular/platform-browser'
import { RouterModule } from '@angular/router'
import { SijilModule } from "sijil";

import { CoreModule } from './core/core.module'
import { AppComponent } from './app.component'
import { AppHomeComponent } from './app-home.component'

import { routes } from './app-routing.module'

@NgModule({
    imports: [
        BrowserModule,
        RouterModule.forRoot(routes),
        CoreModule,
        SijilModule.forRoot()
    ],
    declarations: [
        AppComponent,
        AppHomeComponent
    ],
    bootstrap: [ AppComponent ]
})
export class AppModule {}
