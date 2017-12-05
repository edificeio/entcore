import { 
    Component, Input, Output, Renderer, 
    ContentChildren, AfterContentInit, QueryList, 
    ElementRef, EventEmitter,
    OnDestroy } from '@angular/core'
import { LabelsService } from '../services'


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
            <nav class="steps-nav-button">
                <button class="cancel" 
                    (click)="cancel.emit()"
                    [title]="labels('cancel')">
                    {{ labels('cancel') }}
                </button>
                <button class="previous" 
                    (click)="onPreviousStep()" 
                    *ngIf="activeStep > 0" 
                    [title]="labels('previous')">
                    {{ labels('previous') }}
                </button>
                <button class="next" 
                    (click)="onNextStep()" 
                    *ngIf="activeStep < steps.length - 1" ng-disabled="!canDoNext()"
                    [title]="labels('next')">
                    {{ labels('next') }}
                </button>
                <button class="finish" 
                    *ngIf="activeStep === steps.length - 1" 
                    (click)="finish.emit()" ng-disabled="!canDoFinish()"
                    [title]="labels('finish')">
                    {{ labels('finish') }}
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
            private labelsService: LabelsService,
            private renderer : Renderer,
            private ref : ElementRef)
    {}
    
    @Output("cancel") cancel: EventEmitter<{}> = new EventEmitter();
    @Output("finish") finish: EventEmitter<{}> = new EventEmitter();
    @Output("previousStep") previousStep : EventEmitter<Number> = new EventEmitter();
    @Output("nextStep") nextStep : EventEmitter<Number> = new EventEmitter();

    doCancel() {
        this.activeStep = 0;
        this.steps.forEach((step, index) => {
            index == 0 ? step.isActived = true : step.isActived = false; 
        })
    }

    doFinish() {
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

    onNextStep() {
        this.nextStep.emit(this.activeStep);
    }
    doNextStep() {
        if (this.activeStep < this.steps.length -1) {
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

    labels(label){
        return this.labelsService.getLabel(label)
    }

    canDoNext():boolean {
        return true;
    }

    canDoFinish():boolean {
        return true;
    }

}
