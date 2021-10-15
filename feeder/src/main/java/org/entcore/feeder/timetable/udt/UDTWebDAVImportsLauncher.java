/*
 * Copyright © "Open Digital Education", 2016
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

 */

package org.entcore.feeder.timetable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.FileProps;

import org.entcore.common.storage.Storage;
import org.entcore.feeder.dictionary.structures.PostImport;
import org.entcore.feeder.timetable.edt.EDTUtils;
import org.w3c.dom.events.Event;

public class UDTWebDAVImportsLauncher extends WebDAVImportsLauncher {

	private static final Pattern UDT_WEBDAV_PATTERN = Pattern.compile("([0-9]{7}[A-Z])_([0-9]{14})_UDT\\.zip");
    private static final SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	public UDTWebDAVImportsLauncher(Vertx vertx, Storage storage, String path, PostImport postImport, boolean timetableUserCreation, boolean isManualImport) {
		super(vertx, storage, path, postImport, timetableUserCreation, isManualImport);
	}

	public UDTWebDAVImportsLauncher(Vertx vertx, Storage storage, String path, PostImport postImport, EDTUtils edtUtils,
			boolean timetableUserCreation, boolean isManualImport) {
        super(vertx, storage, path, postImport, edtUtils, timetableUserCreation, isManualImport);
	}

    protected void chooseFileToImport(List<String> files, Handler<String> handler)
    {
		Matcher matcher;
        Date bestDate = null;
        String bestFile = null;

        for(int i = files.size(); i-- > 0;)
        {
            String file = files.get(i);
            if (file != null && (matcher = UDT_WEBDAV_PATTERN.matcher(file)).find())
            {
                try
                {
                    Date date = timestampFormat.parse(matcher.group(2));
                    if(bestDate == null || date.after(bestDate) == true)
                    {
                        bestDate = date;
                        bestFile = file;
                    }
                }
                catch(ParseException e)
                {
                    log.error("Failed to parse timestamp " + matcher.group(2));
                }
            }
        }

        handler.handle(bestFile);
    }
}
