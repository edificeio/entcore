import { 
    Component, Input, Output, Renderer, 
    ContentChildren, AfterContentInit, QueryList, 
    ElementRef, EventEmitter,
    OnDestroy } from '@angular/core'

@Component({
    selector : 'step',
    template: `<ng-content></ng-content>`,
    styles: [`
        :host {
            display: none;
            overflow: auto;
            padding : 1em;
        }
        :host.active {
            display: block;
        }
    `]
})
export class StepComponent {
    @Input() name:string; 
    @Input() isActived:boolean; 
    hasError:boolean = false;
    isFinished:boolean = false;
}

@Component({
    selector: 'wizard',
    template: `
        <nav class="steps-progress-menu">
            <ul>
                <li *ngFor="let step of steps" 
                    [class.active]="step.isActived"
                    [class.finish]="step.isFinished">
                    {{step.name}}
                </li>
            </ul>
        </nav>
        <section class="steps-content">
            <ng-content select="step"></ng-content>
            <nav class="steps-nav-button" *ngIf="activeStep < steps.length - 1">
                <button class="cancel" 
                    (click)="cancel.emit()"
                    [title]="'cancel' | translate">
                    {{ 'cancel' | translate }}
                </button>
                <button class="previous" 
                    (click)="onPreviousStep()" 
                    *ngIf="activeStep > 0" 
                    [title]="'previous' | translate">
                    {{ 'previous' | translate }}
                </button>
                <button class="next" 
                    (click)="onNextStep()" 
                    [disabled]="!canDoNext"
                    [title]="'next' | translate">
                    {{ 'next' | translate }}
                </button>
            </nav>
        </section>
    `,
    styles: [`
        :host {
            display: block;
            overflow: auto;
            background-color: #ccc;
        }
        section.steps-content {
            float: right;
            width: 75%;
            background: #fff;
        }
        nav.steps-progress-menu {
            float: left;
            width: 24%;
            background-color: transparent;
        }
        nav.steps-progress-menu ul li {
            list-style-type: none;
            padding: 7px;
        }
        nav.steps-progress-menu ul li.active,
        nav.steps-progress-menu ul li.finish {
            font-weight: bold;
        }
        nav.steps-progress-menu ul li.finish {
            color: green;
        }
        nav.steps-nav-button {
            text-align : right;
        }
    `]
})
export class WizardComponent implements AfterContentInit, OnDestroy {

    constructor(
            private renderer : Renderer,
            private ref : ElementRef)
    {}
    
    @Output("cancel") cancel: EventEmitter<{}> = new EventEmitter();
    @Output("previousStep") previousStep : EventEmitter<Number> = new EventEmitter();
    @Output("nextStep") nextStep : EventEmitter<Number> = new EventEmitter();

    doCancel() {
        this.activeStep = 0;
        this.steps.forEach((step, index) => {
            index == 0 ? step.isActived = true : step.isActived = false; 
        })
    }

    onPreviousStep() {
        this.previousStep.emit(this.activeStep);
    }

    doPreviousStep() {
        if (this.activeStep > 0) {
            this.steps.toArray()[this.activeStep].isActived = false;
            this.steps.toArray()[this.activeStep -1].isActived = true;
            this.activeStep--;
        }
    }

    canDoNext:boolean = true;

    onNextStep() {
        this.nextStep.emit(this.activeStep);
    }
    doNextStep() {
        if (this.activeStep < this.steps.length -1) {
            this.canDoNext = true;
            this.steps.toArray()[this.activeStep].isActived = false;
            this.steps.toArray()[this.activeStep + 1].isActived = true;
            this.activeStep++;
        }
    }


    @ContentChildren(StepComponent) steps: QueryList<StepComponent>;
    activeStep:number = 0;

    ngAfterContentInit() {
        if (this.steps.length == 0) 
            throw new Error("<wizard> component musts nest at least 1 <step> compoent")
    }

    ngOnDestroy() : void {
    }
}
