package org.entcore.common.s3;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.core.buffer.Buffer;

import java.net.URI;
import java.net.URISyntaxException;

import org.entcore.common.s3.storage.StorageObject;
import org.junit.Test;

import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class S3ClientTest {

    @Test
    public void testReadFile(TestContext context) throws URISyntaxException {
        final Async async = context.async();
        final Vertx vertx = Vertx.vertx();
        
        final URI uri = new URI("https://s3.rbx.io.cloud.ovh.net/");
        final S3Client client = new S3Client(vertx, uri, "***", "***", "rbx", "tests-s3");
        
        client.readFile("1c470afc-d21d-4899-adba-0d6f757f7f30", event -> {
            final StorageObject object = event.result();
            if (object == null) context.fail();

            vertx.fileSystem().writeFile("/tmp/testReadFile.png", object.getBuffer(), result -> {
                if (result.succeeded()) {
                    async.complete();
                }
                else {
                    context.fail();
                }
            });
        });
    }

    @Test
    public void testWriteFile(TestContext context) throws URISyntaxException {
        final Async async = context.async();
        final Vertx vertx = Vertx.vertx();
        
        final URI uri = new URI("https://s3.rbx.io.cloud.ovh.net/");
        final S3Client client = new S3Client(vertx, uri, "***", "***", "rbx", "tests-s3");
        
        final Buffer buffer = vertx.fileSystem().readFileBlocking("/home/pbjoubert/Téléchargements/test1234/f38/default/f38-01-day.png");
        final StorageObject object = new StorageObject(buffer, "test.png", null);

        client.writeFile(object, result -> {
            if (result.succeeded()) {
                async.complete();
            }
            else {
                context.fail();
            }
        });
    }

    @Test
    public void testDeleteFile(TestContext context) throws URISyntaxException {
        final Async async = context.async();
        final Vertx vertx = Vertx.vertx();
        
        final URI uri = new URI("https://s3.rbx.io.cloud.ovh.net/");
        final S3Client client = new S3Client(vertx, uri, "***", "***", "rbx", "tests-s3");
        
        client.deleteFile("707365b4-fd59-4935-8699-1a61fdddbb2a", result -> {
            if (result.succeeded()) {
                async.complete();
            }
            else {
                context.fail();
            }
        });
    }

    @Test
    public void testcopyFile(TestContext context) throws URISyntaxException {
        final Async async = context.async();
        final Vertx vertx = Vertx.vertx();
        
        final URI uri = new URI("https://s3.rbx.io.cloud.ovh.net/");
        final S3Client client = new S3Client(vertx, uri, "***", "***", "rbx", "tests-s3");
        
        client.copyFile("1c470afc-d21d-4899-adba-0d6f757f7f30", result -> {
            if (result.succeeded()) {
                async.complete();
            }
            else {
                context.fail();
            }
        });
    }

    @Test
    public void testWriteToFileSystem(TestContext context) throws URISyntaxException {
        final Async async = context.async();
        final Vertx vertx = Vertx.vertx();
        
        final URI uri = new URI("https://s3.rbx.io.cloud.ovh.net/");
        final S3Client client = new S3Client(vertx, uri, "***", "***", "rbx", "tests-s3");

        client.writeToFileSystem("1c470afc-d21d-4899-adba-0d6f757f7f30", "/tmp/testWriteToFileSystem.png", result -> {
            if (result.succeeded()) {
                async.complete();
            }
            else {
                context.fail();
            }
        });
    }

    @Test
    public void testWriteFromFileSystem(TestContext context) throws URISyntaxException {
        final Async async = context.async();
        final Vertx vertx = Vertx.vertx();
        
        final URI uri = new URI("https://s3.rbx.io.cloud.ovh.net/");
        final S3Client client = new S3Client(vertx, uri, "***", "***", "rbx", "tests-s3");

        client.writeFromFileSystem("1c470afc-d21d-4899-adba-0d6f757f7f31", "/tmp/testWriteToFileSystem.png", result -> {
            if (result.getString("status") == "ok") {
                async.complete();
            }
            else {
                context.fail();
            }
        });
    }

}
