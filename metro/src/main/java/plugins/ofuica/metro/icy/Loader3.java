/**
 * 
 */
package plugins.ofuica.metro.icy;

import java.awt.Rectangle;
import java.io.IOException;

import icy.common.exception.UnsupportedFormatException;
import icy.file.FileUtil;
import icy.gui.frame.progress.CancelableProgressFrame;
import icy.main.Icy;
import icy.sequence.MetaDataUtil;
import icy.sequence.Sequence;
import icy.sequence.SequenceIdImporter;
import loci.formats.ome.OMEXMLMetadataImpl;
import ome.xml.meta.OMEXMLMetadata;
import plugins.adufour.ezplug.EzStatus;

/**
 * @author osvaldo
 */

public class Loader3 {
   
   public static void load(final SequenceIdImporter importer, int serie, final CancelableProgressFrame progress)
    throws UnsupportedFormatException, IOException {
      
      final OMEXMLMetadataImpl metaData = importer.getMetaData();
      
      if ( ! (metaData instanceof OMEXMLMetadata) )
         throw new UnsupportedFormatException("I could not cast OMEXMLMetadataImpl to OMEXMLMetadata!");
      
      int sizeT = MetaDataUtil.getSizeT( (OMEXMLMetadata) metaData, serie);
      int sizeZ = MetaDataUtil.getSizeZ( (OMEXMLMetadata) metaData, serie);
      
      final Sequence sequence = new Sequence();
      boolean cancelled = false;
      
      if ( progress != null ) {
         progress.setLength(sizeT*sizeZ);
      }
      
      for ( int t = 0 ; t < sizeT; t++ ) {
         for ( int z = 0 ; z < sizeZ ; z++ ) {
            if ( progress != null ) {
                
               if (progress.isCancelRequested()) {
                  cancelled = true;
                  break;
               }
               
               progress.incPosition();
            }
            
            sequence.setImage(t, z, importer.getImage(serie, 0, z, t));
         }
      }
      
      if ( !cancelled ) {
         sequence.setMetaData(metaData);
         sequence.setName(MetaDataUtil.getName( (OMEXMLMetadata) metaData, serie));
         Icy.getMainInterface().addSequence(sequence);
         progress.close();
      }
   }
   
   public static Sequence loadSequence(final SequenceIdImporter importer, int serie) throws 
      UnsupportedFormatException, IOException {
      return loadSequence(importer, serie, null, null);
   }
   
   public static Sequence loadSequence(final SequenceIdImporter importer) throws
      UnsupportedFormatException, IOException {
      return loadSequence(importer, 0, null, null);
   }
   
   public static Sequence loadSequence(final SequenceIdImporter importer, int serie, Rectangle rectangle) throws
   UnsupportedFormatException, IOException {
      return loadSequence(importer, serie, null, rectangle);
   }
   
   public static Sequence loadSequence(final SequenceIdImporter importer, int serie, EzStatus status, Rectangle rectangle)
         throws UnsupportedFormatException, IOException {
           
      final OMEXMLMetadata metaData = importer.getMetaData();
      int sizeT = MetaDataUtil.getSizeT(metaData, serie);
      int sizeZ = MetaDataUtil.getSizeZ(metaData, serie);
      
      // TODO: We can improve this measure :)
      double total = sizeT*sizeZ;
      
      final Sequence sequence = new Sequence();
      
      for ( int t = 0 ; t < sizeT; t++ ) {
         for ( int z = 0 ; z < sizeZ ; z++ ) {
            
            if ( rectangle != null )
               sequence.setImage(t, z, importer.getImage(serie, 0, rectangle, z, t));
            else
               sequence.setImage(t, z, importer.getImage(serie, 0, z, t));
            
            if ( status != null) {
               if ( total > 0 ) {
                  status.setCompletion( (t+1)*(z+1)/total);
               }
            }
         }
      }
      
      if ( metaData != null ) {
         if ( rectangle == null ) {
            sequence.setMetaData((OMEXMLMetadataImpl) metaData);
            sequence.setName(MetaDataUtil.getName(metaData, serie));
         }
         
         else {
            // MetaDataUtil.setSizeX(metaData, serie, rectangle.width);
            // MetaDataUtil.setSizeY(metaData, serie, rectangle.height);
            final String fileName = MetaDataUtil.getName(metaData, serie);
            final String name = FileUtil.getFileName(fileName, false);
            sequence.setMetaData((OMEXMLMetadataImpl) metaData);
            sequence.setName(name + "-" + rectangle.x + "-" + rectangle.y + "-" + rectangle.width + "-"+rectangle.height);
         }
      }
      
      return sequence;
   }
}
