export interface WelcomeResponse {
  enabled: boolean;
  [lang: string]: string | boolean;
}

export type WelcomeState =
  | { status: 'loading' }
  | { status: 'hidden' }
  | { status: 'ready'; html: string };
