export interface RegisterI18NFilesRequestDTO {
  application?: string;
  translationsByLanguage?: {
    [k: string]: {
      [k: string]: string;
    };
  };
}


export interface RegisterI18NFilesResponseDTO {
  application?: string;
  languagesCount?: number;
  translationsCount?: number;
}

