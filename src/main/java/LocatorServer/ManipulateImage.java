package LocatorServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.codec.binary.Base64;


public class ManipulateImage {

    public static String convertStringtoImage(String encodedImageStr, String path,String fileName) {

        String fullPath = "";

        try {

            // Decode String using Base64 Class

            byte[] imageByteArray = Base64.decodeBase64(encodedImageStr);



            // Write Image into File system - Make sure you update the path

            File dir = new File(path);
            if(dir.exists() == false)
                dir.mkdir();

             fullPath = dir+"//" + fileName;
            FileOutputStream imageOutFile = new FileOutputStream(fullPath);

            imageOutFile.write(imageByteArray);



            imageOutFile.close();



            System.out.println("Image Successfully Stored");

        } catch (FileNotFoundException fnfe) {

            System.out.println("Image Path not found" + fnfe);

        } catch (IOException ioe) {

            System.out.println("Exception while converting the Image " + ioe);

        }

        return fullPath;
    }

}
