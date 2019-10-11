/**
 * 
 */
package plugins.ofuica.metro.omero;

import java.awt.Rectangle;
import java.io.IOException;

import icy.common.exception.UnsupportedFormatException;
import icy.image.IcyBufferedImage;
import icy.sequence.MetaDataUtil;
import icy.sequence.SequenceIdImporter;
import icy.type.DataType;
import loci.formats.ome.OMEXMLMetadataImpl;
import ome.xml.meta.OMEXMLMetadata;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;

/**
 * This class implements SequenceIdImporter interface.
 * Allows import an image or rectangle image with its metadata from omero server.
 * 
 * @author osvaldo
 */

public class OmeroImporter implements SequenceIdImporter {
   public static final int TILE_WIDTH = 512;
   public static final int TILE_HEIGHT = 512;
   
   protected final Client client_;
   protected Long imageId_ = -1L;
   
   protected OmeroImporter(final Client client) {
      this.client_ = client;
   }
   
   protected DataType getDataType() {
      try {
         return client_.getDataType(imageId_);
      } catch (ClientException e) {
         e.printStackTrace();
         return DataType.BYTE;
      }
   }
   
   @Override
   public OMEXMLMetadataImpl getMetaData() throws UnsupportedFormatException, IOException {
      
      if ( imageId_ != -1L ) {
         try {
            return client_.getMetaData(imageId_);
         } catch (ClientException e) {
            throw new UnsupportedFormatException(e.getMessage());
         }
      }
      
      throw new IOException("Un jour je suis sorti de ma maison.");
   }
   
   @Override
   public int getTileWidth(int serie) throws UnsupportedFormatException, IOException {
      return TILE_WIDTH;
   }
   
   @Override
   public int getTileHeight(int serie) throws UnsupportedFormatException, IOException {
      return TILE_HEIGHT;
   }
   
   @Override
   public IcyBufferedImage getThumbnail(int serie) throws UnsupportedFormatException, IOException {
      return null;
      // return IcyBufferedImage.createFrom(client_.getBirdEyeViewImage(imageInstance_.getId(), 
      //      TILE_WIDTH, TILE_HEIGHT));
   }
   
   @Override
   public Object getPixels(int serie, int resolution, Rectangle rectangle, int z, int t, int c)
         throws UnsupportedFormatException, IOException {
      
      try {
         return client_.downloadPlanar(imageId_, t, z, c, rectangle);
      } catch (ClientException e) {
         throw new IOException(e.getMessage());
      }
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int resolution, Rectangle rectangle, int z, int t, int c)
         throws UnsupportedFormatException, IOException {
      
      OMEXMLMetadataImpl metadata = null;
      try {
         metadata = client_.getMetaData(imageId_);
      } catch (ClientException e) {
         throw new IOException(e.getMessage());
      }
      
      final int sizeC = MetaDataUtil.getSizeC(metadata, serie);
      final IcyBufferedImage icyBufferedImage = new IcyBufferedImage(
            (int) rectangle.getWidth(), (int) rectangle.getHeight(), sizeC,
            getDataType());
      
      icyBufferedImage.beginUpdate();
      switch(getDataType()) {
         case BYTE:
         case UBYTE:
            icyBufferedImage.setDataXY(c, getPixels(serie, resolution, rectangle, z, t, c));
            // return IcyBufferedImageUtil.extractChannel(bufferedImage, c); TODO: Think about it!.
            break;
            
         case SHORT:
         case USHORT:
            icyBufferedImage.setDataXYAsShort(c, (short[]) getPixels(serie, resolution, rectangle, z, t, c));
            break;
         case FLOAT:
            icyBufferedImage.setDataXYAsFloat(c, (float[]) getPixels(serie, resolution, rectangle, z, t, c));
            break;
         case DOUBLE:
            icyBufferedImage.setDataXYAsDouble(c, (double[]) getPixels(serie, resolution, rectangle, z, t, c));
            break;
            
         default:
            icyBufferedImage.endUpdate();
            throw new UnsupportedFormatException("not imeplemented!");
      }
      
      icyBufferedImage.endUpdate();
      return icyBufferedImage;
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int resolution, Rectangle rectangle, int z, int t) {
      
      OMEXMLMetadataImpl metadata = null;
      try {
         metadata = client_.getMetaData(imageId_);
      } catch (ClientException e) {
         e.printStackTrace();
      }
      
      final int sizeC = MetaDataUtil.getSizeC(metadata, serie);
      
      final IcyBufferedImage icyBufferedImage = new IcyBufferedImage(
            (int) rectangle.getWidth(), (int) rectangle.getHeight(), sizeC,
            getDataType());
      
      icyBufferedImage.beginUpdate();
      
      try {
         for ( int c = 0 ; c < sizeC ; c++ ) {
            final Object buf = getPixels(serie, resolution, rectangle, z, t, c);
            
            // TODO: please put this in only one place!... is annoying adding data-type support in more that one place.!
            if (buf != null) {
               switch(getDataType()) {
                  case BYTE:
                  case UBYTE:
                     icyBufferedImage.setDataXY(c, buf);
                     break;
                  case SHORT:
                  case USHORT:
                     icyBufferedImage.setDataXYAsShort(c, (short[]) buf);
                     break;
                  case FLOAT:
                  case DOUBLE:
                     icyBufferedImage.setDataXYAsFloat(c, (float[]) buf);
                     break;
                  default:
                     throw new UnsupportedFormatException("not imeplemented!");
               }
            }
         }
      } catch (UnsupportedFormatException | IOException e) {
         // e.printStackTrace(); ignored error!
      } finally {
         icyBufferedImage.endUpdate();
      }
      
      return icyBufferedImage;
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int resolution, int z, int t, int c)
         throws UnsupportedFormatException, IOException {
      
      OMEXMLMetadataImpl metaData = null;
      try {
         metaData = client_.getMetaData(imageId_);
      } catch (ClientException e) {
         throw new IOException(e.getMessage());
      }
      
      final int sizeX = MetaDataUtil.getSizeX(metaData, serie);
      final int sizeY = MetaDataUtil.getSizeY(metaData, serie);
      final Rectangle rectangle = new Rectangle(0, 0, sizeX, sizeY);
      return getImage(serie, resolution, rectangle, z, t, c);
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int resolution, int z, int t)
         throws UnsupportedFormatException, IOException {
      OMEXMLMetadataImpl metaData = null;
      try {
         metaData = client_.getMetaData(imageId_);
      } catch (ClientException e) {
         throw new IOException(e.getMessage());
      }
      
      final int sizeX = MetaDataUtil.getSizeX(metaData, serie);
      final int sizeY = MetaDataUtil.getSizeY(metaData, serie);
      final Rectangle rectangle = new Rectangle(0, 0, sizeX, sizeY);
      return getImage(serie, resolution, rectangle, z, t);
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int z, int t) throws UnsupportedFormatException, IOException {
      return getImage(serie, 0, z, t); // 0 means the best resolution ?
   }
   
   @Override
   public IcyBufferedImage getImage(int z, int t) throws UnsupportedFormatException, IOException {
      return getImage(0, z, t);
   }
   
   @Override
   public String getOpened() {
      if ( imageId_ == -1L ) {
         return "";
      }
      
      return "" + imageId_ ;
   }
   
   @Override
   public boolean open(String id, int flags) throws UnsupportedFormatException, IOException {
      try {
         imageId_ = Long.parseLong(id);
         return true;
      } catch (final NumberFormatException e) {
         return false;
      }
   }
   
   @Override
   public void close() throws IOException {
      imageId_ = -1L;
   }
   
   public static OmeroImporter create(Client client) {
      return new OmeroImporter(client);
   }
   
   public String toString() {
      return getOpened();
   }
   
   @Override
   public OMEXMLMetadata getOMEXMLMetaData() throws UnsupportedFormatException, IOException {
      try {
         return (OMEXMLMetadata) client_.getMetaData(imageId_);
      } catch (ClientException e) {
         throw new UnsupportedFormatException(e.getMessage());
      }
   }
}
