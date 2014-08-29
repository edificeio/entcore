package migrateScrapbooks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.util.JSON;

public class Main {
	
	public static void printCursor(DBCursor cursor){
		try {
			while(cursor.hasNext()) {
				System.out.println(cursor.next());
			}
		} finally {
			cursor.close();
		}
	}
	
	public static DBObject reformat(String content, GridFSDBFile file, boolean trash){
		DBObject insert_object = (DBObject) JSON.parse(content);
		String ownerId   = (String) insert_object.get("owner");
		String ownerName = (String) insert_object.get("ownerName");
		if(trash)
			insert_object.put("trashed", 1);
		else
			insert_object.put("trashed", 0);
		insert_object.removeField("owner");
		insert_object.removeField("ownerName");
		insert_object.put("name", insert_object.get("title"));
		insert_object.put("owner", JSON.parse("{ 'userId': '"+ownerId+"', 'displayName': '"+ownerName+"' }"));
		insert_object.put("modified", file.get("uploadDate"));
		System.out.println(insert_object);
		return insert_object;
	}
	
	public static void main(String[] args) throws IOException {
		
			//ARGS PARSING
		final String DB_ADDRESS = args.length == 0 ? "localhost:27017" : args[0];
		final String DB_NAME 	= args.length  < 2 ? "one_gridfs" 	   : args[1];
		
			//MONGO VARS INIT
		final BasicDBObject query_not_trashed = new BasicDBObject(),
							query_trashed     = new BasicDBObject();
		
		final MongoClient client 	= new MongoClient(DB_ADDRESS);
		final DB db 				= client.getDB(DB_NAME);
		final DBCollection docs 	= db.getCollection("documents");
		final DBCollection scraps 	= db.getCollection("scrapbook");
		final GridFS gridfs 		= new GridFS(db);
		
			//QUERIES
		query_not_trashed.put("folder", "scrapbooks");
		query_trashed.put("old-folder", "scrapbooks");
		
			//MONGO OPS
		DBCursor result_not_trashed = docs.find(query_not_trashed);
		DBCursor result_trashed 	= docs.find(query_trashed);
		
		try {
			while(result_not_trashed.hasNext()) {
				final DBObject result_item = result_not_trashed.next();
				final BasicDBObject query_file = new BasicDBObject();
				
				query_file.put("_id", result_item.get("file"));
				GridFSDBFile file = gridfs.findOne(query_file);
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line;
				try {
					while((line = reader.readLine()) != null) 
						builder.append(line);
					scraps.insert(reformat(builder.toString(), file, false));
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					reader.close();
				}
			}
		} finally {
			result_not_trashed.close();
		}
		
		try {
			while(result_trashed.hasNext()) {
				final DBObject result_item = result_trashed.next();
				final BasicDBObject query_file = new BasicDBObject();
				
				query_file.put("_id", result_item.get("file"));
				GridFSDBFile file = gridfs.findOne(query_file);
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line;
				try {
					while((line = reader.readLine()) != null) 
						builder.append(line);
					scraps.insert(reformat(builder.toString(), file, true));
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					reader.close();
				}
			}
		} finally {
			result_trashed.close();
		}
		
		
	}
}
