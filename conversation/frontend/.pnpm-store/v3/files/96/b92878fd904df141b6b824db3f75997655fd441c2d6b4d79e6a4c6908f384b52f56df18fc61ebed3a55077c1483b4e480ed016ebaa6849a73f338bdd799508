import { ReleaseType } from 'semver';
import { ReleaseGroupWithName } from './filter-release-groups';
export interface VersionPlanFile {
    absolutePath: string;
    relativePath: string;
    fileName: string;
    createdOnMs: number;
}
export interface RawVersionPlan extends VersionPlanFile {
    content: Record<string, string>;
    message: string;
}
export interface VersionPlan extends VersionPlanFile {
    message: string;
}
export interface GroupVersionPlan extends VersionPlan {
    groupVersionBump: ReleaseType;
    /**
     * Will not be set if the group name was the trigger, otherwise will be a list of
     * all the individual project names explicitly found in the version plan file.
     */
    triggeredByProjects?: string[];
}
export interface ProjectsVersionPlan extends VersionPlan {
    projectVersionBumps: Record<string, ReleaseType>;
}
export declare function readRawVersionPlans(): Promise<RawVersionPlan[]>;
export declare function setResolvedVersionPlansOnGroups(rawVersionPlans: RawVersionPlan[], releaseGroups: ReleaseGroupWithName[], allProjectNamesInWorkspace: string[]): ReleaseGroupWithName[];
export declare function getVersionPlansAbsolutePath(): string;
