import {AppRoutingModule} from './app-routing.module';
import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {SijilModule} from 'sijil';

import {CoreModule} from './core/core.module';
import {AppComponent} from './app.component';
import {AppHomeComponent} from './app-home.component';
import {HttpClientModule} from '@angular/common/http';

import { registerLocaleData } from '@angular/common';
import localeDe from '@angular/common/locales/de';
import localeEs from '@angular/common/locales/es';
import localeFr from '@angular/common/locales/fr';
import localeIt from '@angular/common/locales/it';
import localePt from '@angular/common/locales/pt';
registerLocaleData(localeDe);
registerLocaleData(localeEs);
registerLocaleData(localeFr);
registerLocaleData(localeIt);
registerLocaleData(localePt);

@NgModule({
    imports: [
        BrowserModule,
        AppRoutingModule,
        CoreModule,
        HttpClientModule,
        SijilModule.forRoot()
    ],
    declarations: [
        AppComponent,
        AppHomeComponent
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
