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
  link: string;
  source: string;
  plain_text: string;
  favorite: boolean;
  date: number;
  structure_name: string;
  structure_uai: string;
  is_pinned: boolean;
  user: string;
}

export interface MediacentreFavoritesResponse {
  event: string;
  state: string;
  status: string;
  data: MediacentreSignet[];
}
