export interface FetchTranslationsRequestDTO {
  headers?: {
    [k: string]: {
      [k: string]: unknown;
    };
  };
  langAndDomain?: LangAndDomain;
}
export interface LangAndDomain {
  lang?: string;
  domain?: string;
}


export interface FetchTranslationsResponseDTO {
  translations?: {
    [k: string]: {
      [k: string]: unknown;
    };
  };
}

