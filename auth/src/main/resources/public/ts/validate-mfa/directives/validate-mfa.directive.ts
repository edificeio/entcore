import angular = require("angular");
import { IAttributes, IController, IDirective, IScope } from "angular";
import { L10n, conf, http, session, notify, notif } from "ode-ngjs-front";
import { IMfaInfos, IMobileValidationInfos, IPromisified, IHttpResponse } from "ode-ts-client";

type OTPStatus = ""|"wait"|"ok"|"ko";
type IHttpResponseErrorWithPayload = IHttpResponse & {data?:{error?:any}};

/* Controller for the directive */
export class ValidateMfaController implements IController {
    private me = session().user;
	public lang = conf().Platform.idiom;

	// Scoped data
	public force?:Boolean;
	public redirect?:string;
	public fullscreen?:Boolean;

	// Input data
	public inputCode?:String;
	public status:OTPStatus = "";
	public koStatusCause = "";
	// Server data
	private infos?: IMfaInfos;

	public async initialize() {
		try {
			await Promise.all([
				notif().onSessionReady().promise,
				conf().Platform.idiom.addBundlePromise("/auth/i18n")
			]);
			this.infos = await this.getMfaInfos();
		} catch( e ) {
			setTimeout( () => notify.error('validate-mfa.error.network', 4000), 500 );
		};
	}

	private async getMfaInfos(): Promise<IMfaInfos> {
		try {
			const i = await session().getMfaInfos();
			// We want more details about any error
			const response = http().latestResponse as IHttpResponseErrorWithPayload;
			if( response.status>=400 && typeof response.data?.error === "string" ) {
				let msg = response.data.error;
				if( msg.indexOf('apicall.error')>=0 ) {
					msg = 'apicall.error';
				} else if( msg.indexOf('invalid.receivers')>=0 ) {
					msg = 'invalid.receivers';
				}
				throw ('validate-mfa.error.'+msg);
			}
			return i;
		} catch( e ) {
			const msg = (typeof e !== "string") ? 'validate-mfa.error.network' : e;
			setTimeout( () => notify.error(msg, 4000), 500 );
			return null;
		}
	}

	public get mobile() {
		return session()?.description.mobile;
	}

	public validateCode():Promise<OTPStatus> {
		// Wait at least 0,5s while validating
		const time = new Date().getTime();

		return (session().tryMfaCode(this.inputCode))
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
		return this.getMfaInfos()
		.then( infos => {
			if( infos !== null ) {
				notify.success('validate-mfa.step2.renewed');
			}
			this.infos = infos;
			this.inputCode = "";
			this.status = "";
			this.koStatusCause = "";
		});
	}
};

interface ValidateMfaScope extends IScope {
	canRenderUi: boolean;
	onCodeChange: (form:angular.IFormController) => Promise<void>;
	onCodeRenew: () => Promise<void>;
}

/* Directive */
class Directive implements IDirective<ValidateMfaScope,JQLite,IAttributes,IController[]> {
    restrict = 'E';
	template = require("./validate-mfa.directive.html");
    scope = {
		force: "=?",
		redirect: "=?",
		fullscreen: "=?"
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