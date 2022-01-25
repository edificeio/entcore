package org.entcore.common.folders;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.entcore.common.folders.impl.FolderImporterZip;
import org.entcore.common.folders.impl.FolderManagerMongoImpl;
import org.entcore.common.folders.impl.FolderManagerWithQuota;
import org.entcore.common.share.ShareService;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface FolderManager {
	String FOLDER_TYPE = "folder";
	String FILE_TYPE = "file";
	void setAllowDuplicate(boolean allowDuplicate);
	static FolderManager mongoManager(String collection, Storage sto, Vertx vertx, ShareService shareService, String imageResizerAddress, boolean useOldQueryChildren) {
		return new FolderManagerMongoImpl(collection, sto, vertx, vertx.fileSystem(), vertx.eventBus(), shareService, imageResizerAddress, useOldQueryChildren);
	}

	static FolderManager mongoManagerWithQuota(String collection, Storage sto, Vertx vertx,
			ShareService shareService, String imageResizerAddress, QuotaService quota, int quotaTreshold, boolean useOldQueryChildren) {
		return new FolderManagerWithQuota(collection, quotaTreshold, quota,
				mongoManager(collection, sto, vertx, shareService, imageResizerAddress, useOldQueryChildren), vertx,useOldQueryChildren);
	}

	/**
	 * 
	 * @param query   multi criteria query
	 * @param user    user executing the query
	 * @param handler emitting the number of files matching the query
	 */
	void countByQuery(ElementQuery query, UserInfos user, Handler<AsyncResult<Integer>> handler);

	/**
	 * 
	 * @param query   multi criteria query
	 * @param user    user executing the query
	 * @param handler emitting all files matching the query
	 */
	void findByQuery(ElementQuery query, UserInfos user, Handler<AsyncResult<JsonArray>> handler);

	/**
	 *
	 * @param parentId  the folder containing the file
	 * @param doc       the file object
	 * @param ownerId   id of user saving the file
	 * @param ownerName name of user saving the file
	 * @param handler   the handler that emit the file saved
	 */
	void addFile(Optional<String> parentId, JsonObject doc, String ownerId, String ownerName,
				 Handler<AsyncResult<JsonObject>> handler);
	/**
	 *
	 * @param parent the folder containing the file
	 * @param doc       the file object
	 * @param ownerId   id of user saving the file
	 * @param ownerName name of user saving the file
	 * @param handler   the handler that emit the file saved
	 */
	void addFileWithParent(Optional<JsonObject> parent, JsonObject doc, String ownerId, String ownerName,
				 Handler<AsyncResult<JsonObject>> handler);

	/**
	 * 
	 * @param id       of the file to update
	 * @param parentId folder containing the file
	 * @param doc      the content of the file object
	 * @param handler  the handler that emit the file object save or an error if any
	 */
	void updateFile(String id, Optional<String> parentId, JsonObject doc, Handler<AsyncResult<JsonObject>> handler);


	/**
	 *
	 * @param filePath of the file
	 * @param oldId    the previous file id, if any
	 * @param userId   the user's id
	 * @param handler  the handler that emit the file object save or an error if any
	 */
	void importFile(String filePath, String oldId, String userId, Handler<JsonObject> handler);


	/**
	 *
	 * @param context containing all infos about import
	 * @param handler  the handler that emit the file object save or an error if any
	 */
	void importFileZip(FolderImporterZip.FolderImporterZipContext context, Handler<AsyncResult<JsonObject>> handler);

	/**
	 *
	 * @param zipPath of the zip file
	 * @param user   the user's doing import
	 * @param handler  the handler that emit the file object save or an error if any
	 */
	default void importFileZip(String zipPath, UserInfos user, Handler<AsyncResult<JsonObject>> handler){
		importFileZip(new FolderImporterZip.FolderImporterZipContext(zipPath, user, "invalid"), handler);
	}

	/**
	 * Create the file's thumbnails if applicable
	 *
	 * @param document		the image document to update
	 * @param thumbnails	the thumbnails object, 
	 * 						where keys are strings formatted like "WIDTHxHEIGHT" (WIDTH and HEIGHT being integers).
	 * 						Values are not used.
	 * @param handler		the handler that emits the saved file
	 */
	public void createThumbnailIfNeeded(JsonObject document, JsonObject thumbnails, Handler<AsyncResult<JsonObject>> handler);

	/**
	 * Add thumbnails to the file
	 *
	 * @param document		the document to update
	 * @param thumbnails	the thumbnails object, 
* 							where keys are strings formatted like "WIDTHxHEIGHT" (WIDTH and HEIGHT being integers),
	 * 						and values are the filename (UUID) of the corresponding thumbnail image in storage.
	 * @param handler		the handler that emits the saved file
	 */
	public void addThumbnails(JsonObject document, JsonObject thumbnails, Handler<AsyncResult<JsonObject>> handler);

	/**
	 * Create an external folder that will be displayed at the root of tree (edumedia for example)
	 *
	 * @param folder  informations
	 * @param user    owner of the folder
	 * @param externalId a virtual ID used to link document to the external folder
	 * @param handler emit the folder informations with the DB id created
	 */
	void createExternalFolder(JsonObject folder, UserInfos user, String externalId, final Handler<AsyncResult<JsonObject>> handler);
	/**
	 * Create a folder as root folder
	 *
	 * @param folder  informations
	 * @param user    owner of the folder
	 * @param handler emit the folder informations with the DB id created
	 */
	void createFolder(JsonObject folder, UserInfos user, final Handler<AsyncResult<JsonObject>> handler);

	/**
	 * Create a folder under destination folder
	 * 
	 * @param destinationFolderId
	 * @param user                owner of the folder
	 * @param folder              informations
	 * @param handler             emit the folder informations with the DB id
	 *                            created
	 */
	void createFolder(String destinationFolderId, UserInfos user, JsonObject folder,
			final Handler<AsyncResult<JsonObject>> handler);

	/**
	 * get infos related to a folder or a file
	 * 
	 * @param id      of the file or the folder
	 * @param user    reading infos
	 * @param handler emit info related to the folder or the file
	 */
	void info(String id, UserInfos user, final Handler<AsyncResult<JsonObject>> handler);

	/**
	 * list children belonging to a folder (files or folders)
	 * 
	 * @param idFolder
	 * @param user
	 * @param handler  emit the list of files/folders belonging to the folder
	 */
	void list(String idFolder, UserInfos user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * 
	 * List all folders that belongs to or are shared with the user. Starting from
	 * idFolder
	 * 
	 * @param idFolder id of the folder from which we search
	 * @param user
	 * @param handler  emit the list of files/folders under the root folder
	 *                 (recursively)
	 */
	void listFoldersRecursivelyFromFolder(String idFolder, UserInfos user,
			final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * List all folders that belongs to or are shared with the user
	 * 
	 * @param user
	 * @param handler emit the list of files/folders under the root folder
	 *                (recursively)
	 */
	void listFoldersRecursively(UserInfos user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * 
	 * @param id      of the root folder or of the file
	 * @param user    the user to check wich files he is able to see
	 * @param request
	 */
	void downloadFile(String id, UserInfos user, HttpServerRequest request);

	/**
	 * 
	 * @param ids     id's list of files and folders
	 * @param user    the user to check wich files he is able to see
	 * @param request
	 */
	void downloadFiles(Collection<String> ids, UserInfos user, boolean includeDeleted, HttpServerRequest request);

	/**
	 * 
	 * @param id      of the file or the folder
	 * @param user    sharing the file or the folder
	 * @param handler emit success if inheritedShared has been computed successfully
	 */
	void updateShared(String id, UserInfos user, final Handler<AsyncResult<Void>> handler);

	/**
	 * 
	 * @param id      of the file or the folder
	 * @param newName the new name of the file or folder
	 * @param user    user renaming file
	 * @param handler emit an error if rename failed
	 */
	void rename(String id, String newName, UserInfos user, final Handler<AsyncResult<JsonObject>> handler);

	/**
	 * 
	 * @param sourceId            of the file or the folder
	 * @param destinationFolderId
	 * @param user                moving the folder
	 * @param handler             emit moved file an error if destination does not
	 *                            exists or an error occurs
	 */
	void move(String sourceId, String destinationFolderId, UserInfos user,
			final Handler<AsyncResult<JsonObject>> handler);

	/**
	 * 
	 * @param sourceId            collection of id's file or the folder
	 * @param destinationFolderId
	 * @param user                moving the folder
	 * @param handler             emit moved files an error if destination does not
	 *                            exists or an error occurs
	 */
	void moveAll(Collection<String> sourceId, String destinationFolderId, UserInfos user,
			final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * 
	 * @param sourceId            of the file or the folder
	 * @param destinationFolderId the id of the destination folder or empty if the
	 *                            destination is root
	 * @param user                the user doing the copy
	 * @param handler             emit the list of copied files/folders or an error
	 *                            if the destination or the source does not exists
	 *                            or an error occurs
	 */
	void copy(String sourceId, Optional<String> destinationFolderId, UserInfos user,
			final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * copy a file or folder without checking user visibility
	 * 
	 * @param sourceId            of the file or the folder
	 * @param destinationFolderId the id of the destination folder or empty if the
	 *                            destination is root
	 * @param user                the user doing the copy
	 * @param handler             emit the list of copied files/folders or an error
	 *                            if the destination or the source does not exists
	 *                            or an error occurs
	 */
	void copyUnsafe(String sourceId, Optional<String> destinationFolderId, UserInfos user,
			final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * 
	 * @param sourceIds           collection of id's file or the folder
	 * @param destinationFolderId the id of the destination folder or empty if the
	 *                            destination is root
	 * @param user                the user doing the copy
	 * @param handler             emit the list of copied files/folders or an error
	 *                            if the destination or the source does not exists
	 *                            or an error occurs
	 */
	void copyAll(Collection<String> sourceIds, Optional<String> destinationFolderId, UserInfos user,
			final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * trash only make the file or the folder not visible but it is still saved in
	 * hard disk. If you want to delete it definitely use delete
	 * 
	 * @param id      of the file or directory. In case of directory, the method
	 *                trash all files and folders recursively
	 * @param user    the user trashing the folder or the file
	 * @param handler emit a list of the files/folders ID trashed
	 */
	void trash(String id, UserInfos user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * restore a trashed file or folder
	 * 
	 * @param id      of the file or directory. In case of directory, the method
	 *                restore all files and folders recursively
	 * @param user    the user restoring the folder or the file
	 * @param handler emit a list of the files/folders ID restored
	 */
	void restore(String id, UserInfos user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * trash only make the file or the folder not visible but it is still saved in
	 * hard disk. If you want to delete it definitely use delete
	 * 
	 * @param ids     list of the file or directory. In case of directory, the
	 *                method trash all files and folders recursively
	 * @param user    the user trashing the folder or the file
	 * @param handler emit a list of the files/folders ID trashed
	 */
	void trashAll(Set<String> ids, UserInfos user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * restore a trashed file or folder
	 * 
	 * @param ids     list of the file or directory. In case of directory, the
	 *                method restore all files and folders recursively
	 * @param user    the user restoring the folder or the file
	 * @param handler emit a list of the files/folders ID restored
	 */
	void restoreAll(Set<String> ids, UserInfos user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * delete a file or a folder definitly
	 * 
	 * @param id      of the file or directory. In case of directory, the method
	 *                delete all files and folders recursively
	 * @param user    the user deleting the folder or the file
	 * @param handler emit a list of the files/folders deleted
	 */
	void delete(String id, UserInfos user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * delete all files or a folders definitly
	 * 
	 * @param ids     set of the file or directory. In case of directory, the method
	 *                delete all files and folders recursively
	 * @param user    the user deleting the folder or the file
	 * @param handler emit a list of the files/folders deleted
	 */
	void deleteAll(Set<String> ids, UserInfos user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * delete definitely files and folders matching query
	 * 
	 * @param query
	 * @param user    the user if any (could be in case of automatic task)
	 * @param handler emit the list of files or folders deleted
	 */
	void deleteByQuery(ElementQuery query, Optional<UserInfos> user, final Handler<AsyncResult<JsonArray>> handler);

	/**
	 * 
	 * @param id              of the file or the folder to share
	 * @param shareOperations defining what kind of share operation to do
	 * @param h               handler that emit the list of userid who can see files
	 *                        or folders
	 */
	public void share(String id, ElementShareOperations shareOperations, Handler<AsyncResult<JsonObject>> h);

	/**
	 * 
	 * @param ids             collection of the file or the folder IDS to share
	 * @param shareOperations defining what kind of share operation to do
	 * @param h               handler that emit the list of userid who can see files
	 *                        or folders
	 */
	public void shareAll(Collection<String> ids, ElementShareOperations shareOperations,
			Handler<AsyncResult<Collection<JsonObject>>> h);

	/**
	 * 
	 * @param ids
	 * @param h
	 */
	void markFavorites(Collection<String> ids, Boolean markOrUnmark, UserInfos user,
			Handler<AsyncResult<Collection<JsonObject>>> h);
}
