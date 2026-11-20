import { SessionModel } from '../../core/store/models/session.model';

/** Workflow right protecting GET /admin/reports (see AdminController#serveReports). */
export const REPORTED_CONTENTS_RIGHT = 'org.entcore.admin.controllers.AdminController|serveReports';

/**
 * Whether the current user may see the "Contenus signalés" (BETA) tab.
 * ADMC are allowed regardless of the workflow right, as on the backend side.
 */
export async function canAccessReportedContents(): Promise<boolean> {
    const session = await SessionModel.getSession();
    return session.isADMC() || session.hasRight(REPORTED_CONTENTS_RIGHT);
}
