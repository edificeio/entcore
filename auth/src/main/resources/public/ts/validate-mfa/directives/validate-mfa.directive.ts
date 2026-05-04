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
	public otpDigits: string[] = ['', '', '', '', '', ''];
	public status:OTPStatus = "";
	public koStatusCause = "";
	// Server data
	private infos?: IMfaInfos;
	
	// TOTP enrollment management
	public showTotpEnrollment: boolean = false;
	public totpSecret?: string = "";
	private savingTotp: boolean = false;
	private userHasTotp: boolean = false;

	public async initialize() {
		try {
			await Promise.all([
				notif().onSessionReady().promise,
				conf().Platform.idiom.addBundlePromise("/auth/i18n")
			]);
			// Initialize userHasTotp from session
			this.userHasTotp = (this.me as any)?.hasTotp === true;
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

	public get isTotp(): boolean {
		return this.infos?.type === 'totp' as any;
	}

	public get hasTotp(): boolean {
		return this.userHasTotp;
	}

	public openTotpEnrollment(): void {
		this.totpSecret = "";
		this.showTotpEnrollment = true;
	}

	public async saveTotp(): Promise<void> {
		if (this.savingTotp) return;
		try {
			this.savingTotp = true;
			const response = await http().putJson('/directory/user/totp', { totp: this.totpSecret });
			if (response.status >= 200 && response.status < 300) {
				this.userHasTotp = true;
				this.showTotpEnrollment = false;
				this.totpSecret = "";
				notify.success('validate-mfa.totp.saved');
			} else {
				notify.error('validate-mfa.totp.error');
			}
		} catch (e) {
			notify.error('validate-mfa.totp.error');
		} finally {
			this.savingTotp = false;
		}
	}

	public async removeTotp(): Promise<void> {
		try {
			const response = await http().putJson('/directory/user/totp', { totp: null });
			if (response.status >= 200 && response.status < 300) {
				this.userHasTotp = false;
				this.showTotpEnrollment = false;
				notify.success('validate-mfa.totp.removed');
			} else {
				notify.error('validate-mfa.totp.error');
			}
		} catch (e) {
			notify.error('validate-mfa.totp.error');
		}
	}

	public cancelTotpEnrollment(): void {
		this.showTotpEnrollment = false;
		this.totpSecret = "";
	}

	public async validateCode():Promise<OTPStatus> {
		// Wait at least 0,5s while validating
		const time = new Date().getTime();

		try {
			const validation = await session().tryMfaCode(this.inputCode);
			if( http().latestResponse.status>=400 ) {
				throw ('validate-mfa.error.network');
			}

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

			const waitMs = 500;
			const duration = Math.min( Math.max(waitMs-new Date().getTime()+time, 0), waitMs);
			const debounceTime:IPromisified<void> = notif().promisify();
			setTimeout( () => debounceTime.resolve(), duration);

			return debounceTime.promise.then( () => this.status );
		} catch( e ) {
			const msg = (typeof e !== "string") ? 'validate-mfa.error.network' : e;
			notify.error(msg);
			this.status = "";
			return this.status;
		}
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
	onDigitChange: (index:number) => void;
	onCodeRenew: () => Promise<void>;
	onOpenTotpEnrollment: () => void;
	onSaveTotp: () => Promise<void>;
	onCancelTotpEnrollment: () => void;
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

        const safeApply = (fn?) => {
            const phase = scope.$root.$$phase;
            if (phase == '$apply' || phase == '$digest') {
                if (fn && (typeof (fn) === 'function')) {
                    fn();
                }
            } else {
                scope.$apply(fn);
            }
        };

		scope.onCodeChange = async (form) => {
			try {
				if( form.$invalid ) {
					ctrl.status = "";
				} else if( form.$valid ) {
					form && this.setAttr(form.inputCode, "readonly", true);
					ctrl.status = "wait";
					safeApply(); // Display the spinner
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
				safeApply();
			}
		}

		scope.onCodeRenew = async () => {
			angular.element(document.getElementById('btnRenew')).prop("disabled", "disabled");
			await ctrl.renewCode();
			setTimeout( ()=>angular.element(document.getElementById('btnRenew')).prop("disabled", false), 15000);
			safeApply();
		}

		scope.onOpenTotpEnrollment = () => {
			ctrl.openTotpEnrollment();
			safeApply();
		}

		scope.onSaveTotp = async () => {
			await ctrl.saveTotp();
			safeApply();
		}

		scope.onDigitChange = (index: number) => {
			// Garder uniquement les chiffres
			ctrl.otpDigits[index] = (ctrl.otpDigits[index] || '').replace(/[^0-9]/g, '').slice(-1);
			if (ctrl.otpDigits[index]) {
				// Avancer au champ suivant
				const next = document.getElementById('otp-digit-' + (index + 1));
				if (next) (next as HTMLInputElement).focus();
			}
			// Si tous les champs sont remplis, valider automatiquement
			const code = ctrl.otpDigits.join('');
			if (code.length === 6 && /^[0-9]{6}$/.test(code)) {
				ctrl.inputCode = code;
				// Bloquer tous les champs
				for (let i = 0; i < 6; i++) {
					const el = document.getElementById('otp-digit-' + i);
					if (el) this.setAttr(el, 'readonly', true);
				}
				ctrl.status = 'wait';
				safeApply();
				ctrl.validateCode().then(newStatus => {
					if (newStatus === 'ok') {
						this.setAttr('btnRenew', 'disabled', true);
						if (ctrl.redirect) {
							setTimeout(() => {
								try {
									const url = new URL(ctrl.redirect);
									window.location.href = url.toString();
								} catch { /* silent fail */ }
							}, 2000);
						}
					} else {
						// Débloquer et vider les champs
						ctrl.otpDigits = ['', '', '', '', '', ''];
						for (let i = 0; i < 6; i++) {
							const el = document.getElementById('otp-digit-' + i);
							if (el) this.setAttr(el, 'readonly', false);
						}
						setTimeout(() => {
							const first = document.getElementById('otp-digit-0');
							if (first) (first as HTMLInputElement).focus();
						}, 10);
					}
					this.setAttr('btnRenew', 'disabled', false);
					safeApply();
				});
			}
		}

		scope.onCancelTotpEnrollment = () => {
			ctrl.cancelTotpEnrollment();
			safeApply();
		}

		// Gestion du backspace : revenir au champ précédent
		for (let i = 0; i < 6; i++) {
			elem[0].addEventListener('keydown', (e: KeyboardEvent) => {
				const target = e.target as HTMLInputElement;
				if (target.id === 'otp-digit-' + i && e.key === 'Backspace' && !target.value && i > 0) {
					const prev = document.getElementById('otp-digit-' + (i - 1)) as HTMLInputElement;
					if (prev) { prev.value = ''; ctrl.otpDigits[i - 1] = ''; prev.focus(); }
				}
			});
		}

		// Gestion du paste : coller un code complet
		elem[0].addEventListener('paste', (e: ClipboardEvent) => {
			const text = e.clipboardData.getData('text').replace(/[^0-9]/g, '').slice(0, 6);
			if (text.length > 0) {
				e.preventDefault();
				for (let i = 0; i < 6; i++) {
					ctrl.otpDigits[i] = text[i] || '';
				}
				safeApply();
				const lastFilled = Math.min(text.length, 5);
				const el = document.getElementById('otp-digit-' + lastFilled) as HTMLInputElement;
				if (el) el.focus();
				if (text.length === 6) scope.onDigitChange(5);
			}
		});

		ctrl.initialize()
		.then( () => {
			scope.canRenderUi = true;
			safeApply();
			setTimeout( ()=>{ const el = document.getElementById("otp-digit-0"); if(el) (el as HTMLInputElement).focus(); }, 10 );
			setTimeout( ()=>angular.element(document.getElementById('btnRenew')).prop("disabled", false), 15000);
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