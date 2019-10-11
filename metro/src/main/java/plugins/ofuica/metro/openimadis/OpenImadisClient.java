/**
 * 
 */
package plugins.ofuica.metro.openimadis;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.ArrayUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.strandgenomics.imaging.iclient.ImageSpace;
import com.strandgenomics.imaging.iclient.ImageSpaceObject;
import com.strandgenomics.imaging.iclient.PixelMetaData;
import com.strandgenomics.imaging.iclient.Project;
import com.strandgenomics.imaging.icore.Channel;
import com.strandgenomics.imaging.icore.Dimension;
import com.strandgenomics.imaging.icore.IAttachment;
import com.strandgenomics.imaging.icore.IChannel;
import com.strandgenomics.imaging.icore.IPixelData;
import com.strandgenomics.imaging.icore.IVisualOverlay;
import com.strandgenomics.imaging.icore.ImageType;
import com.strandgenomics.imaging.icore.Site;
import com.strandgenomics.imaging.icore.SourceFormat;
import com.strandgenomics.imaging.icore.VODimension;
import com.strandgenomics.imaging.icore.image.PixelArray;
import com.strandgenomics.imaging.icore.image.PixelDepth;
import com.strandgenomics.imaging.icore.vo.Circle;
import com.strandgenomics.imaging.icore.vo.Ellipse;
import com.strandgenomics.imaging.icore.vo.GeometricPath;
import com.strandgenomics.imaging.icore.vo.Polygon;
import com.strandgenomics.imaging.icore.vo.VisualObject;
import com.strandgenomics.imaging.icore.vo.VisualObjectType;
import com.strandgenomics.imaging.iclient.Record;
import com.strandgenomics.imaging.iclient.RecordBuilder;

import icy.file.FileUtil;
import icy.file.Saver;
import icy.painter.Anchor2D;
import icy.roi.ROI;
import icy.roi.ROI3D;
import icy.sequence.MetaDataUtil;
import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.type.rectangle.Rectangle5D;
import icy.util.OMEUtil;
import loci.common.services.ServiceException;
import loci.common.xml.XMLTools;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.ome.OMEXMLMetadataImpl;
import loci.formats.services.OMEXMLServiceImpl;
import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DPoint;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;
import plugins.ofuica.metro.client.SearchCondition;
import plugins.ofuica.metro.utils.NotNullArrayList;

/**
 * OpenImadisClient implementation.
 * 
 * @author osvaldo
 */

public final class OpenImadisClient implements Client {
   
   @Override
   public boolean login(boolean ssl, String host, int port, String[] accessKey) throws ClientException {
      ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      return ispace.setAccessKey(ssl, host, port, accessKey[0]);
   }
   
   @Override
   public boolean login(boolean ssl, String host, int port, String appId, String[] code) throws ClientException {
      ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      return ispace.login(ssl, host, port, appId, code[0]);
   }
   
   @Override
   public boolean login(String url, String[] accessKey) throws ClientException {
      try {
         URI uri = new URI("http://" + url);
         String host = uri.getHost();
         int port = uri.getPort();
         
         if (host == null || port == -1) {
            throw new URISyntaxException(uri.toString(), "URI must have host and port parts");
         }
         
         return login(port == 443, host, port, accessKey[0]);
         
      } catch (URISyntaxException ex) {
         throw new ClientException(ex.getMessage());
      }
   }
   
   @Override
   public boolean login(String url, String appId, String[] code) throws ClientException {
      
      try {
         URI uri = new URI("http://" + url);
         String host = uri.getHost();
         int port = uri.getPort();
         
         if (host == null || port == -1) {
            throw new URISyntaxException(uri.toString(), "URI must have host and port parts");
         }
         
         return login(port == 443, host, port, appId, code[0]);
         
      } catch (URISyntaxException ex) {
         throw new ClientException(ex.getMessage());
      }
   }
   
   @Override
   public void logout() throws ClientException {
      ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      
      if ( ispace.getUser() != null ) {
         ispace.logout();
      }
   }
   
   @Override
   public boolean isConnected() throws ClientException {
      ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      String key = ispace.getAccessKey();
      return key != null;
   }
   
   @Override
   public Object downloadPlanar(Long id, int t, int z, int c, Rectangle rectangle) throws ClientException {
      boolean fullImage = rectangle == null;
      int serie = 0;
      final Record record = getRecord(id);
      
      if ( fullImage ) {
         rectangle = new Rectangle(0, 0, record.getImageWidth(), record.getImageHeight());
      }
      
      int X = (int) rectangle.getX();
      int Y = (int) rectangle.getY();
      int W = Math.max(Math.min((int) rectangle.getWidth(), record.getImageWidth()-X), 0);
      int H = Math.max(Math.min((int) rectangle.getHeight(), record.getImageHeight()-Y), 0);
      
      OMEXMLMetadataImpl metaData = getOriginalMetaData(id);
      
      if ( metaData == null ) {
         metaData = getMetaData(id);
      }
      
      final DataType dataType = MetaDataUtil.getDataType( (OMEXMLMetadata) metaData, serie);
      final Object ret = Array1DUtil.createArray(dataType, ((int)rectangle.getWidth())*((int)rectangle.getHeight()));
      
      IPixelData pixelData = record.getPixelData(new Dimension(t, z, c, 0));
      PixelArray pa = null;
      
      try {
         pa = pixelData.getRawData(rectangle);
         
      } catch (IOException e) {
         throw new ClientException(e.getMessage());
      }
      
      final int maxX = W - 1;
      final int maxY = H - 1;
      
      for ( int i = 0 ; i < maxX*maxY; i++ ) {
         if (dataType == DataType.BYTE) {
            ((byte[]) ret)[i] = (byte) pa.getPixelValue(i);
         }
         
         else if ( dataType == DataType.UBYTE) {
            Array1DUtil.setValue(ret, i, pa.getPixelValue(i));
         }
         
         else if ( dataType == DataType.SHORT) { 
            ((short[]) ret)[i] = (short) pa.getPixelValue(i);
         }
         
         else if (dataType == DataType.USHORT ) {
            Array1DUtil.setValue(ret, i, pa.getPixelValue(i));
         }
         
         else if (dataType == DataType.INT ) {
            ((int[]) ret)[i] = pa.getPixelValue(i);
         }
         
         else if (dataType == DataType.UINT ) {
            Array1DUtil.setValue(ret, i, pa.getPixelValue(i));
         }
         
         // TODO: You can use only this line and call getType foreach iteration.
         // Array1DUtil.setValue(ret, i, pa.getPixelValue(i));
         // FIXME: this depends on the GetDataType definition. (be careful)
         else {
            ((float[]) ret)[i] = pa.getPixelValue(i);
         }
      }
      
      return ret;
   }
   
   @Override
   public boolean annotateImage(Long id, String key, Object value) throws ClientException {
      final Record record = getRecord(id);
      final Map<String, Object> annotations = record.getUserAnnotations();
      Iterator<String> it = annotations.keySet().iterator();
      
      while(it.hasNext()) {
         final String k = it.next();
         
         if ( k.equals(key) ){
            final Object val = annotations.get(key);
            record.removeUserAnnotation(key);
            record.addCustomHistory("Annotation "+ key +"=" + val + " has been deleted from the record by ICY client.");
         }
      }
      
      if ( value instanceof Double ) {
         record.addUserAnnotation(key, (Double) value);
      }
      
      else if ( value instanceof Integer ) {
         record.addUserAnnotation(key, (Integer) value);
      }
      
      else if ( value instanceof String ) {
         record.addUserAnnotation(key, (String) value); 
      }
      
      else if ( value instanceof Date ) {
         record.addUserAnnotation(key, (Date) value);
      }
      
      else {
         System.out.println("Annotation for the datatype is not implemented: " + value.getClass().getName());
         return false;
      }
      
      return true;
   }
   
   @Override
   public Long uploadSequence(String idContainer, Sequence sequence) throws ClientException {
      final ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      final Project project = ispace.findProject(idContainer);
      
      if ( project == null ) {
         throw new ClientException("I could not find the container.");
      }
      
      return uploadSequence(project, sequence);
   }
   
   @Override
   public Long uploadSequence(Long idImage, Sequence sequence) throws ClientException {
      final Record record = getRecord(idImage);
      final Project project = record.getParentProject();
      
      if (project == null) {
         throw new ClientException("I could find the parent project.");
      }
      
      return uploadSequence(project, sequence);
   }
   
   private Record getRecord(Long id) throws ClientException {
      final ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      
      if ( ispace == null ) {
         throw new ClientException("Connection was not established");
      }
      
      final Record record = ispace.findRecordForGUID(id.longValue());
      
      if ( record == null ) {
         throw new ClientException("This record does not exist or you do not have access with your account, currently: "
               + ispace.getUser());
      }
      
      return record;
   }
   
   /**
    * Note: This function should be called just in case that OriginalMetaData does not exist.
    * 
    */
   @Override
   public OMEXMLMetadataImpl getMetaData(Long id) throws ClientException {
      final Record record = getRecord(id);
      
      final OMEXMLMetadata metaData = new OMEXMLMetadataImpl();
      int serie = 0 ; // are there multiples series support in openImadis ?
      boolean signed = false; // are there signed definition. I hope yes!.
      MetaDataUtil.setNumSerie(metaData, 1); // this number depend on the last line.
      MetaDataUtil.setDataType(metaData, serie, getDataType(record.getPixelDepth(), signed));
      MetaDataUtil.setNumChannel(metaData, serie,record.getChannelCount());
      
      for ( int c = 0 ; c <  record.getChannelCount() ; c++ ) {
         IChannel channel = record.getChannel(c);
         MetaDataUtil.setChannelName(metaData, serie, c, channel.getName());
      }
      
      MetaDataUtil.setImageID(metaData, serie, "" + record.getGUID());
      
      MetaDataUtil.setSizeC(metaData, serie, record.getChannelCount());
      MetaDataUtil.setSizeX(metaData, serie, record.getImageWidth());
      MetaDataUtil.setSizeY(metaData, serie, record.getImageHeight());
      MetaDataUtil.setSizeZ(metaData, serie, record.getSliceCount());
      MetaDataUtil.setSizeT(metaData, serie, record.getFrameCount());
      
      MetaDataUtil.setPixelSizeX(metaData, serie, record.getPixelSizeAlongXAxis());
      MetaDataUtil.setPixelSizeY(metaData, serie, record.getPixelSizeAlongYAxis());
      MetaDataUtil.setPixelSizeZ(metaData, serie, record.getPixelSizeAlongZAxis());
      
      // TODO: check if there is another user-friendly name available in the site. IT SHOULD
      MetaDataUtil.setName(metaData, serie, "" + record.getGUID());
      return (OMEXMLMetadataImpl) metaData;
   }
   
   @Override
   public DataType getDataType(Long id) throws ClientException {
      final Record record = getRecord(id);
      
      if ( record == null ) {
         throw new ClientException("Error, record was not found.");
      }
      
      final PixelDepth pixelDepth = record.getPixelDepth();
      boolean signed = false;
      // TODO: now assumming that OpenImadis is always using unsigned information.
      // FIXME: clarify if there is a criteria to know signed information without MetaData.
      return getDataType(pixelDepth, signed);
   }
   
   private DataType getDataType(PixelDepth pixelDepth, boolean signed) {
      
      if ( pixelDepth == PixelDepth.BYTE ) {
         if ( signed )
            return DataType.BYTE;
         else 
            return DataType.UBYTE;
      } else if ( pixelDepth == PixelDepth.SHORT ) {
         if ( signed ) 
            return DataType.SHORT;
         else 
            return DataType.USHORT;
      } else if ( pixelDepth == PixelDepth.INT ) {
         if ( signed ) 
            return DataType.INT;
         else
            return DataType.UINT;
      }
      
      //  FIXME: Implementation for double/float long type is supported in OpenImadis? or we should do
      // a conversion?. 
      // else {
      //    throw new ClientException("Data type is not supported");
      // }
      
      return DataType.FLOAT;
   }
   
   // FIXME: Try not be using temporary folders :/.
   @Override
   public boolean attachSequence(Long id, Sequence sequence, String name, String notes) throws ClientException {
      
      final Record record = getRecord(id);
      final String directory = FileUtil.getApplicationDirectory() + "/metro/";
      Collection<IAttachment> attachments = record.getAttachments();
      
      for ( IAttachment attachment : attachments ) {
         if ( attachment.getName().equals(name) ) {
            record.removeAttachment(name);
            attachment.delete();
            record.addCustomHistory("Attachment "+ name +" with file " + attachment.getFile().getAbsolutePath() + " has been deleted from the record by ICY client.");
            break;
         }
      }
      
      if ( FileUtil.createDir(new File(directory)) ) {
         final File file = new File(directory + "/" + name);
         Saver.save(sequence, file, false, false);
         record.addAttachment(file, name, notes);
         FileUtil.delete(file, false);
         return true;
      }
      
      return false;
   }
   
   @Override
   public OMEXMLMetadataImpl getOriginalMetaData(Long id) throws ClientException {
      final Record record = getRecord(id);
      final Collection<IAttachment> attachments = record.getAttachments();
      
      for ( IAttachment attachment : attachments ) {
         if ( attachment.getName().equals("OMEXMLMetaData.xml") ) {
            try {
               InputStream in =  attachment.getInputStream();
               InputStreamReader inputStreamReader = new InputStreamReader((InputStream)in, "UTF-8");
               BufferedReader reader = new BufferedReader(inputStreamReader);
               StringBuilder result = new StringBuilder();
               
               String line;
               while( (line = reader.readLine()) != null) {
                   result.append(line);
               }
               
               String xmlSanitized = XMLTools.sanitizeXML(result.toString());
               
               // HACK! This is a very dirty hack!. when we have unexpected values errors. (Please consider get this out)
               Document document = null;
               try {
                  document = XMLTools.parseDOM(xmlSanitized);
                  Element e = document.getDocumentElement();
                  
                  for ( int i = 0; i < e.getChildNodes().getLength() ; i++ ) {
                     Node node = e.getChildNodes().item(i);
                     if ( node.getNodeName().equals("Image") ) {
                        for ( int j = 0; j < node.getChildNodes().getLength(); j++ ) {
                           Node n = node.getChildNodes().item(j);
                           if ( n.getNodeName().equals("Pixels") ) {
                              for ( int k = 0 ; k < n.getAttributes().getLength(); k++ ) {
                                 Node kn = n.getAttributes().item(k);
                                 if (kn.getNodeValue().equals("?m") ) {
                                    kn.setNodeValue("µm");
                                 }
                              }
                              
                              for ( int k = 0 ; k < n.getChildNodes().getLength(); k++ ) {
                                 Node kn = n.getChildNodes().item(k);
                                 if ( kn.getNodeName().equals("Channel") ) {
                                    for ( int kk = 0 ; kk < kn.getAttributes().getLength(); kk++ ) {
                                       Node kkn = kn.getAttributes().item(kk);
                                       if (kkn.getNodeValue().equals("?m") ) {
                                          kkn.setNodeValue("µm");
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               } catch (ParserConfigurationException e) {
                  e.printStackTrace();
               } catch (SAXException e) {
                  e.printStackTrace();
               }
               
               // END OF HACK
               
               OMEXMLServiceImpl impl = new OMEXMLServiceImpl();
               try {
                  OMEXMLMetadata meta = impl.createOMEXMLMetadata(XMLTools.getXML(document));
                  return OMEUtil.createOMEMetadata(meta);
               } catch (ServiceException e1) {
                  throw new ClientException(e1.getMessage());
               } catch (TransformerConfigurationException e) {
                  throw new ClientException(e.getMessage());
               } catch (TransformerException e) {
                  throw new ClientException(e.getMessage());
               }
            } catch (IOException e) {
               throw new ClientException(e.getMessage());
            }
         }
      }
      
      return null;
   }
   
   private Long uploadSequence(final Project project, final Sequence seq) throws ClientException {
      
      // Double Image is not supported by OpenImadis, for this reason we should convert the sequence of double to float.
      // beware of this in future versions.
      Sequence sequence = null;
      if ( seq.getDataType_() == DataType.DOUBLE ) {
         sequence = SequenceUtil.convertToType(seq, DataType.FLOAT, false);
      } else {
         sequence = seq;
      }
      
      final List<Channel> channels = new ArrayList<>();
      for (int c = 0; c < sequence.getSizeC(); c++ ) {
         channels.add(new Channel(sequence.getChannelName(c)));
      }
      
      final List<Site> sites = new ArrayList<>();
      sites.add(new Site(0, "Site 0"));
      
      final OMEXMLMetadataImpl metaData = sequence.getMetadata();
      final RecordBuilder rb = project.createRecordBuilder(sequence.getName(), sequence.getSizeT(), sequence.getSizeZ(),
            channels, sites , sequence.getWidth(), sequence.getHeight(), getPixelDepth(sequence.getDataType_()),
            sequence.getPixelSizeX(), sequence.getPixelSizeY(), sequence.getPixelSizeZ(),
            ImageType.GRAYSCALE, new SourceFormat("IMG"), "", "/tmp", System.currentTimeMillis(),
            System.currentTimeMillis(), System.currentTimeMillis());
      
      final double interval = sequence.getTimeInterval();
      double elapsedtime = interval;
      
      if ( sequence.getSizeT() > 1 ) {
         elapsedtime = interval /(sequence.getSizeT()-1);
      }
      
      Double exposureTime = 1.0;
      
      try {
         exposureTime = (Double) metaData.getPlaneExposureTime(0, 0).value();
      } catch (Exception e)  {
         exposureTime=1.0;
      }
      
      for(int time = 0; time < sequence.getSizeT();time++) {
         for(int slice = 0;slice < sequence.getSizeZ();slice++) {
            for(int channel = 0;channel < sequence.getSizeC();channel++) {
               PixelArray rawData = PixelArray.toPixelArray(sequence.getImage(time, slice, channel));
               PixelMetaData pixelData = new PixelMetaData(new Dimension(time, slice, channel, 0), 
                     sequence.getPixelSizeX(), sequence.getPixelSizeY(), sequence.getPixelSizeZ(), 
                     elapsedtime, exposureTime, new Date());
               rb.addImageData(new Dimension(time, slice, channel, 0), rawData, pixelData );
            }
         }
      }
      
      Record record = rb.commit();
      record.addCustomHistory("Record was uploaded using Icy Block");
      System.out.println("Record correctly uploaded: new ID is : "+record.getGUID());
      return record.getGUID();
   }
   
   // TODO: Please consider moving it to a helper class, package...
   
   private PixelDepth getPixelDepth(DataType dataType_)
   {
      if(dataType_ == DataType.BYTE || dataType_ == DataType.UBYTE)
         return PixelDepth.BYTE;
      if(dataType_ == DataType.SHORT || dataType_ == DataType.USHORT)
         return PixelDepth.SHORT;
      if(dataType_ == DataType.INT || dataType_ == DataType.UINT)
         return PixelDepth.INT;
      
      if (dataType_ == DataType.FLOAT )
         return PixelDepth.INT; // this is correct i guess ?
      
      // Double not supported by OpenImadis stack.
      throw new IllegalArgumentException("unknown data type: " + dataType_);
   }
   
   @Override
   public Object getAnnotationValue(Long idImage, String key) throws ClientException {
      final Record record = getRecord(idImage);
      final Map<String, Object> annotations = record.getUserAnnotations();
      final Iterator<String> it = annotations.keySet().iterator();
      
      while(it.hasNext()) {
         final String k = it.next();
         
         if ( k.equals(key) )
            return annotations.get(key);
      }
      
      return null;
   }
   
   @Override
   public Map<String, Object> getAllAnnotations(Long idImage) throws ClientException {
      final Record record = getRecord(idImage);
      return record.getUserAnnotations();
   }
   
   @Override
   public boolean commentImage(Long idImage, String commentary) throws ClientException {
      final Record recordImage = getRecord(idImage);
      recordImage.addUserComments(commentary);
      return true;
   }
   
   public String toString() {
      return "OpenImadisClient";
   }
   
   @Override
   public boolean createOverlay(Long idImage, int siteNo, String overlayName) throws ClientException {
      final Record record = getRecord(idImage);
      Set<String> overlays = record.getAvailableVisualOverlays(siteNo);
      
      if (overlays!= null && overlays.contains(overlayName)) {
         return true;
      }
      
      record.createVisualOverlays(siteNo, overlayName);
      overlays = record.getAvailableVisualOverlays(siteNo);
      return overlays.contains(overlayName);
   }
   
   private Ellipse createEllipseROI(ROI2DEllipse roi) {
      List<Anchor2D> ctrlPoints = roi.getControlPoints();
      
      if ( ctrlPoints.size() != 4 ) 
         return null;
      
      final Anchor2D ctrl1 = ctrlPoints.get(0);
      final Anchor2D ctrl2 = ctrlPoints.get(1);
      final Anchor2D ctrl3 = ctrlPoints.get(2);
      
      double rx = (ctrl2.getPositionX() - ctrl1.getPositionX()) * 0.5f;
      double ry = (ctrl3.getPositionY() - ctrl1.getPositionY()) * 0.5f;
      
      double x = roi.getPosition5D().getX() + rx;
      double y = roi.getPosition5D().getY() + ry;
      
      final Ellipse e = new Ellipse(x, y, rx, ry);
//TODO: use dimensions to set this op.
//      ellipse.setC(roi.getC());
//      ellipse.setZ(roi.getZ());
//      ellipse.setT(roi.getT());
//      ellipse.setText(roi.getName());
      final Color rColor = roi.getColor();
      final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
            roi.getOpacity());
      
      e.setPenColor(color);
      e.setPenWidth((float)roi.getStroke());
      return e;
   }
   
   private com.strandgenomics.imaging.icore.vo.Rectangle createRectangleROI(ROI2DRectangle roi) {
      final List<Anchor2D> controlPoints = roi.getControlPoints();
      
      if ( controlPoints.size() == 4 ) {
         final Anchor2D ctrl1 = controlPoints.get(0);
         final Anchor2D ctrl2 = controlPoints.get(1);
         final Anchor2D ctrl3 = controlPoints.get(2);
         
         double w = ctrl2.getPositionX() - ctrl1.getPositionX();
         double h = ctrl3.getPositionY() - ctrl1.getPositionY();
         double x = roi.getPosition5D().getX();
         double y = roi.getPosition5D().getY();
         
         final com.strandgenomics.imaging.icore.vo.Rectangle rectangle =
               new com.strandgenomics.imaging.icore.vo.Rectangle(x, y, w, h);
         
         final Color rColor = roi.getColor();
         final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
               roi.getOpacity());
         
         rectangle.setPenColor(color);
         rectangle.setPenWidth((float) roi.getStroke());
         return rectangle;
      }
      
      return null;
   }
   
   private Circle createCircleRoiFromPoint(ROI2DPoint roi) {
      final double x = roi.getPosition5D().getX();
      final double y = roi.getPosition5D().getY();
      final Circle c = new Circle(x, y, 5, 5);
      final Color rColor = roi.getColor();
      final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f,
            roi.getOpacity());
      
      c.setPenColor(color);
      c.setPenWidth((float) roi.getStroke());
      return c;
   }
   
   private Polygon createPoligonROI(ROI2DPolygon roi) {
      
      final Polygon polygon = new Polygon();
      final List<Point2D> pts = roi.getPoints();
      
      for (final Point2D pt : pts) {
         polygon.lineTo(pt.getX(), pt.getY());
      }
      
      if ( pts.size() > 2) {
         Point2D ini = pts.get(0);
         polygon.lineTo(ini.getX(), ini.getY());
      }
      
      final Color rColor = roi.getColor();
      final Color color = new Color(rColor.getRed()/255.0f, rColor.getGreen()/255.0f, rColor.getBlue()/255.0f, 
            roi.getOpacity());
      polygon.setPenColor(color);
      polygon.setPenWidth((float) roi.getStroke());
      return polygon;
   }
   
   private ROI createRoiFromEllipse(Ellipse ellipse, VODimension dim)
   {
      double x = ellipse.getBounds().getX();
      double y = ellipse.getBounds().getY();
      double xmax = ellipse.getBounds().getMaxX();
      double ymax = ellipse.getBounds().getMaxY();
      
      // TODO: We could apply this transformation with rotation with Closed and Open Paths.
      /*
      final AffineTransform at = new AffineTransform();
      at.setToIdentity();
      at.rotate(elipse.getRotation()); // check rad or degrees.
      */
      
      final ROI2DEllipse e = new ROI2DEllipse(x, y, xmax, ymax);
      e.setPosition2D(new Point2D.Double(x, y)); // check me please.
      e.setColor(ellipse.getPenColor());
      e.setStroke(ellipse.getPenWidth());
      e.setZ(dim.sliceNo);
      e.setT(dim.frameNo);
      return e;
   }
   
   private ROI createRoiFromPolygon(GeometricPath poly, VODimension dim) {
      final List<Point2D> points = poly.getPathPoints();
      final ROI2DPolygon roi = new ROI2DPolygon(points);
      roi.setColor(poly.getPenColor());
      roi.setStroke(poly.getPenWidth());
      roi.setZ(dim.sliceNo);
      roi.setT(dim.frameNo);
      return roi;
   }
   
   private ROI createRoiFromRectangle(com.strandgenomics.imaging.icore.vo.Rectangle rectangle, VODimension dim) {
      final ROI2DRectangle roi = new ROI2DRectangle();
      roi.setBounds2D(rectangle.getBounds());
      roi.setColor(rectangle.getPenColor());
      roi.setStroke(rectangle.getPenWidth());
      roi.setZ(dim.sliceNo);
      roi.setT(dim.frameNo);
      return roi;
   }
   
   private ROI createRoiFromCircle(com.strandgenomics.imaging.icore.vo.Circle circle, VODimension dim) {
      final ROI2DPoint roi = new ROI2DPoint(circle.getBounds().getX(), circle.getBounds().getY());
      roi.setZ(dim.sliceNo);
      roi.setT(dim.frameNo);
      return roi;
   }
   
   @Override
   public boolean addShapesToOverlay(Long idImage, int siteNo, String overlayName, final ROI[] rois)
         throws ClientException
   {
      final Record record = getRecord(idImage);
      
      if ( !createOverlay(idImage, siteNo, overlayName) ) {
         return false; // it could not create an overlay.
      }
      
      for ( final ROI roi : rois ) {
         final List<VisualObject> vObjects = new NotNullArrayList<>();
         
         if ( roi instanceof ROI2DPoint ) {
            vObjects.add(createCircleRoiFromPoint((ROI2DPoint) roi));
         }
         
         else if ( roi instanceof ROI2DEllipse ) {
            vObjects.add(createEllipseROI((ROI2DEllipse) roi));
         }
         
         else if ( roi instanceof ROI2DRectangle ) {
            vObjects.add(createRectangleROI((ROI2DRectangle) roi));
         }
         
         else if ( roi instanceof ROI2DPolygon ) {
            vObjects.add(createPoligonROI((ROI2DPolygon) roi));
         }
         
         else if ( roi instanceof ROI3D ) {
            System.out.println("Not supported right now. But we are working on it. " + roi.getClassName());
            return false;
         }
         
         else {
            System.out.println("Not supported shape: " + roi.getClassName());
            return false;
         }
         
         // This is possible because we are only supporting 2D shapes, but in different coordinates. (t, z)
         final Rectangle5D rect = roi.getBounds5D();
         record.addVisualObjects(vObjects, overlayName, new VODimension((int)rect.getMinT(),(int)rect.getMinZ(),siteNo));
         
         // But, the number of ROI3D will be multiplied by T and Z :/
         // So as TODO: consider this issue when one ROI comes back from OpenImadis to Icy.
         // for ( double t = rect.getMinT(); t < rect.getMaxT(); t++ ) {
         // for ( double z = rect.getMinZ(); z < rect.getMaxZ(); z++ ) {
         // record.addVisualObjects(vObjects, overlayName, new VODimension((int) t, (int) z, siteNo));
         // }
         // }
         vObjects.clear();
      }
      
      return true;
   }
   
   @Override
   public ROI[] getShapesFromOverlay(Long idImage, int site, String overlayName) throws ClientException {
      final Record record = getRecord(idImage);
      final OMEXMLMetadata metaData = getOriginalMetaData(idImage);
      final int T = MetaDataUtil.getSizeT(metaData, 0);
      final int Z = MetaDataUtil.getSizeZ(metaData, 0);
      
      final List<ROI> rois = new NotNullArrayList<>();
      
      for ( int t = 0 ; t < T ; t++ ) {
         for ( int z = 0; z < Z ; z++ ) {
            final VODimension dim = new VODimension(t, z, site);
            final IVisualOverlay overlay = record.getVisualOverlay(dim, overlayName);
            
            if ( overlay == null || overlay.getVisualObjects() == null ) 
               continue;
            
            for ( final VisualObject vobj : overlay.getVisualObjects() ) {
               // TODO: please implement each visual object's transformation.
               
               if ( vobj.getType() == VisualObjectType.ELLIPSE ) {
                  rois.add(createRoiFromEllipse((Ellipse) vobj, dim));
               }
               
               else if ( vobj.getType() == VisualObjectType.POLYGON ) {
                  rois.add(createRoiFromPolygon((GeometricPath) vobj, dim));
               }
               
               else if ( vobj.getType() == VisualObjectType.CIRCLE ) {
                  System.out.println("Circle class: " + vobj.getClass().getName()); // Are we going to use that for points or not ?2
                  createRoiFromCircle((com.strandgenomics.imaging.icore.vo.Circle) vobj, dim);
               }
               
               else if ( vobj.getType() == VisualObjectType.RECTANGLE ) {
                  rois.add(createRoiFromRectangle((com.strandgenomics.imaging.icore.vo.Rectangle) vobj, dim));
               }
               
               else {
                  System.out.println("Visual ObjectType conversion not supported: " + vobj.getType());
                  return null;
               }
            }
         }
      }
      
      return rois.toArray(new ROI[rois.size()]);
   }
   
   @Override
   public void setTaskProgress(Long taskHandlerid, int progress) throws ClientException {
      ImageSpaceObject.getImageSpace().setTaskProgress(taskHandlerid, progress);
   }
   
   @Override
   public long[] searchImages(String freeText, Set<SearchCondition> filters, int maxResult) throws ClientException {
      final ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      
      final Set<com.strandgenomics.imaging.icore.SearchCondition> conditions = new HashSet<>();
      
      for (final SearchCondition c : filters) {
         final Object ll = c.getLowerLimit();
         final Object ul = c.getUpperLimit();
         
         if ( ll instanceof Long && ul instanceof Long ) {
            conditions.add(new com.strandgenomics.imaging.icore.SearchCondition(c.getField(),
                  (Long) c.getLowerLimit(), (Long)c.getUpperLimit()));
         }
         
         else if ( ll instanceof Double && ul instanceof Double ) {
            conditions.add(new com.strandgenomics.imaging.icore.SearchCondition(c.getField(),
                  (Double) c.getLowerLimit(), (Double)c.getUpperLimit()));
         }
         
         else if ( ll instanceof String && ul instanceof String ) {
            conditions.add(new com.strandgenomics.imaging.icore.SearchCondition(c.getField(),
                  (String) c.getLowerLimit(), (String)c.getUpperLimit()));
         }
         
         else {
         }
      }
      
      return ispace.search(freeText, null, conditions, maxResult);
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
      final ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      final List<Project> projects = ispace.getActiveProjects();
      final List<Long> ret = new ArrayList<>();
      
      for ( Project prj : projects ) {
         for ( long guid : prj.getRecords() ) {
            final Map<String, Object> annotationMap = getAllAnnotations(guid);
            
            if (recordMatchesConditions(annotationMap, filters))
               ret.add(guid);
         }
      }
      
      return ArrayUtils.toPrimitive(ret.toArray(new Long[ret.size()]));
   }
   
   @Override
   public long[] getAllProjects() throws ClientException {
      final List<Long> ret = new ArrayList<>();
      final ImageSpace ispace = ImageSpaceObject.getConnectionManager();
      final List<Project> projects = ispace.getActiveProjects();
      
      for ( final Project p : projects )
         for ( long guid : p.getRecords() )
            ret.add(guid);
      
      return ArrayUtils.toPrimitive(ret.toArray(new Long[ret.size()]));
   }
   
   @Override
   public BufferedImage getThumbnail(Long id_) {
      try {
         final Record record = getRecord(id_);
         return record.getThumbnail();
      } catch (ClientException e) {
         e.printStackTrace();
      }
      return null;
   }
   
   @Override
   public boolean deleteOverlay(Long idImage, int siteNo, String overlayName) throws ClientException {
      final Record record = getRecord(idImage);
      Set<String> overlays = record.getAvailableVisualOverlays(siteNo);
      
      if (overlays!= null && overlays.contains(overlayName)) {
         record.deleteVisualOverlays(siteNo, overlayName);
         return true;
      }
      
      return false;
   }
   
   @Override
   public boolean deleteAllShapes(Long idImage, int site, String overlayName) throws ClientException {
      final Record record = getRecord(idImage);
      final List<VisualObject> vObjects = new NotNullArrayList<>();
      
      final OMEXMLMetadata metaData = getOriginalMetaData(idImage);
      int T = MetaDataUtil.getSizeT(metaData, 0);
      int Z = MetaDataUtil.getSizeZ(metaData, 0);
      
      for ( int t = 0 ; t < T ; t++ ) {
         for ( int z = 0; z < Z ; z++ ) {
            final VODimension dim = new VODimension(t, z, site);
            final IVisualOverlay overlay = record.getVisualOverlay(dim, overlayName);
            
            if ( overlay == null || overlay.getVisualObjects() == null ) 
               continue;
            
            for ( final VisualObject vobj : overlay.getVisualObjects() ) {
               vObjects.add(vobj);
            }
            
            record.deleteVisualObjects(vObjects, overlayName, dim);
            vObjects.clear();
         }
      }
      
      return false;
   }
   
   @Override
   public boolean deleteAttach(Long id, String name) throws ClientException {
      throw new ClientException("deleteAttach is not implemented yet");
   }
   
   @Override
   public boolean attachFile(Long id, String filePath, String name, String notes) throws ClientException {
      final Record record = getRecord(id);
      final Collection<IAttachment> attachments = record.getAttachments();
      
      for ( IAttachment attachment : attachments ) {
         if ( attachment.getName().equals(name) ) {
            record.removeAttachment(name);
            attachment.delete();
            record.addCustomHistory("Attachment "+ name +" with file " + attachment.getFile().getAbsolutePath() + " has been deleted from the record by ICY client.");
            break;
         }
      }
      
      if ( FileUtil.exists(filePath) ) {
         record.addAttachment(new File(filePath), name, notes);
         return true;
      }
      
      return false;
   }
   
   @Override
   public File getAttachment(Long id, String name) throws ClientException {
      final Record record = getRecord(id);
      
      for ( final IAttachment attach : record.getAttachments() ) {
         if ( attach.getName().equals(name) ) {
            return attach.getFile();
         }
      }
      
      return null;
   }
}
