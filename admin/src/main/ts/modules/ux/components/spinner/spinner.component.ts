import { Component, Input, ChangeDetectionStrategy, ChangeDetectorRef,
    OnInit, OnDestroy } from '@angular/core'
import { LoadingService } from '../../../../services/loading.service'
import { Subscription } from 'rxjs/Subscription'

@Component({
    selector: 'spinner-cube',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
    <div class="spinner-wrapper" *ngIf="ls.isLoading(loadingProp)">
        <div class="spinner-cube">
            <div class="sk-cube sk-cube1"></div>
            <div class="sk-cube sk-cube2"></div>
            <div class="sk-cube sk-cube3"></div>
            <div class="sk-cube sk-cube4"></div>
            <div class="sk-cube sk-cube5"></div>
            <div class="sk-cube sk-cube6"></div>
            <div class="sk-cube sk-cube7"></div>
            <div class="sk-cube sk-cube8"></div>
            <div class="sk-cube sk-cube9"></div>
        </div>
    </div>
    `,
    styles: [`
        div.spinner-wrapper{
            position: fixed;
            width: 100%;
            height: 100%;
            pointer-events: all;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        div.spinner-cube {
            width: 20vh;
            height: 20vh;
        }

        div.sk-cube {
            width: 33%;
            height: 33%;
            float: left;
            -webkit-animation: sk-cubeGridScaleDelay 1.3s infinite ease-in-out;
            animation: sk-cubeGridScaleDelay 1.3s infinite ease-in-out;
        }

        div.sk-cube1 {
            -webkit-animation-delay: 0.2s;
            animation-delay: 0.2s;
        }
        div.sk-cube2 {
            -webkit-animation-delay: 0.3s;
            animation-delay: 0.3s;
        }
        div.sk-cube3 {
            -webkit-animation-delay: 0.4s;
            animation-delay: 0.4s;
        }
        div.sk-cube4 {
            -webkit-animation-delay: 0.1s;
            animation-delay: 0.1s;
        }
        div.sk-cube5 {
            -webkit-animation-delay: 0.2s;
            animation-delay: 0.2s;
        }
        div.sk-cube6 {
            -webkit-animation-delay: 0.3s;
            animation-delay: 0.3s;
        }
        div.sk-cube7 {
            -webkit-animation-delay: 0s;
            animation-delay: 0s;
        }
        div.sk-cube8 {
            -webkit-animation-delay: 0.1s;
            animation-delay: 0.1s;
        }
        div.sk-cube9 {
            -webkit-animation-delay: 0.2s;
            animation-delay: 0.2s;
        }

        @-webkit-keyframes sk-cubeGridScaleDelay {
            0%, 70%, 100% {
                -webkit-transform: scale3D(1, 1, 1);
                        transform: scale3D(1, 1, 1);
            } 35% {
                -webkit-transform: scale3D(0, 0, 1);
                        transform: scale3D(0, 0, 1);
            }
        }

        @keyframes sk-cubeGridScaleDelay {
            0%, 70%, 100% {
                -webkit-transform: scale3D(1, 1, 1);
                        transform: scale3D(1, 1, 1);
            } 35% {
                -webkit-transform: scale3D(0, 0, 1);
                        transform: scale3D(0, 0, 1);
            }
        }
    `]
})
export class SpinnerComponent implements OnInit, OnDestroy {

    @Input("waitingFor")
    set loadingProp(prop: string) {
        this._loadingProp = prop
    }
    get loadingProp() { return this._loadingProp }
    private _loadingProp : string

    private subscription : Subscription

    constructor(
        public ls: LoadingService,
        private cdRef: ChangeDetectorRef) {}

    ngOnInit() {
        this.subscription = this.ls.trigger.subscribe(() => {
            this.cdRef.markForCheck()
        })
    }

    ngOnDestroy() {
        if(this.subscription)
            this.subscription.unsubscribe()
    }

}