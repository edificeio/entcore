import { Component } from '@angular/core'

@Component({
    selector: 'portal',
    template: `
        <header>
            <ng-content select="[header-left]"></ng-content>
            <ng-content select="[header-middle]"></ng-content>
            <ng-content select="[header-right]"></ng-content>
        </header>
        <section>
            <ng-content select="[section]"></ng-content>
        </section>
        <footer>
            <ng-content select="[footer]"></ng-content>
        </footer>
    `,
    styles: [`
        header{
            position:relative;
            display: flex;
            align-items: center;
            top: 0px;
            width: 100%;
        }

        header >>> > div {
            flex-grow: 1;
            flex-basis: 33.3%;
        }
        header >>> > div {
            display: flex;
            align-items: center;
        }
        header >>> > div[header-middle] {
            justify-content: center;
        }
        header >>> > div[header-right] {
            flex-direction: row-reverse;
        }
        header >>> > div > *{
            display: inline-block;
            vertical-align: middle;
        }
    `]
})
export class PortalComponent {}
