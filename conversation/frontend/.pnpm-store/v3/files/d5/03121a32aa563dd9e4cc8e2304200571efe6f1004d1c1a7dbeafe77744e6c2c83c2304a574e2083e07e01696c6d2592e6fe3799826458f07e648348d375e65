import type { NxReleaseConfiguration } from '../../config/nx-json';
/**
 * @public
 */
export declare class ReleaseClient {
    private overrideReleaseConfig;
    releaseChangelog: (args: import("./command-object").ChangelogOptions) => Promise<import("./changelog").NxReleaseChangelogResult>;
    releasePublish: (args: import("./command-object").PublishOptions, isCLI?: boolean) => Promise<number>;
    releaseVersion: (args: import("./command-object").VersionOptions) => Promise<import("./version").NxReleaseVersionResult>;
    release: (args: import("./command-object").ReleaseOptions) => Promise<import("./version").NxReleaseVersionResult | number>;
    constructor(overrideReleaseConfig: NxReleaseConfiguration);
}
/**
 * @public
 */
export declare const releaseChangelog: any;
/**
 * @public
 */
export declare const releasePublish: any;
/**
 * @public
 */
export declare const releaseVersion: any;
/**
 * @public
 */
export declare const release: any;
