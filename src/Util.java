import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility routines
 */
public class Util {

    /**
     * Open a file and catch exceptions
     * 
     * @param  name File name to open for reading
     * @return Open FileReader
     */
    public static FileReader openRead(String name) {
        FileReader fr ;
        try {
            fr = new FileReader(name); 
            return fr;
            } 
        catch (IOException ex) {
            throw new RuntimeException("Error reading: "+name);
        } 
    }

    /** 
     * Open a file for writing and catch exceptions
     * 
     * @param  name Filename to open for writing
     * @return Open PrintWriter
     */
    public static PrintWriter openWrite(String name) {
        PrintWriter pw;
        try {
            pw = new PrintWriter(name); 
            return pw;
            } 
        catch (IOException ex) {
            throw new RuntimeException("Error opening "+name+" for writing");
        } 
    }
}
