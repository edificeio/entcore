export interface MediacentreSignet {
  _id: string;
  id: string;
  title: string;
  editors: string[];
  authors: string[];
  image: string;
  disciplines: string[];
  levels: string[];
  document_types: string[];
  link?: string;
  /** Personal signets (favorites/pins the user added) return this column instead of `link`. */
  url?: string;
  source: string;
  plain_text: string;
  favorite?: boolean;
  date: number;
  structure_name: string;
  structure_uai: string;
  is_pinned?: boolean;
  user?: string;
  pinned_title?: string;
  pinned_description?: string;
  is_parent?: boolean;
  structure_owner?: string;
  structures_children?: string[];
}

export interface MediacentreFavoritesResponse {
  event: string;
  state: string;
  status: string;
  data: MediacentreSignet[];
}

export interface MediacentrePublishedSignet {
  id: string;
  title: string;
  image: string;
  link: string;
  plain_text: string[];
  /** Set by Mediacentre on publish: `["Orientation"]` when the signet is flagged orientation, `["Signet"]` otherwise. */
  document_types: string[];
}

export interface MediacentrePublishedSignetsResponse {
  event: string;
  state: string;
  status: string;
  data: {
    signets?: {
      source: string;
      resources: MediacentrePublishedSignet[];
    };
  };
}
