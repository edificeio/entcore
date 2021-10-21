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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.FileHandler;

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

public class WebDAVImportsLauncher extends ImportsLauncher {


	public WebDAVImportsLauncher(Vertx vertx, Storage storage, String path, PostImport postImport, boolean timetableUserCreation, boolean isManualImport) {
		super(vertx, storage, path, postImport, timetableUserCreation, isManualImport);
	}

	public WebDAVImportsLauncher(Vertx vertx, Storage storage, String path, PostImport postImport, EDTUtils edtUtils,
			boolean timetableUserCreation, boolean isManualImport) {
        super(vertx, storage, path, postImport, edtUtils, timetableUserCreation, isManualImport);
	}

	protected void listFiles(Handler<AsyncResult<List<String>>> handler)
	{
        // We want to find all of the latest files in a WebDAV folder structured like so:
        // /webdav/<user-folder>/<timetable-file>
        // In case there are multiple files we want to import only the latest one
        listFilesInDirectory(path, true, false, new Handler<List<String>>()
        {
            @Override
            public void handle(List<String> folders)
            {
                if(folders == null)
					handler.handle(Future.failedFuture("Error reading WebDAV root directory."));
                else
                {
                    List<String> filesToImport = new LinkedList<String>();
					final Handler<List<String>>[] folderHandlers = new Handler[folders.size() + 1];
                    for(int i = folders.size(); i-- > 0;)
                    {
                        final int j = i;
                        folderHandlers[i] = new Handler<List<String>>()
                        {
                            private void next()
                            {
                                if(j + 1 < folders.size())
                                    listFilesInDirectory(folders.get(j + 1), false, true, folderHandlers[j + 1]);
                                else
                                    folderHandlers[j + 1].handle(null);
                            }

                            @Override
                            public void handle(List<String> validFiles)
                            {
                                if(validFiles == null || validFiles.size() == 0)
                                    next();
                                else
                                {
                                    chooseFileToImport(validFiles, new Handler<String>()
                                    {
                                        @Override
                                        public void handle(String choice)
                                        {
                                            if(choice != null)
                                                filesToImport.add(choice);
                                            next();
                                        }
                                    });
                                }
                            }
                        };
                    }

                    folderHandlers[folders.size()] = new Handler<List<String>>()
                    {
                        @Override
                        public void handle(List<String> v)
                        {
                            handler.handle(Future.succeededFuture(filesToImport));
                        }
                    };
                    if(folders.size() > 0)
                        listFilesInDirectory(folders.get(0), false, true, folderHandlers[0]);
                    else
                        folderHandlers[0].handle(new LinkedList<String>());
                }
            }
        });
	}

    private void listFilesInDirectory(String directory, boolean includeDirectories, boolean includeFiles, Handler<List<String>> handler)
    {
        final FileSystem fs = vertx.fileSystem();
		fs.readDir(directory, new Handler<AsyncResult<List<String>>>()
        {
            @Override
            public void handle(AsyncResult<List<String>> result)
            {
				if (result.succeeded())
                {
                    List<String> files = result.result();
					final Handler<List<String>>[] fileHandlers = new Handler[files.size() + 1];

					for (int i = files.size(); i-- > 0;)
                    {
                        final int j = i;
                        fileHandlers[i] = new Handler<List<String>>()
                        {
                            @Override
                            public void handle(List<String> validFiles)
                            {
                                fs.props(files.get(j), new  Handler<AsyncResult<FileProps>>()
                                {
                                    @Override
                                    public void handle(AsyncResult<FileProps> filePropsRes)
                                    {
                                        if(filePropsRes.succeeded())
                                        {
                                            if(filePropsRes.result().isDirectory() == true)
                                            {
                                                if(includeDirectories == true)
                                                    validFiles.add(files.get(j));
                                            }
                                            else if(includeFiles == true)
                                                validFiles.add(files.get(j));

                                        }
                                        fileHandlers[j + 1].handle(validFiles);
                                    }
                                });
                            }
                        };
					}

                    fileHandlers[fileHandlers.length -1] = handler;
                    fileHandlers[0].handle(new LinkedList<String>());
				}
                else
                {
					log.error("Error reading WebDAV directory.");
                    handler.handle(null);
                }
            }
        });
    }

    protected void chooseFileToImport(List<String> files, Handler<String> handler)
    {
        handler.handle(files.get(0));
    }

	protected void importFile(String file, Handler<Void> handler)
	{
        super.importFile(file, new Handler<Void>()
        {
            @Override
            public void handle(Void v)
            {
                String folderToRemove = file.substring(0, file.lastIndexOf(File.separator));
                if(folderToRemove.contains(path) == true && folderToRemove.equals(path) == false)
                {
                    vertx.fileSystem().deleteRecursive(folderToRemove, true, new Handler<AsyncResult<Void>>()
                    {
                        @Override
                        public void handle(AsyncResult<Void> res)
                        {
                            if(res.succeeded() == false)
                                log.error("Error removing WebDAV directory.");
                            handler.handle(null);
                        }
                    });
                }
            }
        });
	}
}
