package plugins.ofuica.metro.client;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Set;

import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.DataType;
import loci.formats.ome.OMEXMLMetadataImpl;

/**
 * Common interface to be used in connection with different services.
 * 
 * @author osvaldo
 */

public interface Client {
   
   /**
    * Log-in
    * 
    * @param ssl
    * @param host
    * @param port
    * @param appId
    * @param code: string where the service use two codes or login/pass pair.
    * @return
    * @throws ClientException
    */
   public boolean login(boolean ssl, String host, int port, String appId, String[] code) throws ClientException;
   
   /**
    * @param ssl
    * @param host
    * @param port
    * @param appId
    * @param code
    * @return
    * @throws ClientException
    */
   public default boolean login(boolean ssl, String host, int port, String appId, String code) throws ClientException {
      return login(ssl, host, port, appId, new String[] { code }); 
   }
   
   /**
    * Login using access key. *** :)
    * 
    * @param ssl
    * @param host
    * @param port
    * @param accessKey
    * @return
    * @throws ClientException
    */
   public boolean login(boolean ssl, String host, int port, String[] accessKey) throws ClientException;
   
   /**
    * 
    * @param ssl
    * @param host
    * @param port
    * @param accessKey
    * @return
    * @throws ClientException
    */
   
   public default boolean login(boolean ssl, String host, int port, String accessKey) throws ClientException {
      return login(ssl, host, port, new String[] { accessKey });
   }
   
   /**
    * Log-in passing an url. host:port (443 is interpreted as ssl).
    * @param url
    * @param appId
    * @param code
    * @return
    * @throws ClientException
    */
   public boolean login(String url, String appId, String[] code) throws ClientException;
   
   /**
    * 
    * @param url
    * @param appId
    * @param code
    * @return
    * @throws ClientException
    */
   public default boolean login(String url, String appId, String code) throws ClientException {
      return login(url, appId, new String[] {code});
   }
   
   /**
    * Log-in passing an url. host:port (443 is interpreted as ssl), but ussing an accessKey
    * 
    * @param url
    * @param accessKey
    * @return
    * @throws ClientException
    */
   public boolean login(String url, String[] accessKey) throws ClientException;
   
   /**
    * 
    * @param url
    * @param accessKey
    * @return
    * @throws ClientException
    */
   public default boolean login(String url, String accessKey) throws ClientException {
      return login(url, new String[]{accessKey});
   }
   
   /**
    * Logout the current connection.
    * @return
    * @throws ClientException
    */
   public void logout() throws ClientException;
   
   /**
    * Checks if the connection that has been done with login() is already active or available.
    * 
    * @return
    * @throws ClientException
    */
   public boolean isConnected() throws ClientException;
   
   /**
    * Return a planar XY image.
    * @param id
    * @param z slice
    * @param rect crop or rectangle of interest. null means full-image.
    * @return an array XY
    * @throws ClientException
    */
   
   public Object downloadPlanar(Long id, int t, int z, int c, Rectangle rect) throws ClientException;
   
   /**
    * Returns the data's type of an image (PixelDepth equivalent)
    * 
    * @param id
    * @return
    * @throws ClientException
    */
   public DataType getDataType(Long id) throws ClientException; 
   
   /**
    * Annotates an image (defines an entry key-value data).
    * 
    * @param id
    * @param annotation
    * @return
    * @throws ClientException
    */
   
   public boolean annotateImage(Long id, String key, Object value) throws ClientException;
   
   /**
    * Attaches a sequence as File to an image id. (gid)
    * @param id
    * @param sequence
    * @param name
    * @param notes
    * @return
    * @throws ClientException
    */
   public boolean attachSequence(Long id, Sequence sequence, String name, String notes) throws ClientException;
   
   /**
    * Attaches a file path as File to an image id.
    * 
    * @param id
    * @param filePath
    * @param name
    * @param notes
    * @return
    * @throws ClientException
    */
   public boolean attachFile(Long id, String filePath, String name, String notes) throws ClientException;
   
   /**
    * Returns a temporary file with the attachment stored in the server.
    * 
    * @param value
    * @param name
    * @throws ClientException
    */
   public File getAttachment(Long value, String name) throws ClientException;
   
   /***
    * Deletes attached file with name associated with the image guid: id.
    * 
    * @param id
    * @param name
    * @return
    * @throws ClientException
    */
   public boolean deleteAttach(Long id, String name) throws ClientException;
   
   /**
    * Uploads a sequence in a container (project or dataset).
    * 
    * @param idContainer: OpenImadis (project (name unique key) ) and Omero (dataset (id) ). 
    * @param image 
    * @return
    * @throws ClientException
    */
   public Long uploadSequence(String idContainer, Sequence sequence) throws ClientException;
   
   /**
    * Uploads a sequence in the same project that idImage belongs.
    * 
    * @param idImage: Id of the relative image which belongs at the same project or dataset.
    * @param sequence
    * @return
    * @throws ClientException
    */
   public Long uploadSequence(Long idImage, Sequence sequence) throws ClientException;
   
   /**
    * Creates a meta-data using the variables available in the respective client. 
    * This function should not return null (avoid cause bad things would potentially happen). 
    * So if you have any problem please use the exception.
    * 
    * Return metaData
    * @param id image 
    * @return
    * @throws ClientException
    */
   public OMEXMLMetadataImpl getMetaData(Long id) throws ClientException;
   
   /**
    * Returns the original or an attached metadata.
    * @param id
    * @return
    * @throws ClientException
    */
   public OMEXMLMetadataImpl getOriginalMetaData(Long id) throws ClientException;
   
   
   /**
    * Returns the value of an annotation (key) as String.
    * 
    * @param id image: Image's id
    * @param key the key value in the annotation system.
    * @return
    * @throws ClientException
    */
   
   public Object getAnnotationValue(Long idImage, String key) throws ClientException;
   
   /**
    * Returns a list of all annotations associated to an image.
    * 
    * @param idImage
    * @return
    */
   
   public Map<String, Object> getAllAnnotations(Long idImage) throws ClientException;
   
   /**
    * Adds a comment to the image. 
    * 
    * @param idImage
    * @param commentary String text.
    * @return
    * @throws ClientException
    */
   public boolean commentImage(Long idImage, String commentary) throws ClientException;
   
   /***
    * Creates an overlay to add shapes. The shapes will be created using List<ROI> of Icy.
    * 
    * @param idImage
    * @param siteNo
    * @param overlayName
    * @return
    * @throws ClientException
    */
   
   public boolean createOverlay(Long idImage, int siteNo, String overlayName) throws ClientException;
   
   /***
    * Destroys an overlay and all its shapes inside.
    * 
    * @param idImage
    * @param siteNo
    * @param overlayName
    * @return
    * @throws ClientException
    */
   
   public boolean deleteOverlay(Long idImage, int siteNo, String overlayName) throws ClientException;
   
   /**
    * retrieves an overlay and its shapes.
    * 
    * @param idImage
    * @param site
    * @param overlayName
    * @return ROI[] Icy's ROI arrays.
    * @throws ClientException
    */
   
   public ROI[] getShapesFromOverlay(Long idImage, int site, String overlayName) throws ClientException;
   
   /***
    * Adds Icy's ROI to an overlay.
    * 
    * @param idImage guid
    * @param site 
    * @param overlayName
    * @param rois Icy's ROIs collection.
    * @return true if it checks all its shapes added.
    * @throws ClientException
    */
   public boolean addShapesToOverlay(Long idImage, int site, String overlayName, final ROI[] rois) throws ClientException;
   
   /***
    * Deletes all shapes for a given overlayName.
    * If the system does not support overlay it should delete all shapes (ROI) related to the image.
    * 
    * @param idImage
    * @param site
    * @param overlayName
    * @return
    * @throws ClientException
    */
   public boolean deleteAllShapes(Long idImage, int site, String overlayName) throws ClientException;
   
   /***
    * Notifies to server the task's progress.
    * @param taskHandlerid
    * @param progress (number from 0 to 100 %)
    */
    
    public void setTaskProgress(Long taskHandlerid, int progress) throws ClientException;
    
    /***
     * Searches uid or guid's images using conditions for filters.
     * 
     * @param freeText
     * @param filters
     * @param maxResult
     * @throws ClientException
     */
    public long[] searchImages(String freeText, Set<SearchCondition> filters, int maxResult) throws ClientException;
    
    /***
     * Returns all images's gui of the active projects that I can access and which matches the filters.
     * 
     * @param filters
     * @return
     * @throws ClientException
     */
    public long[] searchAll(Set<SearchCondition> filters) throws ClientException;
    
    /**
     * Returns all the projects that belong to the logged user.
     * 
     * @return
     * @throws ClientException
     */
    public long[] getAllProjects() throws ClientException;
    
    /**
     * Returns the thumbnail of the image id.
     *  
     * @param id_
     * @return
     */
    public BufferedImage getThumbnail(Long id_) throws ClientException;
}
