export interface App {
    name: string;
    address: string;
    icon: string;
    target: string;
    displayName: string;
    display: boolean;
    prefix: string;
    casType: string;
    scope: Array<string>;
    isExternal: boolean
}