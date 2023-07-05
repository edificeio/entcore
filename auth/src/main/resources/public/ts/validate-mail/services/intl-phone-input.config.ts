import { TransportFrameworkFactory } from "ode-ts-client";

/** The intl-phone-input conf service provider */
export async function LoadIntlPhoneInputConfig() {
    let defaultConf = {
        onlyCountries: ["fr"]
    };

    try {
        const publicConf = await TransportFrameworkFactory.instance().http.get<any>(`/auth/conf/public`);
        if( publicConf && typeof publicConf['intl-phone-input'] === 'object' ) {
            defaultConf = publicConf['intl-phone-input'];
        }
    } catch {
        // intl-phone-input configuration undefined, keep using default values.
    }
  
    return defaultConf;
  }