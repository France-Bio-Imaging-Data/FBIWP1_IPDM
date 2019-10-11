/**
 * 
 */
package plugins.ofuica.metro.omero;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.ArrayUtils;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import icy.file.FileUtil;
import icy.file.Saver;
import icy.roi.ROI;
import icy.sequence.MetaDataUtil;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.util.OMEUtil;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.ome.OMEXMLMetadataImpl;
import ome.model.units.BigResult;
import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.model.AnnotationData;
import omero.gateway.model.ImageData;
import omero.gateway.model.MapAnnotationData;
import omero.gateway.model.PixelsData;
import omero.gateway.model.ProjectData;
import omero.model.NamedValue;
import omero.model.enums.UnitsLength;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;
import plugins.ofuica.metro.client.SearchCondition;

/**
 * Omero client implementation.
 * We could use Omercy plugin code + github project.
 * 
 * @author osvaldo
 */

public class OmeroClient implements Client {
   
   private static final int NOT_USED = 3;
   
   private OMEClient omeClient_ = new OMEClient();
   
   @Override
   public boolean login(boolean ssl, String host, int port, String[] accessKey) throws ClientException {
      
      if ( accessKey.length != 2 ) {
         throw new ClientException("Ops.. accessKey array is expecting two elements.");
      }
      
      try {
         omeClient_.connect(host, port, accessKey[0], accessKey[1]);
         return true;
      } catch (OMEClientException e) {
         throw new ClientException(e.getMessage());
      }
   }
   
   @Override
   public boolean login(boolean ssl, String host, int port, String appId, String[] code) throws ClientException {
      throw new ClientException("login with SSL is not implemented");
   }
   
   @Override
   public boolean login(String url, String appId, String[] code) throws ClientException {
      return login(url, code);
   }
   
   @Override
   public boolean login(String url, String[] accessKey) throws ClientException {
      final String[] arr = url.split(":");
      
      if ( arr.length != 2 )
         throw new ClientException("Login input url invalid.");
      try {
         omeClient_.connect(arr[0], Integer.parseInt(arr[1]), accessKey[0], accessKey[1]);
         return true;
         
      } catch (NumberFormatException e) {
         throw new ClientException(e.getMessage());
      } catch (OMEClientException e) {
         throw new ClientException(e.getMessage());
      }
   }
   
   @Override
   public void logout() throws ClientException {
      omeClient_.disconnect();
   }
   
   @Override
   public boolean isConnected() throws ClientException {
      return omeClient_.isConnected();
   }
   
   @Override
   public Object downloadPlanar(Long id, int t, int z, int c, Rectangle rectangle) throws ClientException {
      
      try {
         final ImageData imageData = omeClient_.getImageData(id);
         final PixelsData pixels = imageData.getDefaultPixels();
         final int sizeX = pixels.getSizeX();
         final int sizeY = pixels.getSizeY();
         final int sizeZ = pixels.getSizeZ();
         final int sizeT = pixels.getSizeT();
         final int sizeC = pixels.getSizeC();
         
         if ( z > sizeZ || t > sizeT || c > sizeC || 
               rectangle.getX() + rectangle.getWidth() > sizeX || 
               rectangle.getY() + rectangle.getHeight() >  sizeY ) {
            throw new ClientException("invalid parameters");
         }
         
         try {
            RawPixelsStorePrx store = omeClient_.getStore();
            
            if ( store != null ) {
               store.setPixelsId(pixels.getId(), false);
               final int byteWidth = store.getByteWidth();
               
               //TODO: implement all the rest formats.
               switch(byteWidth) {
                  case Byte.BYTES:
                  {
                     Object ret = store.getTile(z, c, t, (int) rectangle.getX(),
                           (int) rectangle.getY(), (int) rectangle.getWidth(),
                           (int) rectangle.getHeight());
                     store.close();
                     return ret;
                  }
                  
                  case Short.BYTES:
                  {
                     final byte buff[] = store.getTile(z, c, t, 
                           (int) rectangle.getX(), (int) rectangle.getY(),
                           (int) rectangle.getWidth(), (int) rectangle.getHeight());
                     final short[] shorts = new short[buff.length/2];
                     ByteBuffer.wrap(buff).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get(shorts);
                     store.close();
                     return shorts;
                  }
                  
                  case NOT_USED:
                     store.close();
                     break;
                  case Integer.BYTES:
                  {
                     // TODO: There is or not any difference using Float or Double. If yes. 
                     // Please detect and manage particular case.
                     final byte buff[] = store.getTile(z, c, t,
                           (int) rectangle.getX(), (int) rectangle.getY(), 
                           (int) rectangle.getWidth(), (int) rectangle.getHeight());
                     
                     final float[] floats = new float[buff.length/4];
                     ByteBuffer.wrap(buff).asFloatBuffer().get(floats);
                     store.close();
                     return floats;
                  }
               }
               
               store.close();
            }
         } catch (OMEClientException e) {
            throw new ClientException(e.getMessage());
         }
         
      } catch(final NoSuchElementException ee ) {
         System.out.println("image not found");
      } catch(final ServerError e1) {
         //e1.printStackTrace(); ignored error!.
      }
      
      return null;
   }
   
   @Override
   public OMEXMLMetadataImpl getMetaData(Long id) throws ClientException {
      OMEXMLMetadata meta = omeClient_.getImageMetadata(id);
      if ( meta == null ) {
         final OMEXMLMetadata metaImpl = OMEUtil.createOMEMetadata();
         final ImageData imageData = omeClient_.getImageData(id);
         PixelsData pxData = imageData.getDefaultPixels();
         // TODO: Please fix multiples series support!.
         // TODO: Please add all parameters that I missed.
         MetaDataUtil.setSizeC(metaImpl, 0, pxData.getSizeC());
         MetaDataUtil.setSizeT(metaImpl, 0, pxData.getSizeT());
         MetaDataUtil.setSizeZ(metaImpl, 0, pxData.getSizeZ());
         MetaDataUtil.setSizeX(metaImpl, 0, pxData.getSizeX());
         MetaDataUtil.setSizeY(metaImpl, 0, pxData.getSizeY());
         
         try {
            MetaDataUtil.setPixelSizeX(metaImpl, 0, pxData.getPixelSizeX(UnitsLength.PIXEL).getValue());
            MetaDataUtil.setPixelSizeY(metaImpl, 0, pxData.getPixelSizeY(UnitsLength.PIXEL).getValue());
            MetaDataUtil.setPixelSizeZ(metaImpl, 0, pxData.getPixelSizeZ(UnitsLength.PIXEL).getValue());
         } catch (BigResult e) {
            e.printStackTrace();
         }
         
         return (OMEXMLMetadataImpl) metaImpl;
      }
      
      return OMEUtil.createOMEMetadata(meta);
   }
   
   @Override
   public DataType getDataType(Long id) throws ClientException {
      
      final ImageData imageData = omeClient_.getImageData(id);
      final PixelsData pixelData =imageData.getDefaultPixels();
      
      switch(pixelData.getPixelType()) {
         case "int8": return DataType.BYTE;
         case "int16": return DataType.SHORT;
         case "int32": return DataType.INT;
         case "uint8" : return DataType.UBYTE;
         case "uint16": return DataType.USHORT;
         case "uint32": return DataType.UINT;
         case "float" : return DataType.FLOAT;
         case "double" : return DataType.DOUBLE;
         case "bit" : return DataType.BYTE;
         // not supported directly. One channel for real components and
         // other for imaginary.
         case "complex" : return null;
         case "doubleComplex" : return null;
      }
      
      return DataType.BYTE; // default value.
   }
   
   @Override
   public boolean attachSequence(Long id, Sequence sequence, String name, String notes) throws ClientException {
      final String directory = FileUtil.getApplicationDirectory() + "/metro/";
      
      if ( FileUtil.createDir(new File(directory)) ) {
         final File file = new File(directory + "/" + name);
         Saver.save(sequence, file, false, false);
         final Boolean ret = attachFile(id, file.getAbsolutePath(), name, notes);
         FileUtil.delete(file, false);
         return ret;
      }
      
      return false;
   }
   
   @Override
   public OMEXMLMetadataImpl getOriginalMetaData(Long id) throws ClientException {
      throw new ClientException("OMEXMLMetadataImpl is not implemented");
   }
   
   @Override
   public Long uploadSequence(String idContainer, Sequence sequence) throws ClientException {
      final Long idDataset = Long.parseLong(idContainer);
      
      try {
         return omeClient_.upload(idDataset, sequence, null);
      } catch (ExecutionException|DSAccessException|DSOutOfServiceException e) {
         throw new ClientException(e.getMessage());
      }
   }
   
   @Override
   public Long uploadSequence(Long idImage, Sequence sequence) throws ClientException {
      final Long idDataset = omeClient_.getDatasetIdOfImage(idImage);
      
      try {
         return omeClient_.upload(idDataset, sequence, null);
      } catch (ExecutionException|DSAccessException|DSOutOfServiceException e) {
         throw new ClientException(e.getMessage());
      }
   }
   
   @Override
   public boolean annotateImage(Long idImage, String key, Object value) throws ClientException {
      MapAnnotationData map = new MapAnnotationData();
      List<NamedValue> lst = new ArrayList<>();
      map.setContent(lst);
      lst.add(new NamedValue(key, value.toString()));
      return omeClient_.attachAnnotationToImage(map, idImage);
   }
   
   @Override
   public Object getAnnotationValue(Long idImage, String key) throws ClientException {
      final List<AnnotationData> annotations = omeClient_.getAnnotationsImageMap(idImage);
      
      for ( AnnotationData adata : annotations ) {
         final MapAnnotationData map = (MapAnnotationData) adata;
         for ( NamedValue namedValue : (List<NamedValue>) map.getContent() ) {
            if ( namedValue.name.equals(key) ) {
               return namedValue.value;
            }
         }
      }
      
      return null;
   }
   
   @Override
   public Map<String, Object> getAllAnnotations(Long idImage) throws ClientException {
      
      final List<AnnotationData> annotations = omeClient_.getAnnotationsImageMap(idImage);
      final Map<String, Object> ret = new HashMap<> ();
      
      for ( AnnotationData adata : annotations ) {
         final MapAnnotationData map = (MapAnnotationData) adata;
         for ( NamedValue namedValue : (List<NamedValue>) map.getContent() ) {
            ret.put(namedValue.name, namedValue.value);
         }
      }
      
      return ret;
   }
   
   @Override
   public boolean commentImage(Long idImage, String commentary) throws ClientException {
      return omeClient_.commentImage(idImage, commentary);
   }
   
   @Override
   public boolean createOverlay(Long idImage, int siteNo, String overlayName) throws ClientException {
      throw new ClientException("createOverlay is not implemented and probably not neccesary");
   }
   
   // TODO: we could change Overlay concept by ROI... ? which one is better ? (ou pas ?)
   // NOTE: site is not used in Omero's implementation. just don't care.
   @Override
   public boolean addShapesToOverlay(Long idImage, int site, String overlayName, ROI[] rois) throws ClientException {
      boolean ret = false;
      
      try {
         for ( final ROI roi : rois ) {
            omeClient_.addRoi(idImage, roi);
         }
         ret = true;
      } catch (CannotCreateSessionException|PermissionDeniedException|ServerError|DSOutOfServiceException e) {
         throw new ClientException(e.getMessage());
      }
      
      return ret;
   }
   
   @Override
   public ROI[] getShapesFromOverlay(Long idImage, int site, String overlayName) throws ClientException {
      try {
         return omeClient_.getRois(idImage);
      } catch (DSOutOfServiceException | DSAccessException | OMEClientException e) {
         throw new ClientException(e.getMessage());
      }
   }
   
   @Override
   public void setTaskProgress(Long taskHandlerid, int progress) throws ClientException {
      throw new ClientException("setTaskProgress is not implemented");
   }
   
   @Override
   public long[] searchImages(String freeText, Set<SearchCondition> filters, int maxResult) throws ClientException {
      throw new ClientException("searchImages is not implemented");
   }
   
   protected boolean recordMatchesConditions(Map<String, Object> annotationMap, Set<SearchCondition> filters) {
      boolean ret = true;
      
      for ( final SearchCondition condition : filters ) {
         final Object value = annotationMap.get(condition.getField());
         
         if ( value == null ) {
            return false;
         }
         
         if ( condition.getLowerLimit().equals(condition.getUpperLimit())) { // equal comparison.
            ret &= value.toString().equals(condition.getLowerLimit());
            
            if (!ret)
               return ret;
         }
         
         else {
            ret = false;
         }
         
         // TODO: Please implement another operators! (major than and minor than) :)
      }
      
      return ret;
   }
   
   @Override
   public long[] searchAll(Set<SearchCondition> filters) throws ClientException {
      Collection<ProjectData> projects = omeClient_.getAllProjects();
      List<Long> ret = new ArrayList<>();
      
      // TODO: Find out a way that OMERO System can able to improve this directly. (Optimization)
      
      for ( final ProjectData project : projects ) {
         for ( final omero.gateway.model.DatasetData ds : project.getDatasets() ) {
            omero.gateway.model.DatasetData dataset = omeClient_.getDataset(ds.getId());
            final Set<ImageData> idata = (Set<ImageData>) dataset.getImages();
            
            if ( idata == null ) 
               continue;
            
            for ( final ImageData i : idata ) {
               final Map<String, Object> annotationMap = getAllAnnotations(i.getId());
               if (recordMatchesConditions(annotationMap, filters))
                  ret.add(i.getId());
            }
         }
      }
      
      return ArrayUtils.toPrimitive(ret.toArray(new Long[ret.size()]));
   }
   
   @Override
   public long[] getAllProjects() throws ClientException {
      List<Long> ret = new ArrayList<>();
      
      for (ProjectData project :  omeClient_.getAllProjects() ) {
         ret.add(project.getId());
      }
      
      return ArrayUtils.toPrimitive(ret.toArray(new Long[ret.size()]));
   }
   
   @Override
   public BufferedImage getThumbnail(Long id_) throws ClientException {
      throw new ClientException("getThumbnail is not implemented yet");
   }
   
   @Override
   public boolean deleteOverlay(Long idImage, int siteNo, String overlayName) throws ClientException {
      throw new ClientException("deleteOverlay is not implemented yet");
   }
   
   @Override
   public boolean deleteAllShapes(Long idImage, int site, String overlayName) throws ClientException {
      throw new ClientException("deleteAllShapes is not implemented yet");
   }
   
   @Override
   public boolean deleteAttach(Long id, String name) throws ClientException {
      return omeClient_.deleteAttach(id, name);
   }
   
   @Override
   public boolean attachFile(Long id, String filePath, String name, String notes) throws ClientException {
      return omeClient_.attachFile(id, filePath, name, notes);
   }
   
   @Override
   public File getAttachment(Long id, String name) throws ClientException {
      return omeClient_.getAttachment(id, name);
   }
}
