import { TransportFrameworkFactory } from "ode-ts-client";

/** The intl-phone-input conf service provider */
export async function LoadIntlPhoneInputConfig() {
  /**
   * Configuration object for initializing the international phone input.
   *
   * @property {string} initialCountry - The default country code to be used for the phone input.
   * @property {string[]} preferredCountries - An array of country codes that will be displayed at the top of the country dropdown list.
   */
  let defaultConf: any = {
    initialCountry: "fr",
    preferredCountries: [
      "fr",
      "mx",
      "es",
      "co",
      "gf",
      "pf",
      "gp",
      "yt",
      "nc",
      "pm",
      "wf",
      "gy",
      "mq",
      "mm",
      "ph",
    ],
  };

  try {
    const publicConf = await TransportFrameworkFactory.instance().http.get<any>(
      `/auth/conf/public`
    );
    if (publicConf && typeof publicConf["intl-phone-input"] === "object") {
      defaultConf = {
        ...defaultConf,
        ...publicConf["intl-phone-input"],
      };

      if (defaultConf.initialCountry === "auto") {
        defaultConf.geoIpLookup = (success) => {
          fetch("https://ipapi.co/json")
            .then((res) => res.json())
            .then((data) => success(data.country_code))
            .catch(() => {
              success("FR");
            });
        };
      }
    }
  } catch {
    // intl-phone-input configuration undefined, keep using default values.
  }

  return defaultConf;
}
