export type MfaType = "sms" | "email";

export class Context {
    cgu: boolean;
    mandatory: any;
    mfaConfig?: Array<MfaType>;
    passwordRegex: string;
    passwordRegexI18n?: {[countryCode:string]: string };
}
