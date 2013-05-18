package com.brianthetall.api.lace;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.Reader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * The Client class provides access to the File Lace service (c.btt.com)
 */
public class Client {

    static final private String apiVersion = "0";
    static final private String initPath = "/start/api/";
    static final Random rand = new Random();
    private String token;
    private Cloud cloud;
    static {//prevent old SSL from being used
        System.setProperty("https.protocols", "TLSv1");
    }

    /**
     * @param token BTT Issued OAuth token. Server currently does not check the token.
     */
    public Client(String token) {
        this(token, Cloud.cbtt);
    }

    /**
     * @param token An OAuth token.
     * @param cloud The cloud to use.
     */
    public Client(String token, Cloud cloud) {
        this.token = token;
        this.cloud = cloud;
    }

    /**
     * Returns a FileLaceClient
     * @param name The name of the Queue to create.
     */
    public FileLaceClient getFileLaceClient() {
        return new FileLaceClient(this);
    }

    Reader put(String endpoint,String body) throws IOException{
	return request("PUT",endpoint,body);
    }

    Reader delete(String endpoint) throws IOException {
        return request("DELETE", endpoint, null);
    }

    InputStream get(String endpoint) throws IOException {
	return requestBinary("GET",endpoint,null);
    }

    Reader post(String endpoint, byte[] body,String fileName) throws IOException {
        return requestSend("POST", endpoint, body,fileName);
    }


    /**
     * 
     * @return InputStream containing requested file (plain file)
     */
    private InputStream requestBinary(String method, String endpoint,byte[] body) throws IOException {

        String path = initPath + apiVersion + "/Lace/" + endpoint;
        URL url = new URL(cloud.scheme, cloud.host, cloud.port, path);
	
        final int maxRetries = 5;
        int retries = 0;
        while (true) {
            try {
                return singleBinaryRequest(method, url, body);//body is byte[]
            } catch (HttpException e) {
                // ELB sometimes returns this when load is increasing.
                // We retry with exponential backoff.
                if (e.getStatusCode() != 503 || retries >= maxRetries) {
                    throw e;
                }
                retries++;
                // random delay between 0 and 4^tries*100 milliseconds
                int pow = (1 << (2*retries))*100;
                int delay = rand.nextInt(pow);
                try {//implement delay similar to Ethernet standard of exp-backoff
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    private InputStream singleBinaryRequest(String method, URL url,byte[] body) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "OAuth " + token);
        conn.setRequestProperty("User-Agent", "File LAce Java Client");

        if (body != null) {
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
        }

        conn.connect();

        if (body != null) {
	    DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.write(body);
	    out.flush();
            out.close();
        }

        int status = conn.getResponseCode();

        if (status != 200) {

            String msg;
            if (conn.getContentLength() > 0 && conn.getContentType().equals("application/json")) {

                InputStreamReader reader = null;
                try {
                    reader = new InputStreamReader(conn.getErrorStream());
                    Gson gson = new Gson();
                    Error error = gson.fromJson(reader, Error.class);
                    msg = error.msg;
                } catch (JsonSyntaxException e) {
                    msg = "Server's response Invalid: HTTP-STATUS="+status;
                } finally {
                    if (reader != null)
                        reader.close();
                }

            } else {
                msg = "0Server's response Invalid: HTTP-STATUS="+status;
            }
            throw new HttpException(status, msg);
        }

        return conn.getInputStream();
    }


    
    private Reader request(String method, String endpoint, String body) throws IOException {

        String path = initPath + apiVersion + "/Lace/" + endpoint;
        URL url = new URL(cloud.scheme, cloud.host, cloud.port, path);

        final int maxRetries = 5;
        int retries = 0;
        while (true) {
            try {
                return singleRequest(method, url, body);
            } catch (HttpException e) {
                if (e.getStatusCode() != 503 || retries >= maxRetries) {
                    throw e;
                }
                retries++;
                // random delay between 0 and 4^tries*100 milliseconds
                int pow = (1 << (2*retries))*100;
                int delay = rand.nextInt(pow);
                try {//implement delay similar to Ethernet standard of exp-backoff
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    private Reader singleRequest(String method, URL url, String body) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "OAuth " + token);
        conn.setRequestProperty("User-Agent", "File Lace Java Client");

        if (body != null) {
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
        }

        conn.connect();

        if (body != null) {
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(body);
	    out.flush();
            out.close();
        }

        int status = conn.getResponseCode();
        if (status != 200) {

            String msg;

            if (conn.getContentLength() > 0 && conn.getContentType().equals("application/json")) {

                InputStreamReader reader = null;
                try {
                    reader = new InputStreamReader(conn.getErrorStream());
                    Gson gson = new Gson();
                    Error error = gson.fromJson(reader, Error.class);
                    msg = error.msg;
                } catch (JsonSyntaxException e) {
                    msg = "Server's response contained invalid JSON";
                } finally {
                    if (reader != null)
                        reader.close();
                }

            } else {
                msg = "Empty or non-JSON response";
            }

            throw new HttpException(status, msg);
        }

        return new InputStreamReader(conn.getInputStream());
    }

    /**
     * requestSend - Used to pass a binary stream to server, with filename in the HttpHeader
     */
    private Reader requestSend(String method, String endpoint,byte[] body,String fileName) throws IOException {

        String path = initPath + apiVersion + "/Lace/" +  endpoint;
        URL url = new URL(cloud.scheme, cloud.host, cloud.port, path);

        final int maxRetries = 5;
        int retries = 0;
        while (true) {
            try {
                return singleSendRequest(method, url, body,fileName);
            } catch (HttpException e) {
                if (e.getStatusCode() != 503 || retries >= maxRetries) {
                    throw e;
                }
                retries++;
                // random delay between 0 and 4^tries*100 milliseconds
                int pow = (1 << (2*retries))*100;
                int delay = rand.nextInt(pow);
                try {//implement delay similar to Ethernet standard of exp-backoff
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    static private class Error implements Serializable {
        String msg;
    }


    /**
     *singleSendRequest - backend for requestSend()
     * @arg body - sent in HTTP payload
     * @arg fileName - name service will upload file to Drive
     * @return Text-Response from Server
     */
    private Reader singleSendRequest(String method, URL url,byte[] body,String fileName) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "OAuth " + token);
	conn.setRequestProperty("FileName",fileName);
        conn.setRequestProperty("User-Agent", "File Lace Java Client");

        if (body != null) {
            conn.setRequestProperty("Content-Type", "application/json");//wrong inet type
            conn.setDoOutput(true);
        }

        conn.connect();

        if (body != null) {
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.write(body,0,body.length);
	    out.flush();
            out.close();
        }

        int status = conn.getResponseCode();
        if (status != 200) {

            String msg;

            if (conn.getContentLength() > 0 && conn.getContentType().equals("application/json")) {

		System.out.println("ServerReplyError: ContentType="+conn.getContentType());

                InputStreamReader reader = null;
                try {
                    reader = new InputStreamReader(conn.getErrorStream());
                    Gson gson = new Gson();
                    Error error = gson.fromJson(reader, Error.class);
                    msg = error.msg;
                } catch (JsonSyntaxException e) {
                    msg = "Server's response contained invalid JSON";
                } finally {
                    if (reader != null)
                        reader.close();
                }

            } else {
                msg = "Empty or non-JSON response clientPost";
            }

            throw new HttpException(status, msg);
        }

        return new InputStreamReader(conn.getInputStream());
    }
}
