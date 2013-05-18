package com.brianthetall.api.lace;

import java.io.*;

public class FileBean implements Serializable{

    private String fileName;
    private byte[] hash;
    private long size;
    private long id;
    private FileBean payload;

    public FileBean(){}
    public String getName(){return fileName;}
    public FileBean setName(String name){fileName=name; return this;}

    public long getSize(){return size;}
    public FileBean setSize(long size){this.size=size; return this;}

    public long getId(){return id;}
    public FileBean setId(long id){this.id=id; return this;}

    public FileBean getPayloadBean(){ return payload; }
    public FileBean setPayloadBean(FileBean bean){ payload=bean; return this;}

    /**
     * @return first 8 bytes of hash; -1 on Error
     */
    public long truncHash() throws IOException{
	if(hash!=null && hash.length>=8){
	    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(hash));
	    return dis.readLong();
	}
	return -1;
    }

    /**
     * @param hash - the hash of this file to store in bean
     * @return false if input is too small
     */
    public FileBean setHash(byte[] hash){
	if(hash.length<8)
	    return null;
	this.hash=hash;
	return this;
    }

    public byte[] getHash(){
	return hash;
    }

    @Override public String toString(){
	return payload==null ? "Name:"+fileName+" Size:"+size+" ID:"+id : "Name:"+fileName+" Size:"+size+" ID:"+id+" PayloadBean: "+payload.toString();
    }

}
