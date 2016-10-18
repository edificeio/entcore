// angular is loaded globally
declare let angular;

//webpack stuff
declare function require(path:string);

declare interface require{
    ensure(modulesPaths: string[], cb: (modules) => void);
}

// allow parsing strings as int
declare function parseInt(data:any, radix?:number);

declare interface Range{
    intersectsNode(item: HTMLElement): boolean;
}

declare interface Blob{
    name: string;
}

declare interface Document{
    caretRangeFromPoint(x: number, y: number);
    caretPositionFromPoint(x: number, y: number);
}

declare interface Window{
    MathJax: any;
    Prism: any;
    entcore: any;
    html_beautify: any;
    jQuery: any;
}

declare interface Node{
    innerHTML: string;
}

declare interface Object{
    toJSON(): string | {}
}