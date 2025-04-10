import type { GeneratorCallback } from '../config/misc-interfaces';
import type { ProjectGraph } from '../config/project-graph';
import type { ProjectConfiguration } from '../config/workspace-json-project-json';
import { FsTree, type FileChange, type Tree } from '../generators/tree';
export type SyncGeneratorResult = void | {
    callback?: GeneratorCallback;
    outOfSyncMessage?: string;
};
export type SyncGenerator = (tree: Tree) => SyncGeneratorResult | Promise<SyncGeneratorResult>;
export type SyncGeneratorChangesResult = {
    changes: FileChange[];
    generatorName: string;
    callback?: GeneratorCallback;
    outOfSyncMessage?: string;
};
export declare function getSyncGeneratorChanges(generators: string[]): Promise<SyncGeneratorChangesResult[]>;
export declare function flushSyncGeneratorChanges(results: SyncGeneratorChangesResult[]): Promise<void>;
export declare function collectAllRegisteredSyncGenerators(projectGraph: ProjectGraph): Promise<string[]>;
export declare function runSyncGenerator(tree: FsTree, generatorSpecifier: string, projects: Record<string, ProjectConfiguration>): Promise<SyncGeneratorChangesResult>;
export declare function collectRegisteredTaskSyncGenerators(projectGraph: ProjectGraph): Set<string>;
export declare function collectRegisteredGlobalSyncGenerators(nxJson?: import("../config/nx-json").NxJsonConfiguration<string[] | "*">): Set<string>;
export declare function syncGeneratorResultsToMessageLines(results: SyncGeneratorChangesResult[]): string[];
