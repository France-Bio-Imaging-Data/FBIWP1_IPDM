/**
 * 
 */
package plugins.ofuica.metro.openimadis;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import icy.common.exception.UnsupportedFormatException;
import icy.image.IcyBufferedImage;
import icy.sequence.MetaDataUtil;
import icy.sequence.SequenceIdImporter;
import icy.type.DataType;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.ome.OMEXMLMetadataImpl;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;

/**
 * @author osvaldo
 */

public final class OpenImadisImporter implements SequenceIdImporter {
   
   private Client client_ = null;
   private Long id_ = -1L;
   
   private OpenImadisImporter(Client client) {
      super();
      this.client_ = client;
   }
   
   @Override
   public OMEXMLMetadataImpl getMetaData() throws UnsupportedFormatException, IOException {
      try {
         // TODO: If our image guid is not a valid id we could notify to user-land.
         OMEXMLMetadataImpl metaData = client_.getOriginalMetaData(id_);
         
         if ( metaData == null ) {
            metaData = client_.getMetaData(id_);
         }
         
         return metaData;
      } catch (NumberFormatException e) {
         throw new UnsupportedFormatException(e.getMessage());
      } catch (ClientException e) {
         throw new IOException(e.getMessage());
      }
   }
   
   @Override
   public int getTileWidth(int serie) throws UnsupportedFormatException, IOException {
      return 0;
   }
   
   @Override
   public int getTileHeight(int serie) throws UnsupportedFormatException, IOException {
      return 0;
   }
   
   @Override
   public IcyBufferedImage getThumbnail(int serie) throws UnsupportedFormatException, IOException {
      try {
         BufferedImage image = client_.getThumbnail(id_);
         return IcyBufferedImage.createFrom(image);
      } catch (ClientException e) {
         throw new UnsupportedFormatException(e.getMessage());
      }
   }
   
   @Override
   public Object getPixels(int serie, int resolution, Rectangle rectangle, int z, int t, int c)
         throws UnsupportedFormatException, IOException {
      
      try {
         return client_.downloadPlanar(id_, t, z, c, rectangle);
      } catch (ClientException e) {
         // TODO: implement a way to route the correct exception.
         throw new IOException(e.getMessage());
      }
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int resolution, Rectangle rectangle, int z, int t, int c)
         throws UnsupportedFormatException, IOException {
      
      final Object imageArray = getPixels(serie, resolution, rectangle, z, t, c);
      
      try {
         OMEXMLMetadata metaData = client_.getOriginalMetaData(id_);
         
         if ( metaData == null ) {
            metaData = client_.getMetaData(id_);
         }
         
         int sizeX = MetaDataUtil.getSizeX(metaData, serie);
         int sizeY = MetaDataUtil.getSizeY(metaData, serie);
         
         final DataType dataType = MetaDataUtil.getDataType(metaData, serie);
         final boolean unsigned = (dataType == DataType.UBYTE || dataType == DataType.USHORT || dataType == DataType.UINT
               || dataType == DataType.ULONG );
         final IcyBufferedImage result = new IcyBufferedImage(sizeX, sizeY, imageArray, !unsigned);
         return result;
         
      } catch (ClientException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int resolution, Rectangle rectangle, int z, int t)
         throws UnsupportedFormatException, IOException {
      
      int sizeC = MetaDataUtil.getSizeC( (OMEXMLMetadata) getMetaData(), serie);
      final List<IcyBufferedImage> images = new ArrayList<>();
      
      for (int c = 0 ; c < sizeC ; c++ ) {
         images.add(getImage(serie, resolution, rectangle, z, t, c));
      }
      
      return IcyBufferedImage.createFrom(images);
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int resolution, int z, int t, int c)
         throws UnsupportedFormatException, IOException {
      
      return getImage(serie, resolution, null, z, t, c);
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int resolution, int z, int t)
         throws UnsupportedFormatException, IOException {
      return getImage(serie, resolution, null, z, t, 0);
   }
   
   @Override
   public IcyBufferedImage getImage(int serie, int z, int t) throws UnsupportedFormatException, IOException {
      return getImage(serie, 0, null, z, t);
   }
   
   @Override
   public IcyBufferedImage getImage(int z, int t) throws UnsupportedFormatException, IOException {
      return getImage(0, 0, null, z, t);
   }
   
   @Override
   public String getOpened() {
      return "" + id_;
   }
   
   @Override
   public boolean open(String id, int flags) throws UnsupportedFormatException, IOException {
      id_ = Long.parseLong(id);
      return true;
   }
   
   @Override
   public void close() throws IOException {
   }
   
   public String toString() {
      return "OpenImadisImporter";
   }
   
   public static OpenImadisImporter create(Client value) {
      return new OpenImadisImporter(value);
   }
   
   @Override
   public ome.xml.meta.OMEXMLMetadata getOMEXMLMetaData() throws UnsupportedFormatException, IOException {
      try {
         
         // TODO: If our image guid is not a valid id we could notify to user-land.
         OMEXMLMetadataImpl metaData = client_.getOriginalMetaData(id_);
         
         if ( metaData == null ) {
            metaData = client_.getMetaData(id_);
         }
         
         return metaData;
      } catch (NumberFormatException e) {
         throw new UnsupportedFormatException(e.getMessage());
      } catch (ClientException e) {
         throw new IOException(e.getMessage());
      }
   }
}
