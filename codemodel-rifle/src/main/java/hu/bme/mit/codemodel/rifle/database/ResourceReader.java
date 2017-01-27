package hu.bme.mit.codemodel.rifle.database;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Created by steindani on 3/3/16.
 */
public class ResourceReader {

    public static String readFromResource(String fileName) {
        InputStream resourceAsStream = ResourceReader.class.getClassLoader().getResourceAsStream(fileName);
        try {
            return IOUtils.toString(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String query(String queryName) {
        return readFromResource("queries" + File.separator + queryName + ".cypher");
    }
}
