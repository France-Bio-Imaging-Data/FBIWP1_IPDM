/**
 * 
 */
package plugins.ofuica.metro.openimadis;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import icy.common.exception.UnsupportedFormatException;
import icy.image.IcyBufferedImage;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;
import plugins.ofuica.metro.openimadis.OpenImadisClient;
import plugins.ofuica.metro.openimadis.OpenImadisImporter;

/**
 * OpenImadisImporter test.
 * 
 * @author osvaldo
 */

public class OpenImadisImporterTest {
   
   @Before
   public void setUp() throws Exception {
   }
   
   @Test
   public void testOpenImage () {
      
      Client client = new OpenImadisClient();
      try {
         if ( !client.isConnected() ) {
            boolean connected = client.login("cid.curie.fr:443", "auxJGBeKNIAJoGYr5pNtwRnI5jkXUyxUhDrhtpkf");
            
            assertTrue(connected);
            final OpenImadisImporter importer = OpenImadisImporter.create(client);
            assertNotNull(importer);
            
            try {
               boolean open = importer.open("11845", 0);
               assertTrue(open);
               
               if ( open ) {
                  IcyBufferedImage image = importer.getImage(0, 0, null, 0, 0, 0);
                  
                  // TODO: check image properties if you have certainty in that!.
                  assertNotNull(image);
                  assertTrue(image.getWidth() > 0);
                  assertTrue(image.getHeight() > 0);
                  importer.close();
               }
               
               
               client.logout();
               assertTrue(true);
               
            } catch (UnsupportedFormatException e) {
               assertTrue(false);
               e.printStackTrace();
            } catch (IOException e) {
               assertTrue(false);
               e.printStackTrace();
            }
         }
      } catch (ClientException e) {
         assertTrue(false);
         e.printStackTrace();
      }
   }
}
