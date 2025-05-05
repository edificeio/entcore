export interface AppRegistrationRequestDTO {
  application?: AppRegistrationDTO;
  actions?: FrWseducWebutilsSecuritySecuredAction[];
}
export interface AppRegistrationDTO {
  name?: string;
  displayName?: string;
  appType?: string;
  icon?: string;
  address?: string;
  display?: boolean;
  prefix?: string;
  customProperties?: {
    [k: string]: {};
  };
}
export interface FrWseducWebutilsSecuritySecuredAction {
  name?: string;
  displayName?: string;
  type?: string;
}


export interface AppRegistrationResponseDTO {
  success?: boolean;
  message?: string;
}

