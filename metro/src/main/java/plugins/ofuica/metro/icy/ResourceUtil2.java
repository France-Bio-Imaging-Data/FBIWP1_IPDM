/**
 * 
 */
package plugins.ofuica.metro.icy;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import icy.file.FileUtil;

/**
 * @author osvaldo
 */

public class ResourceUtil2 {
   
   /**
    * Brute forced resource's listing. Even it returns a list of files inside your jar!.
    * 
    * @param clazz : Plugin's class.
    * @param path : resource path.
    * @return
    * @throws URISyntaxException
    * @throws IOException
    */
   public static List<String> getResourceListing(Class clazz, String path, String extension) throws URISyntaxException, IOException {
      URL dirURL = clazz.getClassLoader().getResource(path);
      final List<String> result = new ArrayList<>();
      
      if (dirURL != null && dirURL.getProtocol().equals("file")) {
         final List<String> files = Arrays.asList(FileUtil.getFiles(dirURL.getFile(), new FileFilter() {
            @Override
            public boolean accept(File pathname) {
               return FileUtil.getFileExtension(pathname.getAbsolutePath(), false).equals("protocol");
            }}, false, false, false)
         );
         
         for (String file : files ) {
            result.add(file.substring(file.indexOf("protocols/")));
         }
         
         return result;
      }
      
      if (dirURL == null) {
         String me = clazz.getName().replace(".", "/")+".class";
         dirURL = clazz.getClassLoader().getResource(me);
      }
      
      if (dirURL.getProtocol().equals("jar")) {
         final String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
         final JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
         final Enumeration<JarEntry> entries = jar.entries();
         
         while(entries.hasMoreElements()) {
            final String name = entries.nextElement().getName();
            if (name.startsWith(path) && name.endsWith(extension)) {
               String entry = name.substring(path.length());
               int checkSubdir = entry.indexOf("/");
               
               if (checkSubdir >= 0) {
                  entry = entry.substring(0, checkSubdir);
               }
               
               result.add(path+entry);
            }
         }
         
         jar.close();
         return result;
      }
      
      throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
   }
}
