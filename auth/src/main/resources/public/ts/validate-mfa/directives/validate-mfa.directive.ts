import angular = require("angular");
import { IAttributes, IController, IDirective, IScope } from "angular";
import { L10n, conf, http, session, notify, notif } from "ode-ngjs-front";
import { IMobileValidationInfos, IPromisified } from "ode-ts-client";

type OTPStatus = ""|"wait"|"ok"|"ko";

/* Controller for the directive */
export class ValidateMfaController implements IController {
    private me = session().user;
	public lang = conf().Platform.idiom;

	// Scoped data
	public force?:Boolean;
	public redirect?:string;

	// Input data
	public mobilePhone?:String;
	public inputCode?:String;
	public status:OTPStatus = "";
	public koStatusCause = "";
	// Server data
	private infos?: IMobileValidationInfos;

	public async initialize() {
		this.infos = await Promise.all([
			notif().onSessionReady().promise,
			conf().Platform.idiom.addBundlePromise("/auth/i18n")
		])
		.then( unused => (session().getMobileValidationInfos()) as Promise<IMobileValidationInfos>)
		.catch( e => {
			setTimeout( () => notify.error('validate-mfa.error.network', 4000), 500 );
			return null;
		});
	}

	public validateSmsMfa() {
		// Wait at least infos.waitInSeconds (defaults to 10) seconds while validating
		const time = new Date().getTime();

		return session().checkMobile(this.mobilePhone)
		.then( () => {
			this.inputCode && delete this.inputCode;
		})
		.catch( e => {
			notify.error('validate-mfa.error.network');
		})
		.then( () => {
			const waitMs = (this.infos ? this.infos.waitInSeconds:10) * 1000;
			const duration = Math.min( Math.max(waitMs-new Date().getTime()+time, 0), waitMs);
			const debounceTime:IPromisified<void> = notif().promisify();
			setTimeout( () => debounceTime.resolve(), duration);
			return debounceTime.promise;
		})
		;
	}

	public validateCode():Promise<OTPStatus> {
		// Wait at least 0,5s while validating
		const time = new Date().getTime();

		return (session().tryMobileValidation(this.inputCode))
		.then( validation => {
			if( validation.state === "valid" ) {
				this.status = "ok";
			} else {
				this.status = "ko";
				if (validation.state === "outdated") {
					this.koStatusCause = 'validate-mfa.error.ttl';
				} else {
					this.koStatusCause = 'validate-mfa.error.code';
				}
			}
		})
		.catch( e => {
			notify.error('validate-mfa.error.network');
		})
		.then( () => {
			const waitMs = 500;
			const duration = Math.min( Math.max(waitMs-new Date().getTime()+time, 0), waitMs);
			const debounceTime:IPromisified<void> = notif().promisify();
			setTimeout( () => debounceTime.resolve(), duration);
			return debounceTime.promise;
		})
		.then( () => this.status );
	}

	public renewCode():Promise<void> {
		return (session().checkMobile(this.mobilePhone))
		.then( () => (session().getMobileValidationInfos()) as Promise<IMobileValidationInfos>)
		.then( infos => {
			notify.success('validate-mfa.step2.renewed');
			this.infos = infos;
			this.inputCode = "";
			this.status = "";
			this.koStatusCause = "";
		});
	}
};

interface ValidateMfaScope extends IScope {
	canRenderUi: boolean;
	type: string;
	onValidate: () => Promise<void>;
	onCodeChange: (form:angular.IFormController) => Promise<void>;
	onCodeRenew: () => Promise<void>;
}

/* Directive */
class Directive implements IDirective<ValidateMfaScope,JQLite,IAttributes,IController[]> {
    restrict = 'E';
	template = require("./validate-mfa.directive.html");
    scope = {
		force: "=?",
		redirect: "=?"
    };
	bindToController = true;
	controller = [ValidateMfaController];
	controllerAs = 'ctrl';
	require = ['validateMfa'];

	private setAttr(el:string|HTMLElement, attr:"disabled"|"readonly", enabled:boolean = true) {
		if( typeof el==="string" )
			el = document.getElementById(el);
		if( el )
			angular.element(el).prop(attr, enabled ? attr : "");
	}

    link(scope:ValidateMfaScope, elem:JQLite, attr:IAttributes, controllers:IController[]|undefined) {
        const ctrl:ValidateMfaController|null = controllers ? controllers[0] as ValidateMfaController : null;
        if(!ctrl) return;

		scope.canRenderUi = false;

		scope.onValidate = async (): Promise<void> => {
			ctrl.status = "wait";
			await ctrl.validateSmsMfa();
			ctrl.status = "";
			scope.$apply();
			setTimeout( ()=>document.getElementById("input-data").focus(), 10 );
		}

		scope.onCodeChange = async (form) => {
			try {
				if( form.$invalid ) {
					ctrl.status = "";
				} else if( form.$valid ) {
					form && this.setAttr(form.inputCode, "readonly", true);
					ctrl.status = "wait";
					scope.$apply(); // Display the spinner
					const newStatus = await ctrl.validateCode();
					if( newStatus==="ok" ) {
						// Lock UI and redirect after a few seconds
						this.setAttr('btnRenew', "disabled", true);
						this.setAttr('btnBack',  "disabled", true);
						if( ctrl.redirect ) {
							setTimeout( () => {
								try {
									const url = new URL(ctrl.redirect);
									window.location.href = url.toString();
								} catch {
									// silent fail
								}
							}, 2000);
						}
					} else {
						// Unlock UI
						form && this.setAttr(form.inputCode, "readonly", false);
					}
				}
			} catch {
			} finally {
				this.setAttr('btnRenew', "disabled", false);
				scope.$apply();
			}
		}

		scope.onCodeRenew = async () => {
			angular.element(document.getElementById('btnRenew')).prop("disabled", "disabled");
			await ctrl.renewCode();
			setTimeout( ()=>angular.element(document.getElementById('btnRenew')).prop("disabled", false), 15000);
			scope.$apply();
		}

		ctrl.initialize()
		.then( () => {
			scope.canRenderUi = true;
			scope.$apply();
			setTimeout( ()=>document.getElementById("input-data").focus(), 10 );
		});
    }
}

/**
 * The validate-mfa directive.
 * Usage:
 *   &lt;validate-mfa force?="true" redirect?="URL"></validate-mfa&gt;
 */
export function DirectiveFactory() {
	return new Directive();
}