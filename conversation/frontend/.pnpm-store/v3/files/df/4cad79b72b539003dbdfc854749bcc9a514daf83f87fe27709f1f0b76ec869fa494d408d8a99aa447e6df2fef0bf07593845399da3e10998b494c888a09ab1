"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.releasePlanCheckCLIHandler = void 0;
exports.createAPI = createAPI;
const nx_json_1 = require("../../config/nx-json");
const workspace_projects_1 = require("../../project-graph/affected/locators/workspace-projects");
const file_map_utils_1 = require("../../project-graph/file-map-utils");
const file_utils_1 = require("../../project-graph/file-utils");
const project_graph_1 = require("../../project-graph/project-graph");
const all_file_data_1 = require("../../utils/all-file-data");
const command_line_utils_1 = require("../../utils/command-line-utils");
const ignore_1 = require("../../utils/ignore");
const output_1 = require("../../utils/output");
const params_1 = require("../../utils/params");
const config_1 = require("./config/config");
const deep_merge_json_1 = require("./config/deep-merge-json");
const filter_release_groups_1 = require("./config/filter-release-groups");
const version_plans_1 = require("./config/version-plans");
const print_config_1 = require("./utils/print-config");
const releasePlanCheckCLIHandler = (args) => (0, params_1.handleErrors)(args.verbose, () => createAPI({})(args));
exports.releasePlanCheckCLIHandler = releasePlanCheckCLIHandler;
function createAPI(overrideReleaseConfig) {
    return async function releasePlanCheck(args) {
        const projectGraph = await (0, project_graph_1.createProjectGraphAsync)({ exitOnError: true });
        const nxJson = (0, nx_json_1.readNxJson)();
        const userProvidedReleaseConfig = (0, deep_merge_json_1.deepMergeJson)(nxJson.release ?? {}, overrideReleaseConfig ?? {});
        if (args.verbose) {
            process.env.NX_VERBOSE_LOGGING = 'true';
        }
        // Apply default configuration to any optional user configuration
        const { error: configError, nxReleaseConfig } = await (0, config_1.createNxReleaseConfig)(projectGraph, await (0, file_map_utils_1.createProjectFileMapUsingProjectGraph)(projectGraph), userProvidedReleaseConfig);
        if (configError) {
            return await (0, config_1.handleNxReleaseConfigError)(configError);
        }
        // --print-config exits directly as it is not designed to be combined with any other programmatic operations
        if (args.printConfig) {
            return (0, print_config_1.printConfigAndExit)({
                userProvidedReleaseConfig,
                nxReleaseConfig,
                isDebug: args.printConfig === 'debug',
            });
        }
        const { error: filterError, releaseGroups, releaseGroupToFilteredProjects, } = (0, filter_release_groups_1.filterReleaseGroups)(projectGraph, nxReleaseConfig, args.projects, args.groups);
        if (filterError) {
            output_1.output.error(filterError);
            process.exit(1);
        }
        // If no release groups have version plans enabled, provide an explicit error
        if (!releaseGroups.some((group) => group.versionPlans)) {
            output_1.output.error({
                title: 'Version plans are not enabled',
                bodyLines: [
                    'Please ensure at least one release group has version plans enabled in your Nx release configuration if you want to use this command.',
                    // TODO: Add docs link here once it is available
                ],
            });
            return 1;
        }
        const rawVersionPlans = await (0, version_plans_1.readRawVersionPlans)();
        (0, version_plans_1.setResolvedVersionPlansOnGroups)(rawVersionPlans, releaseGroups, Object.keys(projectGraph.nodes));
        // Resolve the final values for base, head etc to use when resolving the changes to consider
        const { nxArgs } = (0, command_line_utils_1.splitArgsIntoNxArgsAndOverrides)(args, 'affected', {
            printWarnings: args.verbose,
        }, nxJson);
        const changedFiles = (0, command_line_utils_1.parseFiles)(nxArgs).files;
        if (nxArgs.verbose) {
            if (changedFiles.length) {
                output_1.output.log({
                    title: `Changed files based on resolved "base" (${nxArgs.base}) and "head" (${nxArgs.head ?? 'HEAD'})`,
                    bodyLines: changedFiles.map((file) => `  - ${file}`),
                });
            }
            else {
                output_1.output.warn({
                    title: 'No changed files found based on resolved "base" and "head"',
                });
            }
        }
        const resolvedAllFileData = await (0, all_file_data_1.allFileData)();
        /**
         * Create a minimal subset of touched projects based on the configured ignore patterns, we only need
         * to recompute when the ignorePatternsForPlanCheck differs between release groups.
         */
        const serializedIgnorePatternsToTouchedProjects = new Map();
        const NOTE_ABOUT_VERBOSE_LOGGING = 'Run with --verbose to see the full list of changed files used for the touched projects logic.';
        let hasErrors = false;
        for (const releaseGroup of releaseGroups) {
            // The current release group doesn't leverage version plans
            if (!releaseGroup.versionPlans) {
                continue;
            }
            const resolvedVersionPlans = releaseGroup.resolvedVersionPlans || [];
            // Check upfront if the release group as a whole is featured in any version plan files
            const matchingVersionPlanFiles = resolvedVersionPlans.filter((plan) => 'groupVersionBump' in plan);
            if (matchingVersionPlanFiles.length) {
                output_1.output.log({
                    title: `${releaseGroup.name === config_1.IMPLICIT_DEFAULT_RELEASE_GROUP
                        ? `There are`
                        : `Release group "${releaseGroup.name}" has`} pending bumps in version plan(s)`,
                    bodyLines: [
                        ...matchingVersionPlanFiles.map((plan) => `  - "${plan.groupVersionBump}" in ${plan.fileName}`),
                    ],
                });
                continue;
            }
            // Exclude patterns from .nxignore, .gitignore and explicit version plan config
            let serializedIgnorePatterns = '[]';
            const ignore = (0, ignore_1.getIgnoreObject)();
            if (typeof releaseGroup.versionPlans !== 'boolean' &&
                Array.isArray(releaseGroup.versionPlans.ignorePatternsForPlanCheck) &&
                releaseGroup.versionPlans.ignorePatternsForPlanCheck.length) {
                output_1.output.note({
                    title: `Applying configured ignore patterns to changed files${releaseGroup.name !== config_1.IMPLICIT_DEFAULT_RELEASE_GROUP
                        ? ` for release group "${releaseGroup.name}"`
                        : ''}`,
                    bodyLines: [
                        ...releaseGroup.versionPlans.ignorePatternsForPlanCheck.map((pattern) => `  - ${pattern}`),
                    ],
                });
                ignore.add(releaseGroup.versionPlans.ignorePatternsForPlanCheck);
                serializedIgnorePatterns = JSON.stringify(releaseGroup.versionPlans.ignorePatternsForPlanCheck);
            }
            let touchedProjects = {};
            if (serializedIgnorePatternsToTouchedProjects.has(serializedIgnorePatterns)) {
                touchedProjects = serializedIgnorePatternsToTouchedProjects.get(serializedIgnorePatterns);
            }
            else {
                // We only care about directly touched projects, not implicitly affected ones etc
                const touchedProjectsArr = await (0, workspace_projects_1.getTouchedProjects)((0, file_utils_1.calculateFileChanges)(changedFiles, resolvedAllFileData, nxArgs, undefined, ignore), projectGraph.nodes);
                touchedProjects = touchedProjectsArr.reduce((acc, project) => ({ ...acc, [project]: true }), {});
                serializedIgnorePatternsToTouchedProjects.set(serializedIgnorePatterns, touchedProjects);
            }
            const touchedProjectsUnderReleaseGroup = releaseGroup.projects.filter((project) => touchedProjects[project]);
            if (touchedProjectsUnderReleaseGroup.length) {
                output_1.output.log({
                    title: `Touched projects based on changed files${releaseGroup.name !== config_1.IMPLICIT_DEFAULT_RELEASE_GROUP
                        ? ` under release group "${releaseGroup.name}"`
                        : ''}`,
                    bodyLines: [
                        ...touchedProjectsUnderReleaseGroup.map((project) => `  - ${project}`),
                        '',
                        'NOTE: You can adjust your "versionPlans.ignorePatternsForPlanCheck" config to stop certain files from resulting in projects being classed as touched for the purposes of this command.',
                    ],
                });
            }
            else {
                output_1.output.log({
                    title: `No touched projects found based on changed files${typeof releaseGroup.versionPlans !== 'boolean' &&
                        Array.isArray(releaseGroup.versionPlans.ignorePatternsForPlanCheck) &&
                        releaseGroup.versionPlans.ignorePatternsForPlanCheck.length
                        ? ' combined with configured ignore patterns'
                        : ''}${releaseGroup.name !== config_1.IMPLICIT_DEFAULT_RELEASE_GROUP
                        ? ` under release group "${releaseGroup.name}"`
                        : ''}`,
                });
            }
            const projectsInResolvedVersionPlans = resolvedVersionPlans.reduce((acc, plan) => {
                if ('projectVersionBumps' in plan) {
                    for (const project in plan.projectVersionBumps) {
                        acc[project] = acc[project] || [];
                        acc[project].push({
                            bump: plan.projectVersionBumps[project],
                            fileName: plan.fileName,
                        });
                    }
                }
                return acc;
            }, {});
            // Ensure each touched project under this release group features in at least one version plan file
            let touchedProjectsNotFoundInVersionPlans = [];
            for (const touchedProject of touchedProjectsUnderReleaseGroup) {
                if (!resolvedVersionPlans.length) {
                    touchedProjectsNotFoundInVersionPlans.push(touchedProject);
                    continue;
                }
                const matchingVersionPlanFileEntries = projectsInResolvedVersionPlans[touchedProject];
                if (!matchingVersionPlanFileEntries?.length) {
                    touchedProjectsNotFoundInVersionPlans.push(touchedProject);
                    continue;
                }
            }
            // Log any resolved pending bumps, regardless of whether the projects were directly touched or not
            for (const [projectName, entries] of Object.entries(projectsInResolvedVersionPlans)) {
                output_1.output.log({
                    title: `Project "${projectName}" has pending bumps in version plan(s)`,
                    bodyLines: [
                        ...entries.map(({ bump, fileName }) => `  - "${bump}" in ${fileName}`),
                    ],
                });
            }
            if (touchedProjectsNotFoundInVersionPlans.length) {
                const bodyLines = [
                    `The following touched projects${releaseGroup.name !== config_1.IMPLICIT_DEFAULT_RELEASE_GROUP
                        ? ` under release group "${releaseGroup.name}"`
                        : ''} do not feature in any version plan files:`,
                    ...touchedProjectsNotFoundInVersionPlans.map((project) => `  - ${project}`),
                    '',
                    'Please use `nx release plan` to generate missing version plans, or adjust your "versionPlans.ignorePatternsForPlanCheck" config stop certain files from affecting the projects for the purposes of this command.',
                ];
                if (!nxArgs.verbose) {
                    bodyLines.push('', NOTE_ABOUT_VERBOSE_LOGGING);
                }
                output_1.output.error({
                    title: 'Touched projects missing version plans',
                    bodyLines,
                });
                // At least one project in one release group has an issue
                hasErrors = true;
            }
        }
        // Do not print success message if any projects are missing version plans
        if (hasErrors) {
            return 1;
        }
        const bodyLines = [];
        if (!nxArgs.verbose) {
            bodyLines.push(NOTE_ABOUT_VERBOSE_LOGGING);
        }
        output_1.output.success({
            title: 'All touched projects have, or do not require, version plans.',
            bodyLines,
        });
        return 0;
    };
}
