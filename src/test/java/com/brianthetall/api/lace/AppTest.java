package com.brianthetall.api.lace;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.io.*;
/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
	FileLaceClient flc = new Client("DEADbeefCAFE").getFileLaceClient();
	FileBean hotchick = flc.upload(new File("hotchick.bmp"));//UPLOAD TO SERVER

	if(hotchick!=null){
	    assertTrue( hotchick.getName().equals("hotchick.bmp") );
	    System.out.println("FileBean from Server.toString()="+hotchick.toString());

	    byte[] hotchickBytes = flc.download(new FileBean().setName(hotchick.getName()).setId(hotchick.getId()));//DOWNLOAD FROM SERVER

	    System.out.println("From Server byte[] size="+hotchickBytes.length);
	    /*
	    try{
		FileOutputStream fos=new FileOutputStream(new File(hotchick.getName()));
		fos.write(hotchickBytes,0,hotchickBytes.length);
		fos.flush();
		fos.close();
	    }catch(FileNotFoundException e){
		System.out.println("Error in Test FileNotFound "+e.getMessage());
	    }catch(IOException e){
		System.out.println("Error in Test IOException "+e.getMessage());
	    }
	    */
	    {//TEST DELETE		
		FileBean deletedFileBean = flc.delete(hotchick);//DELETE the file from the SERVER
		System.out.println("Deleted File Bean="+deletedFileBean.toString());
		assertTrue( deletedFileBean.getSize() == 469554 );
	    }
	    
	    {//TEST LACE
		File big=new File("hotchick.bmp");
		File small=new File("smallTextFile");
		FileBean carrier=flc.upload(big);
		FileBean payload=flc.upload(small);
		FileBean laced = flc.lace(payload,carrier);
		FileBean carrierDeleted = flc.delete(carrier);
		
		System.out.println(laced.toString());
		//		assertTrue(laced.getPayloadSize() == small.length());
		assertTrue(laced.getSize() == big.length());

		{//TEST UNLACE
		    FileBean payloadDeleted = flc.delete(payload);
		    FileBean unlaced = flc.unlace(laced);
		    System.out.println(unlaced.toString());
		    byte[] recovered = flc.download(unlaced);
		    try{
			FileOutputStream fos=new FileOutputStream("smallTextFileRecovered");
			fos.write(recovered,0,recovered.length);
			fos.flush();
			fos.close();
		    }catch(FileNotFoundException e){
			System.out.println("ERROR: App Test FileNotFound:"+e.getMessage());
		    }catch(IOException e){
			System.out.println("ERROR: App Tets:"+e.getMessage());
		    }

		    assertTrue( unlaced.getName().equals(payload.getName())  );
		    assertTrue( unlaced.getSize() == small.length() );
		    
		    flc.delete(unlaced);
		    flc.delete(laced);
		}
		
	    }


	    
	}
	
        assertTrue( true );
    }
}
