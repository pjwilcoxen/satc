//  Util.java
//
//  General purpose utility routines
//

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class Util {

    //
    //  openRead
    //     Open a file and catch exceptions
    //

    public static FileReader openRead(String name) {
        FileReader fr ;
        try {
            fr = new FileReader(name); 
            return fr;
            } 
        catch (IOException ex) {
            System.out.println("Error reading: "+name);
            System.exit(0);
        } 
        return null ;
    }

    // 
    //  openWrite
    //     Open a file for writing and catch exceptions
    //

    public static PrintWriter openWrite(String name) {
        PrintWriter pw;
        try {
            pw = new PrintWriter(name); 
            return pw;
            } 
        catch (IOException ex) {
            System.out.println("Error opening "+name+" for writing");
            System.exit(0);
        } 
        return null ;
    }
}
