package com.brianthetall.api.lace;

import java.io.Reader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonToken;
import java.lang.InterruptedException;

/**
   Contains the hooks for interfacing with btt's file-lace app
 */
public class FileLaceClient{
    
    private Client client;

    /**
       @param Client - the Client class builds this Class
    */
    FileLaceClient(Client c){
	if(c!=null){
	    client = c;
	}
	else
	    System.err.println("Null Client Passed");
    }

    /**
     * Download File: Send bean to server, get byte[] containing file
     * @param fileID-Used to ID the file with GDrive
     * @return byte[] containing the downloaded file (if successful)
     */
    public byte[] download(FileBean bean){

	try{
	    InputStream is = client.get("laced?id="+bean.getId()+"&name="+bean.getName());
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] buffer = new byte[1024];

	    int countBytes=0;
	    int bytesRead=0;

	    while((bytesRead=is.read(buffer,0,buffer.length)) != -1){
		
		countBytes+=bytesRead;
		baos.write(buffer,0,bytesRead);
		baos.flush();
	    }
	    
	    byte[] result = baos.toByteArray();

	    return result;

	}catch(IOException e){
	    System.err.println("FileLaceClient.download Error:"+e.getMessage());
	}
	return null;
    }

    /**
     * Upload file to service
     * @param payload - java.io.File to read and upload to service
     * @return FileBean describing the file that exists on the server
     */
    public FileBean upload(File payload){

	int fileSize=0;

	if(payload==null)
	    return null;

	byte[] sendFile = readFile(payload);

	Reader serverResponse=null;
	long fileID=0;

	try{
	    serverResponse = client.post("laced",sendFile,payload.getName());
	}catch(IOException e){
	    System.err.println("ERROR: FileLaceClientRX: "+e.getMessage());
	}
	
	Gson gson = new Gson();
	String beanSeed=null;
	if(serverResponse!=null){
	    try{
		BufferedReader br = new BufferedReader(serverResponse);
		beanSeed = br.readLine();
	    }catch(Exception e){
		System.out.println("errr."+e.getMessage());
	    }
	}

	return gson.fromJson(beanSeed,FileBean.class);
    }

    /**
     * Delete a File from Lace service
     * @param bean - FileBean describing server-side file to delete
     * @return FileBean sent from server confirming the file's deletion; Returned bean has FileSize field filled in!
     */
    public FileBean delete(FileBean bean){

	if(bean==null)
	    return null;

	Gson gson=new Gson();

	try{
	    Reader response = client.delete("laced?id="+bean.getId()+"&name="+bean.getName());
	    BufferedReader br=new BufferedReader(response);
	    String rxBean=br.readLine();	
	    return gson.fromJson(rxBean,FileBean.class);
	}catch(IOException e){
	    System.out.println("ERROR: FileLaceClient.delete() "+e.getMessage());
	}
	return null;
    }

    /**
     * lace two files to generate a new file
     * @param payload - server side file previously uploaded
     * @param carrier - server side file previously uploaded
     * @return FileBean - describing new SS file
     */
    public FileBean lace(FileBean payload,FileBean carrier){

	if(payload==null || carrier==null)
	    return null;
	
	if(payload.getName()==null||carrier.getName()==null||payload.getId()==0||carrier.getId()==0)
	    return null;

	FileBean[] beans=new FileBean[2];
	beans[0]=payload;
	beans[1]=carrier;

	Gson gson=new Gson();
	String toSend=gson.toJson(beans);
	Reader response=null;
	try{
	    response=client.put("laced?unlace=false",toSend);
	}catch(IOException e){
	    System.out.println("ERROR: FLC.lace: "+e.getMessage());
	}
	BufferedReader br=new BufferedReader(response);
	try{
	    FileBean retval=gson.fromJson(br.readLine(),FileBean.class);
	    return retval;
	}catch(IOException e){
	    System.out.println("ERRoR");
	}
	return null;
    }

    /**
     * unlace - extract a file and return a bean describing it
     * @param carrier - FileBean describing SS file containing a payload file
     * @return FileBean
     */
    public FileBean unlace(FileBean carrier){

	if(carrier==null || carrier.getName()==null || carrier.getId()==0)
	    return null;

	Gson gson=new Gson();
	Reader response=null;
	try{
	    response=client.put("laced?unlace=true",gson.toJson(carrier));
	}catch(IOException e){
	    System.out.println("ERROR: FLC.lace: "+e.getMessage());
	}

	BufferedReader br=new BufferedReader(response);
	try{
	    FileBean retval=gson.fromJson(br.readLine(),FileBean.class);
	    return retval;
	}catch(IOException e){
	    System.out.println("ERRoR");
	}
	return null;
	

    }

    /**
     * readFile - read a File into, and return a byte[]
     * @param file - input file to read into byte[]
     * @return byte[] containing file data
     */
    private byte[] readFile(File file){

        if(file==null)
            return null;

        byte[] retval=new byte[(int)file.length()];
        try{
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            dis.readFully(retval,0,retval.length);
            dis.close();
        }catch(FileNotFoundException e){
            System.out.println("Error Util.readFile FileNotFound" + e.getMessage());
        }catch(IOException e){
            System.out.println("Error Util.readFile IOE" + e.getMessage());
        }

        return retval;
    }

}
