export type Structure = {
  structureId: string;
  xmlResponse: string;
  address: string;
};

export type ContentTitle = 'retards-absences' | 'grades' | 'diary' | 'skills';

export type ContentItem = {
  value: string;
  pageUrl?: string;
  /** Set on items of the merged 'retards-absences' category to tell them apart. */
  kind?: 'lateness' | 'absence';
  /** Reason, when provided by Pronote, for a 'retards-absences' item. */
  motif?: string;
  /** Sortable timestamp, used to order the merged 'retards-absences' list chronologically. */
  date?: number;
  subsections?: {
    header: string;
    content: string | null;
    pageUrl?: string;
    /** Sortable timestamp for the subsection's due date, used to render a day/month badge. */
    date?: number;
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
