package com.example.floudcloud.app.operation;


import android.util.Log;

import com.example.floudcloud.app.model.FileUpload;
import com.example.floudcloud.app.network.FloudService;
import com.example.floudcloud.app.utility.ProgressListener;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UploadOperation extends RemoteOperation {
    private final String LOG_TAG = UploadOperation.class.getSimpleName();
    private FileUpload fileUpload;
    private File file;
    private ProgressListener progressListener;
    private long uploaded;
    private String regId;

    public UploadOperation(FileUpload fileUpload, String apiKey, String storagePath, String regId) {
        super(apiKey, FloudService.FILE_URL, fileUpload.path);
        this.uploaded = 0;
        this.fileUpload = fileUpload;
        this.file = new File(storagePath + fileUpload.path);
        this.regId = regId;
    }

    public int postFile() throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(getUri() + "/?regId=" + this.regId);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        FileBody fb = new FileBody(file);

        post.setHeader("Authorization", getApiKey());
        builder.addPart("file", fb);
        builder.addTextBody("path", fileUpload.path);
        builder.addTextBody("size", Long.toString(fileUpload.size));
        builder.addTextBody("hash", fileUpload.hash);
        final HttpEntity yourEntity = builder.build();

        int total = 0;

        class ProgressiveEntity implements HttpEntity {
            @Override
            public void consumeContent() throws IOException {
                yourEntity.consumeContent();
            }

            @Override
            public InputStream getContent() throws IOException,
                    IllegalStateException {
                return yourEntity.getContent();
            }

            @Override
            public Header getContentEncoding() {
                return yourEntity.getContentEncoding();
            }

            @Override
            public long getContentLength() {
                return yourEntity.getContentLength();
            }

            @Override
            public Header getContentType() {
                return yourEntity.getContentType();
            }

            @Override
            public boolean isChunked() {
                return yourEntity.isChunked();
            }

            @Override
            public boolean isRepeatable() {
                return yourEntity.isRepeatable();
            }

            @Override
            public boolean isStreaming() {
                return yourEntity.isStreaming();
            } // CONSIDER put a _real_ delegator into here!

            @Override
            public void writeTo(OutputStream outstream) throws IOException {

                class ProxyOutputStream extends FilterOutputStream {
                    /**
                     * @author Stephen Colebourne
                     */

                    public ProxyOutputStream(OutputStream proxy) {
                        super(proxy);
                    }

                    public void write(int idx) throws IOException {
                        out.write(idx);
                    }

                    public void write(byte[] bts) throws IOException {
                        out.write(bts);
                    }

                    public void write(byte[] bts, int st, int end) throws IOException {
                        out.write(bts, st, end);
                    }

                    public void flush() throws IOException {
                        out.flush();
                    }

                    public void close() throws IOException {
                        out.close();
                    }
                } // CONSIDER import this class (and risk more Jar File Hell)

                class ProgressiveOutputStream extends ProxyOutputStream {
                    public ProgressiveOutputStream(OutputStream proxy) {
                        super(proxy);
                    }

                    public void write(byte[] bts, int st, int end) throws IOException {
                        uploaded += (int) end - st;
                        progressListener.notifyProgress((int) ((float) uploaded * 100 / fileUpload.size));

                        out.write(bts, st, end);
                    }
                }

                yourEntity.writeTo(new ProgressiveOutputStream(outstream));
            }

        }
        ProgressiveEntity myEntity = new ProgressiveEntity();

        post.setEntity(myEntity);
        HttpResponse response = client.execute(post);

        return response.getStatusLine().getStatusCode();
    }

    @Override
    public int execute(ProgressListener progressListener) {
        this.progressListener = progressListener;
        int statusCode = 0;
        try {
            statusCode = postFile();
        } catch (Exception e) {
            Log.e(LOG_TAG, "An exception occurred when downloading " + file.getAbsolutePath());
            Log.e(LOG_TAG, "Exception " + e.getMessage());
            Log.e(LOG_TAG, "Stack trace " + e.getStackTrace());
            return 0;
        }

        return statusCode;
    }
}
