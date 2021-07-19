/*
 * Copyright © "Open Digital Education", 2019
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.user;

public interface UserDataSync
{
    public final String STATUS_FIELD = "_status";
    public final String OLD_ID_FIELD = "_old_id";
    public final String NEW_ID_FIELD = "_new_id";
    public final String EXPORT_ATTEMPTS_FIELD = "_exportAttemps";
    public final String IS_EXPORTING_FIELD = "_isExporting";
    public final String EXPORT_ID_FIELD = "_exportId";
    public final String IMPORT_ATTEMPTS_FIELD = "_importAttemps";
    public final String IS_IMPORTING_FIELD = "_isImporting";
    public final String PROFILE_FIELD = "profile";

    public final String PERSONNEL_PROFILE = "Personnel";
    public final String TEACHER_PROFILE = "Teacher";

    public enum SyncState
    {
        UNPROCESSED,
        ACTIVATED,
        EXPORTED,
        IMPORTED,
        ERROR_EXPORT,
        ERROR_IMPORT
    }
}
