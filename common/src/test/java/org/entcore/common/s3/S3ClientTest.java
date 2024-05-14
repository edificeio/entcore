// package org.entcore.common.s3;

// import io.vertx.core.Vertx;
// import io.vertx.ext.unit.Async;
// import io.vertx.ext.unit.TestContext;
// import io.vertx.core.buffer.Buffer;

// import java.net.URI;
// import java.net.URISyntaxException;

// import org.entcore.common.s3.storage.StorageObject;
// import org.junit.Test;

// import io.vertx.ext.unit.junit.VertxUnitRunner;
// import org.junit.runner.RunWith;

// @RunWith(VertxUnitRunner.class)
// public class S3ClientTest {

//     @Test
//     public void testReadFile(TestContext context) throws URISyntaxException {
//         final Async async = context.async();
//         final Vertx vertx = Vertx.vertx();
        
//         final URI uri = new URI("---");
//         final S3Client client = new S3Client(vertx, uri, "---", "---", "---", "---");
        
//         client.readFile("", event -> {
//             final StorageObject object = event.result();
//             if (object == null) context.fail();

//             vertx.fileSystem().writeFile("", object.getBuffer(), result -> {
//                 if (result.succeeded()) {
//                     async.complete();
//                 }
//                 else {
//                     context.fail();
//                 }
//             });
//         });
//     }

//     @Test
//     public void testWriteFile(TestContext context) throws URISyntaxException {
//         final Async async = context.async();
//         final Vertx vertx = Vertx.vertx();
        
//         final URI uri = new URI("---");
//         final S3Client client = new S3Client(vertx, uri, "---", "---", "---", "---");
        
//         final Buffer buffer = vertx.fileSystem().readFileBlocking("");
//         final StorageObject object = new StorageObject(buffer, "", null);

//         client.writeFile(object, result -> {
//             if (result.succeeded()) {
//                 async.complete();
//             }
//             else {
//                 context.fail();
//             }
//         });
//     }

//     @Test
//     public void testDeleteFile(TestContext context) throws URISyntaxException {
//         final Async async = context.async();
//         final Vertx vertx = Vertx.vertx();
        
//         final URI uri = new URI("---");
//         final S3Client client = new S3Client(vertx, uri, "---", "---", "---", "---");
        
//         client.deleteFile("", result -> {
//             if (result.succeeded()) {
//                 async.complete();
//             }
//             else {
//                 context.fail();
//             }
//         });
//     }

//     @Test
//     public void testcopyFile(TestContext context) throws URISyntaxException {
//         final Async async = context.async();
//         final Vertx vertx = Vertx.vertx();
        
//         final URI uri = new URI("---");
//         final S3Client client = new S3Client(vertx, uri, "---", "---", "---", "---");
        
//         client.copyFile("", result -> {
//             if (result.succeeded()) {
//                 async.complete();
//             }
//             else {
//                 context.fail();
//             }
//         });
//     }

//     @Test
//     public void testWriteToFileSystem(TestContext context) throws URISyntaxException {
//         final Async async = context.async();
//         final Vertx vertx = Vertx.vertx();
        
//         final URI uri = new URI("---");
//         final S3Client client = new S3Client(vertx, uri, "---", "---", "---", "---");

//         client.writeToFileSystem("", "", result -> {
//             if (result.succeeded()) {
//                 async.complete();
//             }
//             else {
//                 context.fail();
//             }
//         });
//     }

//     @Test
//     public void testWriteFromFileSystem(TestContext context) throws URISyntaxException {
//         final Async async = context.async();
//         final Vertx vertx = Vertx.vertx();
        
//         final URI uri = new URI("---");
//         final S3Client client = new S3Client(vertx, uri, "---", "---", "---", "---");

//         client.writeFromFileSystem("", "", result -> {
//             if (result.getString("status") == "ok") {
//                 async.complete();
//             }
//             else {
//                 context.fail();
//             }
//         });
//     }

//     @Test
//     public void testGetFileStats(TestContext context) throws URISyntaxException {
//         final Async async = context.async();
//         final Vertx vertx = Vertx.vertx();
        
//         final URI uri = new URI("---");
//         final S3Client client = new S3Client(vertx, uri, "---", "---", "---", "---");

//         client.getFileStats("", result -> {
//             async.complete();
//         });
//     }

// }
