import angular = require("angular");
import { IAttributes, IController, IDirective, IScope } from "angular";
import { L10n, conf, http, session, notify, notif } from "ode-ngjs-front";
import { IEmailValidationInfos, IMobileValidationInfos, IPromisified } from "ode-ts-client";

type OTPStatus = ""|"wait"|"ok"|"ko";

/* Controller for the directive */
export class ValidateMailController implements IController {
    private me = session().user;
	public lang = conf().Platform.idiom;

	// Scoped data
	public step:ValidationStep = "input"; // by default
	public force?:Boolean;
	public redirect?:string;
	public fullscreen?:Boolean;
	public type?:string = "email";

	// Input data
	public emailAddress?:String;
	public mobilePhone?:String;
	public inputCode?:String;
	public acceptableEmailPattern:string = ".*";
	public status:OTPStatus = "";
	public koStatusCause = "";
	// Server data
	private infos?:IEmailValidationInfos | IMobileValidationInfos;

	get isAdml() {
		return this.me.functions && this.me.functions.ADMIN_LOCAL && this.me.functions.ADMIN_LOCAL.scope;
	}

	get isAdmc() {
		return this.me.functions && this.me.functions.SUPER_ADMIN && this.me.functions.SUPER_ADMIN.scope;
	}

	get isTypeEmail	() {
		return this.type === "email";
	}

	public async initialize() {
		this.infos = await Promise.all([
			notif().onSessionReady().promise,
			conf().Platform.idiom.addBundlePromise("/auth/i18n")
		])
		.then( unused => (this.isTypeEmail ? session().getEmailValidationInfos() : session().getMobileValidationInfos()) as Promise<IEmailValidationInfos | IMobileValidationInfos>)
		.catch( e => {
			if(this.isTypeEmail) {
				setTimeout( () => notify.error('validate-mail.error.network', 4000), 500 );
				return null;
			} else if (this.type === "sms") {
				setTimeout( () => notify.error('validate-sms.error.network', 4000), 500 );
				return null;
			}
		});

		if( this.infos ) {
			if( this.step == "input" ) {
				if(this.isTypeEmail) {
					const emailInfo = this.infos as IEmailValidationInfos;
					if( !emailInfo.emailState 
							|| emailInfo.emailState.state !== "valid"
							|| emailInfo.emailState.valid != emailInfo.email ) {
						// Auto-fill the email address field
						this.emailAddress = emailInfo.email || "";
					}
					if( emailInfo.emailState && emailInfo.emailState.valid && emailInfo.emailState.valid.length>0 ) {
						// Reject the current valid email address (cannot be validated twice)
						this.acceptableEmailPattern = "^(?!"+emailInfo.emailState.valid+"$).*";
					}
				} else {
					const mobileInfo = this.infos as IMobileValidationInfos;
					if( !mobileInfo.mobileState 
							|| mobileInfo.mobileState.state !== "valid"
							|| mobileInfo.mobileState.valid != mobileInfo.mobile ) {
						// Auto-fill the phone number field
						this.mobilePhone = mobileInfo.mobile || "";
					}
				}
			} else if (this.isTypeEmail) {
				const emailInfo = this.infos as IEmailValidationInfos;
				// Before displaying the step 2 immediately, the emailAddress must be initialized.
				this.emailAddress = emailInfo.emailState.pending;
			} else {
				const mobileInfo = this.infos as IMobileValidationInfos;
				// Before displaying the step 2 immediately, the phone number must be initialized.
				this.mobilePhone = mobileInfo.mobileState.pending;
			}
		}
	}

	public inputToBTCss(input) {
		return {
			'form-control': true,
			'is-invalid': input.$invalid
		};
	}

	public validateMail() {
		// Wait at least infos.waitInSeconds (defaults to 10) seconds while validating
		const time = new Date().getTime();

		return session().checkEmail(this.emailAddress)
		.then( () => {
			this.step = "code";
			this.inputCode && delete this.inputCode;
		})
		.catch( e => {
			notify.error('validate-mail.error.network');
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

	public validateSms() {
		// Wait at least infos.waitInSeconds (defaults to 10) seconds while validating
		const time = new Date().getTime();

		return session().checkMobile(this.mobilePhone)
		.then( () => {
			this.step = "code";
			this.inputCode && delete this.inputCode;
		})
		.catch( e => {
			notify.error('validate-sms.error.network');
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

		return (this.isTypeEmail ? session().tryEmailValidation(this.inputCode) : session().tryMobileValidation(this.inputCode))
		.then( validation => {
			if( validation.state === "valid" ) {
				this.status = "ok";
			} else {
				this.status = "ko";
				if( validation.state === "outdated" && this.isTypeEmail ) {
					this.koStatusCause = 'validate-mail.error.ttl';
				} else if (validation.state === "outdated" && this.type === "sms") {
					this.koStatusCause = 'validate-sms.error.ttl';
				} else if (this.isTypeEmail){
					this.koStatusCause = 'validate-mail.error.code';
				} else {
					this.koStatusCause = 'validate-sms.error.code';
				}
			}
		})
		.catch( e => {
			notify.error(this.isTypeEmail ? 'validate-mail.error.network' : 'validate-sms.error.network');
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
		return (this.isTypeEmail ? session().checkEmail(this.emailAddress) : session().checkMobile(this.mobilePhone))
		.then( () => (this.isTypeEmail ? session().getEmailValidationInfos() : session().getMobileValidationInfos()) as Promise<IEmailValidationInfos | IMobileValidationInfos>)
		.then( infos => {
			notify.success(this.isTypeEmail ? 'validate-mail.step2.renewed' : 'validate-sms.step2.renewed');
			this.infos = infos;
			this.inputCode = "";
			this.status = "";
			this.koStatusCause = "";
		});
	}
};

interface ValidateMailScope extends IScope {
	step?: ValidationStep;
	canRenderUi: boolean;
	type: string;
	onValidate: (step:ValidationStep) => Promise<void>;
	onCodeChange: (form:angular.IFormController) => Promise<void>;
	onCodeRenew: () => Promise<void>;
}

/* Directive */
class Directive implements IDirective<ValidateMailScope,JQLite,IAttributes,IController[]> {
    restrict = 'E';
	template = require("./validate-mail.directive.html");
    scope = {
		step: "=?",
		force: "=?",
		redirect: "=?",
		fullscreen: "=?",
		type: "=?"
    };
	bindToController = true;
	controller = [ValidateMailController];
	controllerAs = 'ctrl';
	require = ['validateMail'];

	private setAttr(el:string|HTMLElement, attr:"disabled"|"readonly", enabled:boolean = true) {
		if( typeof el==="string" )
			el = document.getElementById(el);
		if( el )
			angular.element(el).prop(attr, enabled ? attr : "");
	}

    link(scope:ValidateMailScope, elem:JQLite, attr:IAttributes, controllers:IController[]|undefined) {
        const ctrl:ValidateMailController|null = controllers ? controllers[0] as ValidateMailController : null;
        if(!ctrl) return;

		scope.canRenderUi = false;

		scope.onValidate = async (step:ValidationStep): Promise<void> => {
			ctrl.status = "wait";
			if( step === "input" ) {
				if(ctrl.type === "email") {
					await ctrl.validateMail();
				} else if (ctrl.type === "sms") {
					await ctrl.validateSms();
				}
			}
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
 * The validate-mail directive.
 * Set step="email" to display the first screen (=email input).
 * Set step="code"  to display the second screen (=code input).
 * Set force="true" when the user MUST validate his email address.
 * Set redirect="URL" when the user must be redirected after validation.
 * Set fullscreen="true" when no need of header (example when embedded in an iframe)
 *
 * Usage:
 *   &lt;validate-mail step?="email|code" force?="true" redirect?="URL"></validate-mail&gt;
 */
export function DirectiveFactory() {
	return new Directive();
}