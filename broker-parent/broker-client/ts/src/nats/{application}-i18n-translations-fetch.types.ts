export interface FetchTranslationsRequestDTO {
  headers?: {
    [k: string]: string;
  };
  langAndDomain?: LangAndDomain;
  application?: string;
}
export interface LangAndDomain {
  lang?: string;
  domain?: string;
}


export interface FetchTranslationsResponseDTO {
  translations?: {
    [k: string]: string;
  };
}

