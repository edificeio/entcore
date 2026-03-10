/** A single RSS feed in a channel. show can be 2, 3, 5 depending on API. */
export interface BibliocollegeFeed {
    show: number;
    title: string;
    link: string;
    /** When true, this feed comes from platform conf and is read-only. */
    fromConf?: boolean;
}

/** Channel response from GET /channels/structure/:structureId */
export interface Channel {
    _id: string;
    created?: { $date?: string };
    modified?: { $date?: string };
    feeds: BibliocollegeFeed[];
    owner?: { userId: string; displayName: string };
    global?: boolean;
    structureID: string;
}
