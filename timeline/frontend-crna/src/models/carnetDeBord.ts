export type Structure = {
  structureId: string;
  xmlResponse: string;
  address: string;
};

export type ContentTitle = 'lateness' | 'absences' | 'grades' | 'diary' | 'skills';

export type ContentItem = {
  value: string;
  pageUrl?: string;
  subsections?: {
    header: string;
    content: string | null;
    pageUrl?: string;
  }[];
};

export type ContentType = {
  title: ContentTitle;
  compact: string | false;
  full: ContentItem[] | false;
  lightboxTitle: string;
};

export type ParsedEleve = {
  element: Element;
  name: string;
  avatar: string;
  address: string;
};

export type UseCarnetDeBordResult = {
  eleves: ParsedEleve[];
  isLoading: boolean;
  isError: boolean;
};
