/**
 * 
 */
package plugins.ofuica.metro.omero;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;
import icy.file.FileUtil;
import icy.file.Loader;
import icy.file.Saver;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.progress.CancelableProgressFrame;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.gui.frame.progress.FileFrame;
import icy.roi.ROI;
import icy.sequence.Sequence;
import ij.IJ;
import loci.common.services.ServiceException;
import loci.common.xml.XMLTools;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import loci.formats.ome.OMEXMLMetadata;
import loci.formats.services.OMEXMLServiceImpl;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.IObservable;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportEvent;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import ome.formats.importer.cli.LoggingImportMonitor;
import ome.model.core.Image;
import omero.RLong;

import omero.RType;
import omero.ServerError;
import omero.client;
import omero.api.ExporterPrx;
import omero.api.IAdminPrx;
import omero.api.IContainerPrx;
import omero.api.IMetadataPrx;
import omero.api.RawPixelsStorePrx;
import omero.api.SearchPrx;
import omero.api.ServiceFactoryPrx;
import omero.api.IPixelsPrx;
import omero.api.IQueryPrx;
import omero.api.RawFileStorePrx;
import omero.cmd.CmdCallbackI;
import omero.cmd.Delete2;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.facility.MetadataFacility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.RawDataFacility;
import omero.gateway.model.AnnotationData;
import omero.gateway.model.DataObject;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.FileAnnotationData;
import omero.gateway.model.GroupData;
import omero.gateway.model.ImageData;
import omero.gateway.model.MapAnnotationData;
import omero.gateway.model.ProjectData;
import omero.gateway.model.ROIData;
import omero.gateway.model.ROIResult;
import omero.gateway.model.ShapeData;
import omero.gateway.util.PojoMapper;
import omero.model.Annotation;
import omero.model.ChecksumAlgorithm;
import omero.model.ChecksumAlgorithmI;
import omero.model.CommentAnnotation;
import omero.model.CommentAnnotationI;
import omero.model.Dataset;
import omero.model.DatasetI;
import omero.model.DatasetImageLink;
import omero.model.DatasetImageLinkI;
import omero.model.Experimenter;
import omero.model.ExperimenterI;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.Fileset;
import omero.model.FilesetEntry;
import omero.model.IObject;
import omero.model.ImageAnnotationLink;
import omero.model.ImageAnnotationLinkI;
import omero.model.ImageI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.Pixels;
import omero.model.PixelsType;
import omero.model.ProjectDatasetLink;
import omero.model.ProjectDatasetLinkI;
import omero.model.ProjectI;
import omero.model.enums.ChecksumAlgorithmSHA1160;
import omero.sys.ParametersI;
import omero.log.Logger;
import omero.log.SimpleLogger;

/**
 * OMEClient's implementation
 * 
 * @author osvaldo
 */

public class OMEClient {
   private final Logger simpleLogger_;
   private final Gateway gateway_;
   private LoginCredentials credentials_ = null;
   private SecurityContext ctx_ = null;
   private ExperimenterData currentUserData_ = null;
   
   private Collection<GroupData> allGroupsCached_ = null;
   private boolean useCache_ = false;
   
   private static final int INC = 262144;//
   
   public OMEClient() {
      simpleLogger_ = new SimpleLogger();
      gateway_ = new Gateway(simpleLogger_);
   }
   
   public void useCache(boolean useCache) {
      useCache_ = useCache;
   }
   
   public void reconnect() throws OMEClientException {
      if ( gateway_ == null ) {
         return;
      }
      
      try {
         currentUserData_ = gateway_.connect(credentials_);
         ctx_ = new SecurityContext(currentUserData_.getDefaultGroup().getGroupId());
         // ctx = new SecurityContext(-1); // accept all groups. it is not working....
         ctx_.setExperimenter(currentUserData_);
         // ctx.sudo();
         // ctx.setExperimenter(currentUserData);
         
      } catch (final DSOutOfServiceException e) {
         throw new OMEClientException("reconnect failed: " + e.getMessage());
      }
   }
   
   public void connect(String host, int port, String username, String password) throws OMEClientException {
      credentials_ = new LoginCredentials(username, password, host, port);
      
      try {
         currentUserData_ = gateway_.connect(credentials_);
         final Long groupId = currentUserData_.getDefaultGroup().getGroupId();
         
         credentials_.setGroupID(groupId);
         ctx_ = new SecurityContext(groupId);
         ctx_.setExperimenter(currentUserData_);
         
         ResourceBundle bundle = ResourceBundle.getBundle("omero");
         String apiVersion = bundle.getString("omero.version");
         
         if ( !gateway_.getServerVersion().equals(apiVersion.split("-")[0])) {
            throw new OMEClientException("Version Incompatible!: hope: " + gateway_.getServerVersion() + ", api: " + apiVersion);
         }
         
         // ctx_.sudo();
         // ctx.sudo();
         // ctx.setExperimenter(currentUserData);
      } catch (final DSOutOfServiceException e) {
         throw new OMEClientException("I could not connect: " + e.getMessage());
      }
   }
   
   public Long getCurrentUserId() throws OMEClientException {
      if ( currentUserData_ != null ) {
         return currentUserData_.getId();
      }
      
      else {
         throw new OMEClientException("Not connected or error.");
      }
   }
   
   public SecurityContext getContext() {
      return ctx_;
   }
   
   public void disconnect() {
      gateway_.disconnect();
   }
   
   public BrowseFacility getBrowser() throws OMEClientException {
      try {
         final BrowseFacility browser = gateway_.getFacility(BrowseFacility.class);
         return browser;
      } catch (final ExecutionException e) {
         throw new OMEClientException(e.getMessage());
      }
   }
   
   public ROIFacility getRoiFacility() throws OMEClientException {
      try {
         final ROIFacility roiFacility = gateway_.getFacility(ROIFacility.class);
         return roiFacility;
      } catch (final ExecutionException e) {
         throw new OMEClientException(e.getMessage());
      }
   }
   
   public RawDataFacility getRawDataFacility() throws OMEClientException {
      try {
         final RawDataFacility rawDataFacility = gateway_.getFacility(RawDataFacility.class);
         return rawDataFacility;
      } catch (final ExecutionException e) {
         throw new OMEClientException(e.getMessage());
      }
   }
   
   public RawPixelsStorePrx getStore() throws OMEClientException {
      try {
         final RawPixelsStorePrx rawPixelsStore = gateway_.getPixelsStore(ctx_); 
         return rawPixelsStore;
      } catch (final DSOutOfServiceException e) {
         throw new OMEClientException(e.getMessage());
      }
   }
   
   public String getCurrentUserFullName() {
      final String completeName = currentUserData_.getFirstName() +
            " " + currentUserData_.getLastName();
      return completeName;
   }
   
   public String getCurrentUsername() {
      final String username = currentUserData_.getUserName();
      return username;
   }
   
   public String getCurrentGroupname() {
      final String groupname = currentUserData_.getDefaultGroup().getName();
      return groupname;
   }
   
   public Long getCurrentGroupId() {
      final Long id = currentUserData_.getDefaultGroup().getId();
      return id;
   }
   
   public Collection<ProjectData> getProjects() {
      try {
         final Collection<ProjectData> projects = getBrowser().getProjects(ctx_);
         return projects;
      } catch (OMEClientException | DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   public ProjectData getProject(final Long projectId) {
      final ProjectData ret = null;
      try {
         final List<Long> ids = new ArrayList<>();
         ids.add(projectId);
         final Collection<ProjectData> projects = getBrowser().getProjects(ctx_, ids);
         
         if ( !projects.isEmpty() ) {
            return (ProjectData) projects.toArray()[0];
         }
      } catch (DSOutOfServiceException | DSAccessException | OMEClientException e) {
         e.printStackTrace();
      }
      
      return ret;
   }
   
   public List<Experimenter> getAllUsersByGroup(long groupId) {
      try {
         final IAdminPrx admin = gateway_.getAdminService(ctx_);
         return admin.containedExperimenters(groupId);
      } catch (final ServerError | DSOutOfServiceException e) {
         e.printStackTrace();
      }
      
      return new ArrayList<>();
   }
   
   public Collection<GroupData> getAllGroups() {
      try {
         if ( useCache_ ) {
            
            if ( allGroupsCached_ == null ) {
               allGroupsCached_ = getBrowser().getAvailableGroups(ctx_, currentUserData_);
            }
            
            return allGroupsCached_;
         }
         else {
            allGroupsCached_ = getBrowser().getAvailableGroups(ctx_, currentUserData_);
            return allGroupsCached_;
         }
      } catch (DSOutOfServiceException | DSAccessException | OMEClientException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   public ImageData getImageData(final Long imageId) {
      
      ImageData ret = null;
      
      try {
         final ParametersI param = new ParametersI();
         param.exp(omero.rtypes.rlong(currentUserData_.getId()));
         param.allGrps();
         param.allExps();
         param.leaves();
         param.grp(omero.rtypes.rlong(-1L));
         param.acquisitionData();
         param.getOrphan();
         param.acquisitionData();
         
         ret = getBrowser().getImage(ctx_, imageId, param);
         
         // HACK: If you did not get a positive number of links that would be unexpected by a normal api-user!.
         // I guess new version of Omero (5.4) has some optimizations inside. LOL
         
         if ( ret.asImage().sizeOfDatasetLinks() < 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append("select i from Image i join fetch i.datasetLinks dil ");
            sb.append("where i.id = :id ");
            IQueryPrx query = gateway_.getQueryService(ctx_);
            param.addId(imageId);
            omero.model.Image image = (omero.model.Image) query.findByQuery(sb.toString(), param);
            ret.asImage().reloadDatasetLinks(image);
         }
         
      } catch (DSOutOfServiceException e) {
         e.printStackTrace();
      } catch (NoSuchElementException e) {
         // ignore this case.
      } catch (DSAccessException e1) {
         // ignore this case too
         e1.printStackTrace();
      } catch (OMEClientException e1) {
         // ignore this case too.
         e1.printStackTrace();
      } catch (ServerError e) {
         e.printStackTrace();
         e.printStackTrace();
      }
      
      return ret;
   }
   
   public Collection<ProjectData> getAllProjects() {
      try {
         return getAllProjectByExperimenter(getCurrentUserId());
      } catch (OMEClientException e) {
         e.printStackTrace();
      }
      
      return new ArrayList<>();
   }
   
   public Collection<ProjectData> getAllProjectByExperimenter(long experimenterId) {
      try {
         return getBrowser().getProjects(ctx_, experimenterId);
      } catch (DSOutOfServiceException | DSAccessException | OMEClientException e) {
         e.printStackTrace();
      }
      
      return new ArrayList<>();
   }
   
   public String getServerName() {
      return credentials_.getServer().getHostname() + ":" + credentials_.getServer().getPort();
   }
   
   public List<ImageI> searchImages(final String key) {
      final List<ImageI> ret = new ArrayList<>();
      
      // TODO: why we are not receiving all images of the server?
      // FIXME: May be there is a bug in this version of Blitz API (the context does not 
      // represent the permissions that it really should.
      
      try {
         final SearchPrx search = gateway_.getSearchService(ctx_);
         final List<String> l = new ArrayList<>();
         
         search.onlyType(Image.class.getName());
         l.add(key);
         search.byFullText(key);
         
         while ( search.hasNext() ) {
            final Object obj = search.next();
            
            if ( obj instanceof ImageI ) {
               final ImageI i = (ImageI) obj;
               ret.add(i);
            }
         }
      } catch (final DSOutOfServiceException | ServerError e) {
         MessageDialog.showDialog("Error: " + e.getMessage());
      }
      
      return ret;
   }
   
   public boolean createProject(final String name, final String description) {
      boolean ret = false;
      
      try {
         final ProjectI project = new ProjectI();
         project.setName(omero.rtypes.rstring(name));
         project.setDescription(omero.rtypes.rstring(description));
         
         final IObject robj = gateway_.getUpdateService(ctx_).saveAndReturnObject(project);
         ret = (robj != null);
      } catch (final DSOutOfServiceException | ServerError e) {
         e.printStackTrace();
      }
      
      return ret;
   }
   
   public boolean deleteProject(final Long idProject) {
      return deleteObject(idProject, "Project");
   }
   
   public boolean renameDataset(final Long idDataset, String name) {
      try {
         final BrowseFacility browser = gateway_.getFacility(BrowseFacility.class);
         final DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         final List<Long> ids = new ArrayList<>();
         ids.add(idDataset);
         
         final Collection<DatasetData> datasets = browser.getDatasets(ctx_, ids);
         
         if ( datasets.size() > 0 ) {
            DatasetData datasetData = datasets.iterator().next();
            if ( datasetData.getName().equals(name) ) {
               return false;
            }
            
            datasetData.setName(name);
            DatasetData returned = (DatasetData) dm.saveAndReturnObject(ctx_, datasetData);
            
            return (returned != null && returned.getName().equals(name));
         }
      } catch (final ExecutionException e) {
         e.printStackTrace();
      } catch (DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      
      return false;
   }
   
   protected boolean deleteObject(final Long idObject, final String typeDescription) {
      final Map<String, List<Long>> targetObject = new HashMap<>();
      final List<Long> idList = new ArrayList<>();
      final List<String> listToIgnore = new ArrayList<>();
      
      idList.add(idObject);
      targetObject.put(typeDescription, idList);
      final Delete2 deleteCommands = new Delete2(targetObject, null, false, listToIgnore);
      CmdCallbackI cb;
      try {
         cb = gateway_.submit(ctx_, deleteCommands);
         cb.loop(10, 500);
      } catch (Throwable e) { // Receive an exception means failure.
         return false;
      }
      
      return true;
   }
   
   public boolean deleteDataset(final Long idDataset) {
      return deleteObject(idDataset, "Dataset");
   }
   
   public boolean deleteImage(final Long idImage) {
      return deleteObject(idImage, "Image");
   }
   
   public void saveImage(final Long idDataset, final Sequence sequence) throws Exception {
      final IPixelsPrx proxy = gateway_.getPixelsService(ctx_);
      final List<IObject> supportedTypes = proxy.getAllEnumerations(PixelsType.class.getName());
      final PixelsType type = DataUtils.getOMEDataTypeFromIcy(sequence.getDataType_(), supportedTypes);
      
      if (type == null) {
         throw new Exception("Pixels Type not valid.");
      }
      
      // Create new image.
      final List<Integer> channels = new ArrayList<>();
      for ( int i = 0 ; i < sequence.getSizeC(); i++ ) {
         channels.add(i);
      }
      
      final RLong idNew = proxy.createImage(sequence.getSizeX(),
            sequence.getSizeY(), sequence.getSizeZ(), sequence.getSizeT(),
            channels, type, sequence.getName(), "Image saved from ICY.");
      
      if ( idNew == null ) {
         System.out.println("Error");
      }
      
      final client client = new client(credentials_.getServer().getHostname(), 
            credentials_.getServer().getPort());
      
      client.createSession(credentials_.getUser().getUsername(), 
            credentials_.getUser().getPassword());
      
      // if you want to have the data transfer encrypted then you can
      // use the entry variable otherwise use the following
      final client unsecureClient = client.createClient(false);
      final ServiceFactoryPrx entryUnencrypted = unsecureClient.getSession();
      final IContainerPrx proxyCS = entryUnencrypted.getContainerService();
      final List<omero.model.Image> results = proxyCS.getImages(
            omero.model.Image.class.getName(), Arrays.asList(idNew.getValue()), 
            new ParametersI());
      
      final ImageData newImage = new ImageData(results.get(0));
      // Link the new image and the dataset hosting the source image.
      DatasetImageLink link = new DatasetImageLinkI();
      link.setParent(new DatasetI(idDataset, false));
      link.setChild(new ImageI(newImage.getId(), false));
      gateway_.getUpdateService(ctx_).saveAndReturnObject(link);
      
      final RawPixelsStorePrx store = gateway_.getPixelsStore(ctx_);
      
      store.setPixelsId(newImage.getDefaultPixels().getId(), false);
      
      for (int z = 0; z < sequence.getSizeZ(); z++) {
         for (int t = 0; t < sequence.getSizeT(); t++) {
            for ( int c = 0 ; c < sequence.getSizeC(); c++) {
               // TODO: Please try to move the low level bit logic to the exporter.!.
               final int bitWidth = type.getBitSize().getValue();
               switch(bitWidth) {
               case Byte.SIZE:
               {
                  store.setTile(sequence.getDataXYAsByte(t, z, c), z, c, t,
                        0, 0, sequence.getWidth(), sequence.getHeight());
               }
               break;
               
               case Short.SIZE:
               {
                  short shorts[] = sequence.getDataXYAsShort(t, z, c);
                  ByteBuffer bb = ByteBuffer.allocate(shorts.length*Short.BYTES);
                  bb.asShortBuffer().put(shorts);
                  store.setTile(bb.array(), z, c, t, 0, 0, sequence.getWidth(), sequence.getHeight());
               }
               
               break;
               
               case Integer.SIZE:
               {
                  ByteBuffer bb = null;
                  switch(type.getValue().getValue()) {
                  case "float":
                     float floats[] = sequence.getDataXYAsFloat(t, z, c);
                     bb = ByteBuffer.allocate(floats.length*Integer.BYTES);
                     bb.asFloatBuffer().put(floats);
                     break;
                     
                  case "double":
                     double doubles[] = sequence.getDataXYAsDouble(t, z, c);
                     bb = ByteBuffer.allocate(doubles.length*Integer.BYTES);
                     bb.asDoubleBuffer().put(doubles);
                     break;
                     
                  case "int":
                  case "uint":
                     int integers[] = sequence.getDataXYAsInt(t, z, c);
                     bb = ByteBuffer.allocate(integers.length*Integer.BYTES);
                     bb.asIntBuffer().put(integers);
                     break;
                  }
                  
                  store.setTile(bb.array(), z, c, t, 0, 0, sequence.getWidth(),
                        sequence.getHeight());
               }
               
               break;
               
               default:
                  throw new Exception("Image format is not supported by the plugin.");
               }
            }
         }
      }
      
      store.save();
      store.close();
      
      // TODO: Store the original metadata in the image. I did not try a way to do this.
      client.closeSession();
      
      if (unsecureClient != null) { 
         unsecureClient.closeSession();
      }
   }
   
   /**
    * TODO: Move this code a FileSequenceImporter interface to follow the design
    * of Icy.
    */
   
   private String createFileSetQuery()
   {
       StringBuffer buffer = new StringBuffer();
       buffer.append("select fs from Fileset as fs ");
       buffer.append("join fetch fs.images as image ");
       buffer.append("left outer join fetch fs.usedFiles as usedFile ");
       buffer.append("join fetch usedFile.originalFile as f ");
       buffer.append("join fetch f.hasher ");
       buffer.append("where image.id in (:imageIds)");
       return buffer.toString();
   }
   
   private List<Path> downloadImage(SecurityContext context, ImageData image,
         final Path outputPath) throws DSAccessException,
     DSOutOfServiceException {
      
      final List<Path> paths = new ArrayList<>();
      String query;
      List<?> filesets;
      try {
         IQueryPrx service = gateway_.getQueryService(context);
         ParametersI param = new ParametersI();
         long id;
         
         if ( image.isFSImage() ) {
            id = image.getId();
            List<RType> l = new ArrayList<>();
            l.add(omero.rtypes.rlong(id));
            param.add("imageIds", omero.rtypes.rlist(l));
            query = createFileSetQuery();
         }
         
         else {//Prior to FS
            if (image.isArchived()) {
               StringBuffer buffer = new StringBuffer();
               id = image.getDefaultPixels().getId();
               buffer.append("select ofile from OriginalFile as ofile ");
               buffer.append("join fetch ofile.hasher ");
               buffer.append("left join ofile.pixelsFileMaps as pfm ");
               buffer.append("left join pfm.child as child ");
               buffer.append("where child.id = :id");
               param.map.put("id", omero.rtypes.rlong(id));
               query = buffer.toString();
            } else return null;
         }
         filesets = service.findAllByQuery(query, param);
      } catch (Exception e) {
         throw new DSAccessException("Cannot retrieve original file", e);
      }
      
      // if (CollectionUtils.isEmpty(filesets)) {
      if ( filesets.size() == 0 ) {
         return paths;
      }
      
      Iterator<?> i;
      final List<OriginalFile> values = new ArrayList<>();
      
      if (image.isFSImage()) {
         i = filesets.iterator();
         Fileset set;
         List<FilesetEntry> entries;
         Iterator<FilesetEntry> j;
         
         while (i.hasNext()) {
            set = (Fileset) i.next();
            entries = set.copyUsedFiles();
            j = entries.iterator();
            
            while (j.hasNext()) {
               FilesetEntry fs = j.next();
               values.add(fs.getOriginalFile());
            }
         }
      } else {
         values.addAll((List<OriginalFile>) filesets);
      }
      
      RawFileStorePrx store = null;
      OutputStream stream = null;
      long size = 0;
      long offset = 0;
      i = values.iterator();
      
      while (i.hasNext()) {
         OriginalFile of = (OriginalFile) i.next();
         
         try {
            store = gateway_.getRawFileService(context);
            store.setFileId(of.getId().getValue());
            paths.add(outputPath);
            stream = Files.newOutputStream(outputPath, StandardOpenOption.CREATE);
            size = of.getSize().getValue();
            
            try {
               try {
                  for (offset = 0; (offset + INC) < size;) {
                     stream.write(store.read(offset, INC));
                     offset += INC;
                  }
               } finally {
                  stream.write(store.read(offset, (int) (size - offset)));
                  stream.close();
               }
            }
            
            catch (Exception e) {
               if (stream != null) {
                  stream.close();
               }
               
               if (outputPath != null) {
                  Files.delete(outputPath);
                  paths.remove(outputPath);
               }
            }
         }
         
         catch (IOException e) {
            if (outputPath != null) {
               try {
                  Files.delete(outputPath);
               } catch (IOException e1) {
                  throw new DSAccessException("Cannot delete imagePath", e);
               }
               
               paths.remove(outputPath);
            }
            
            throw new DSAccessException("Cannot create file in imagePath", e);
         }
         
         catch (Throwable t) {
            throw new DSAccessException("ServerError on retrieveArchived", t);
         }
         
         finally {
            try {
               store.close();
            }
            
            catch (ServerError e) {
            }
         }
      }
      
      return paths;
   }
   
   /**
    * TODO: try to move a portion of code to a partial generalization 
    * FileSequenceImporter or something similar on icy. 
    */
   
   public Sequence downloadImage(final Long idImage)
         throws ExecutionException, DSAccessException, DSOutOfServiceException {
      
      final ImageData imageData = getImageData(idImage);
      final String filePath = FileUtil.getTempDirectory() + imageData.getName();
      final Path imagePath = Paths.get(filePath);
      final String fileBaseDirectory = FileUtil.getDirectory(filePath);
      
      FileUtil.delete(new File(fileBaseDirectory), true);
      FileUtil.createDir(fileBaseDirectory);
      
      try {
         // LociImporterPlugin plugImporter = new LociImporterPlugin();
         Files.createFile(imagePath);
         downloadImage(ctx_, imageData, imagePath);
         return Loader.loadSequence(imagePath.toString(), 0, false);
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   /**
    * Alternate way to do download (not used.)
    * 
    * based on project: https://github.com/bramalingam/OmeroICY/
    * I let it here just to put the implementation code in somewhere, but is not used.
    * 
    * @param idImage
    * @return
    * @throws ExecutionException
    * @throws DSAccessException
    * @throws DSOutOfServiceException
    */
   
   public Sequence donwloadImageAlt(final Long idImage) 
      throws ExecutionException, DSAccessException, DSOutOfServiceException {
      
      StringBuffer buffer = new StringBuffer();
      
      buffer.append("location=[OMERO] open=[omero:server=");
      buffer.append(credentials_.getServer().getHostname());
      buffer.append("\nuser=");
      buffer.append(credentials_.getUser().getUsername());
      buffer.append("\nport=");
      buffer.append(credentials_.getServer().getPort());
      buffer.append("\npass=");
      buffer.append(credentials_.getUser().getPassword());
      buffer.append("\ngroupID=");
      buffer.append(getCurrentGroupId());
      buffer.append("\niid=");
      buffer.append(idImage);
      buffer.append("]");
      buffer.append(" windowless=true ");
      
      IJ.runPlugIn("loci.plugins.LociImporter", buffer.toString());
      
      return null;
//      LociImporter importer = new LociImporter();
//      importer.run(buffer.toString());
   }
   
   // TODO: Refactor this function using another similar function and get just two generalized functions.
   
   public void upload(final Long idDataset, final String filePath, CancelableProgressFrame progress) {
      
      final String[] paths = new String[] { filePath };
      final Sequence sequence = Loader.loadSequence(filePath, 0, false);
      ImportConfig config = new ome.formats.importer.ImportConfig();
      
      config.email.set("");
      config.sendFiles.set(true);
      config.sendReport.set(false);
      config.contOnError.set(false);
      config.debug.set(false);
      
      config.hostname.set(credentials_.getServer().getHostname());
      config.port.set(credentials_.getServer().getPort());
      config.username.set(credentials_.getUser().getUsername());
      config.password.set(credentials_.getUser().getPassword());
      
      config.targetClass.set("omero.model.Dataset");
      config.targetId.set(idDataset);
      
      final List<Long> ids = new ArrayList<>();
      
      LoggingImportMonitor monitor = new LoggingImportMonitor() {
         @Override
         public void update(IObservable importLibrary, ImportEvent event) {
            
            if (event instanceof ImportEvent.IMPORT_DONE) {
               List<Pixels> pixels = ((ImportEvent.IMPORT_DONE) event).pixels;
               if ( pixels.size() > 0 ) {
                  ids.add(pixels.get(0).getImage().getId().getValue());
               }
            }
            
            String log = event.toLog();
            if ( log.contains("FILE_UPLOAD_BYTES") ) {
               String[] s = log.split(" ");
               
               if ( s.length > 4 ) {
                  double pos = Double.parseDouble(s[2]);
                  double max = Double.parseDouble(s[4]);
                  
                  if ( max != 0 ) {
                     progress.setPosition(pos*100.0/max);
                  }
               }
            }
            
            super.update(importLibrary, event);
         }
      };
      
      try {
         final OMEROMetadataStoreClient store = config.createStore();
         store.logVersionInfo(config.getIniVersionNumber());
         final OMEROWrapper reader = new OMEROWrapper(config);
         final ImportLibrary library = new ImportLibrary(store, reader);
         final ErrorHandler handler = new ErrorHandler(config);
         library.addObserver(monitor);
         final ImportCandidates candidates = new ImportCandidates(reader, paths, handler);
         
         reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));
         library.importCandidates(config, candidates);
         store.logout();
         
         // Save RoiS!
         final List<icy.roi.ROI> rois = sequence.getROIs();
         
         if ( ids.size() > 0 ) {
            final Long idImage = ids.get(0);
            
            for ( icy.roi.ROI roi : rois ) {
               addRoi(idImage, roi);
            }
         }
         
         progress.close();
         
      } catch (Exception e1) {
         e1.printStackTrace();
      }
   }
   
   // TODO: We could indicate the different phases and progress.
   // Now it is just showing the upload progress.
   public Long upload(final Long idDataset, final Sequence sequence,
         final CancelableProgressFrame progress)
         throws ExecutionException, DSAccessException, DSOutOfServiceException {
      
      final String basePath = FileUtil.getTempDirectory();
      
      Long ret = -1L;
      if ( sequence.getFilename() == null ) {
         if ( FileUtil.getFileExtension(sequence.getName(), false).equals(sequence.getName()) ) {
            File f = new File(basePath+sequence.getName()+".tif");
            Saver.save(sequence, f, false, false);
            sequence.setFilename(sequence.getName() + ".tif");
         } else {
            File f = new File(basePath+sequence.getName());
            Saver.save(sequence,  f,  false, false);
            sequence.setFilename(sequence.getName());
         }
      }
      
      final File openedFile = new File(sequence.getFilename());
      
      final String filePath = basePath + openedFile.getName();
      String fileBaseDirectory = FileUtil.getDirectory(filePath);
      final File file = new File(filePath);
      final String ext = FileUtil.getFileExtension(filePath, false);
      
      if (fileBaseDirectory.endsWith("/")) {
         fileBaseDirectory = fileBaseDirectory.substring(0, 
               fileBaseDirectory.length()-1);
      }
      
      FileUtil.createDir(fileBaseDirectory);
      Saver.save(sequence, file, false, false);
      
      final String[] paths = new String[] { sequence.getFilename() };
      ImportConfig config = new ome.formats.importer.ImportConfig();
      
      config.email.set("");
      config.sendFiles.set(true);
      config.sendReport.set(false);
      config.contOnError.set(false);
      config.debug.set(false);
      
      config.hostname.set(credentials_.getServer().getHostname());
      config.port.set(credentials_.getServer().getPort());
      config.username.set(credentials_.getUser().getUsername());
      config.password.set(credentials_.getUser().getPassword());
      
      config.targetClass.set("omero.model.Dataset");
      config.targetId.set(idDataset);
      
      final List<Long> ids = new ArrayList<>();
      
      LoggingImportMonitor monitor = new LoggingImportMonitor() {
         @Override
         public void update(IObservable importLibrary, ImportEvent event) {
            
            if (event instanceof ImportEvent.IMPORT_DONE) {
               List<Pixels> pixels = ((ImportEvent.IMPORT_DONE) event).pixels;
               if ( pixels.size() > 0 ) {
                  ids.add(pixels.get(0).getImage().getId().getValue());
               }
            }
            
            String log = event.toLog();
            if ( log.contains("FILE_UPLOAD_BYTES") ) {
               String[] s = log.split(" ");
               
               if ( s.length > 4 ) {
                  double pos = Double.parseDouble(s[2]);
                  double max = Double.parseDouble(s[4]);
                  
                  if ( max != 0 ) {
                     if ( progress != null )
                        progress.setPosition(pos*100.0/max);
                  }
               }
            }
            
            super.update(importLibrary, event);
         }
      };
      
      try {
         final OMEROMetadataStoreClient store = config.createStore();
         store.logVersionInfo(config.getIniVersionNumber());
         final OMEROWrapper reader = new OMEROWrapper(config);
         final ImportLibrary library = new ImportLibrary(store, reader);
         final ErrorHandler handler = new ErrorHandler(config);
         library.addObserver(monitor);
         final ImportCandidates candidates = new ImportCandidates(reader, paths, handler);
         
         if ( ext.equals("png")) {
            progress.close();
            new FailedAnnounceFrame("PNG images can not be uploaded in this version.");
            return ret;
            
         } else {
            reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));
         }
         
         library.importCandidates(config, candidates);
         
         store.logout();
         FileUtil.delete(fileBaseDirectory, false);
         
         // Save RoiS!
         final List<icy.roi.ROI> rois = sequence.getROIs();
         
         if ( ids.size() > 0 ) {
            Long idImage = ids.get(0);
            
            for ( icy.roi.ROI roi : rois ) {
               addRoi(idImage, roi);
            }
            
            ret = idImage;
         }
         
         if ( progress != null )
            progress.close();
         
      } catch (Exception e1) {
         e1.printStackTrace();
      }
      
      return ret;
   }
   
   public boolean createDataset(final Long idProject, final String name, 
         final String description)
      throws CannotCreateSessionException, PermissionDeniedException, ServerError {
      
      final DatasetData datasetData = new DatasetData();
      final ProjectDatasetLink link = new ProjectDatasetLinkI();
      
      datasetData.setName(name);
      datasetData.setDescription(description);
      link.setChild(datasetData.asDataset());
      link.setParent(new ProjectI(idProject, false));
      
      final client cl = new client(credentials_.getServer().getHostname(),
            credentials_.getServer().getPort());
      
      cl.createSession(credentials_.getUser().getUsername(),
            credentials_.getUser().getPassword());
      
      // if you want to have the data transfer encrypted then you can
      // use the entry variable otherwise use the following
      final client unsecureClient = cl.createClient(false);
      final ServiceFactoryPrx entryUnencrypted = unsecureClient.getSession();
      final IObject r = entryUnencrypted.getUpdateService().saveAndReturnObject(link);
      
      if ( r != null ) {
         return true;
      }
      
      cl.closeSession();
      
      if (unsecureClient != null) {
         unsecureClient.closeSession();
      }
      
      return false;
   }
   
   public List<Annotation> getAnnotation(Long id) {
      final client cl = new client(credentials_.getServer().getHostname(),
            credentials_.getServer().getPort());
      
      try {
         cl.createSession(credentials_.getUser().getUsername(), credentials_.getUser().getPassword());
         final client unsecureClient = cl.createClient(false);
         final ServiceFactoryPrx factory = unsecureClient.getSession();
         factory.sharedResources();
         final IMetadataPrx metadata = factory.getMetadataService();
         final List<Long> annotationId = new ArrayList<>();
         annotationId.add(id);
         final List<Annotation> ret = metadata.loadAnnotation(annotationId);
         
         cl.closeSession();
         
         if (unsecureClient != null) {
            unsecureClient.closeSession();
         }
         
         return ret;
      } catch (CannotCreateSessionException | PermissionDeniedException | ServerError e) {
         e.printStackTrace();
      }
      
      return new ArrayList<>();
   }
   
   public boolean renameImage(Long idImage, String name) {
      
      try {
         final BrowseFacility browser = gateway_.getFacility(BrowseFacility.class);
         final DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         final ImageData imageData = browser.getImage(ctx_, idImage);
         
         if ( imageData != null ) {
            if ( imageData.getName().equals(name) ) {
               return false;
            }
            
            imageData.setName(name);
            ImageData returned = (ImageData) dm.saveAndReturnObject(ctx_, imageData);
            return (returned != null && returned.getName().equals(name));
         }
      } catch (final ExecutionException e) {
         e.printStackTrace();
      } catch (DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      
      return false;
   }
   
   public boolean renameProject(Long selectedProject, String name) {
      try {
         final BrowseFacility browser = gateway_.getFacility(BrowseFacility.class);
         final DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         final List<Long> ids = new ArrayList<>();
         ids.add(selectedProject);
         final Collection<ProjectData> projects = browser.getProjects(ctx_, ids);
         
         if ( projects.size() > 0 ) {
            ProjectData project = projects.iterator().next();
            
            if ( project.getName().equals(name) ) {
               return false;
            }
            
            project.setName(name);
            ProjectData returned = (ProjectData) dm.saveAndReturnObject(ctx_, project);
            return (returned != null && returned.getName().equals(name));
         }
      } catch (final ExecutionException e) {
         e.printStackTrace();
      } catch (DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      
      return false;
   }
   
   public boolean createProject(ExperimenterI experimenter, String name, String description) {
      boolean ret = false;
      
      try {
         final ProjectI project = new ProjectI();
         project.setName(omero.rtypes.rstring(name));
         project.setDescription(omero.rtypes.rstring(description));
         final IObject robj = gateway_.getUpdateService(ctx_, 
            experimenter.getOmeName().getValue()).saveAndReturnObject(project);
         ret = (robj != null);
      } catch (final DSOutOfServiceException | ServerError e) {
         // e.printStackTrace();
         ret = false;
      }
      
      return ret;
   }
   
   public boolean changeDescriptionDataset(Long selectedDataset, String description) {
      
      try {
         final BrowseFacility browser = gateway_.getFacility(BrowseFacility.class);
         final DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         final List<Long> ids = new ArrayList<>();
         ids.add(selectedDataset);
         final Collection<DatasetData> datasets = browser.getDatasets(ctx_, ids);
         
         if ( datasets.size() > 0 ) {
            final DatasetData dataset = datasets.iterator().next();
            
            if ( dataset.getDescription().equals(description) ) {
               return false;
            }
            
            dataset.setDescription(description);
            final DatasetData returned = (DatasetData) dm.saveAndReturnObject(ctx_, dataset);
            return (returned != null && returned.getDescription().equals(description));
         }
      } catch (final ExecutionException e) {
         e.printStackTrace();
      } catch (DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      
      return false;
   }
   
   public boolean changeDescriptionProject(Long selectedProject, String description) {
      
      try {
         final BrowseFacility browser = gateway_.getFacility(BrowseFacility.class);
         final DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         final List<Long> ids = new ArrayList<>();
         ids.add(selectedProject);
         final Collection<ProjectData> projects = browser.getProjects(ctx_, ids);
         
         if ( projects.size() > 0 ) {
            final ProjectData project = projects.iterator().next();
            
            if ( project.getDescription().equals(description) ) {
               return false;
            }
            
            project.setDescription(description);
            final ProjectData returned = (ProjectData) dm.saveAndReturnObject(ctx_, 
                  project);
            
            return (returned != null && returned.getDescription().equals(description));
         }
      } catch (final ExecutionException e) {
         e.printStackTrace();
      } catch (DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      
      return false;
   }
   
   public boolean changeDescriptionImage(Long selectedImage, String description) {
      
      try {
         final BrowseFacility browser = gateway_.getFacility(BrowseFacility.class);
         final DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         final ImageData image = browser.getImage(ctx_, selectedImage);
         
         if (image != null) {
            if ( image.getDescription().equals(description) ) {
               return false;
            }
            image.setDescription(description);
            final ImageData returned = (ImageData) dm.saveAndReturnObject(ctx_, 
                  image);
            
            return (returned != null && returned.getDescription().equals(description));
         }
      } catch (final ExecutionException e) {
         e.printStackTrace();
      } catch (DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      
      return false;
   }
   
   public DatasetData getDataset(Long idDataset) {
      DatasetData ret = null;
      
      try {
         final ParametersI param = new ParametersI();
         param.exp(omero.rtypes.rlong(currentUserData_.getId()));
         param.allGrps();
         param.allExps();
         param.leaves();
         param.grp(omero.rtypes.rlong(-1L));
         final List<Long> datasets = new ArrayList<>();
         datasets.add(idDataset);
         final Collection<DatasetData> d = getBrowser().getDatasets(ctx_, datasets);
         
         if ( d.size() > 0 ) {
            ret = d.iterator().next();
         }
      } catch (DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      } catch (OMEClientException e) {
         e.printStackTrace();
      }
      
      return ret;
   }
   
   public ExperimenterData getExprimenterData(String username) {
      try {
         return gateway_.getUserDetails(ctx_, username);
      } catch (DSOutOfServiceException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   public Experimenter getExperimenter(Long idExperimenter) {
      try {
         return gateway_.getAdminService(ctx_).getExperimenter(idExperimenter);
      } catch (ServerError | DSOutOfServiceException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   protected byte[] concatenateByteArrays(byte[] a, byte[] b) {
      if ( a == null ) {
         return b;
      }
      
      byte[] result = new byte[a.length + b.length]; 
      System.arraycopy(a, 0, result, 0, a.length); 
      System.arraycopy(b, 0, result, a.length, b.length); 
      return result;
   } 
   
   public OMEXMLMetadata getImageMetadata(Long idImage) {
      try {
         ExporterPrx e = gateway_.getExporterService(ctx_);
         e.addImage(idImage);
         e.generateXml();
         long read = 0;
         int chunkSize = 1024;
         byte[] buf = null;
         
         while(true) {
            byte[] b = e.read(read, chunkSize);
            buf = concatenateByteArrays(buf, b);
            
            if ( b.length < chunkSize ) {
               break;
            }
            
            read += b.length;
         }
         
         e.close();
         String xml = new String(buf, Charset.forName("UTF-8") );
         String xmlSanitized = XMLTools.sanitizeXML(xml);
         
         OMEXMLServiceImpl impl = new OMEXMLServiceImpl();
         try {
            OMEXMLMetadata meta = impl.createOMEXMLMetadata(xmlSanitized);
            return meta;
         } catch (ServiceException e1) {
            System.out.println("Metadata not found or malformed in the server-side.");
            // e1.printStackTrace(); missing this just for at moment.
         }
      } catch (DSOutOfServiceException | ServerError e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   public OMEXMLMetadata getOriginalImageMetadata(Long idImage) {
      // Not implemented yet!... 
      // there are not a way to do this directly, but this functionality still in 
      // the Omero's TODO list!. 
      // One way, completely not recommended, is to use the original image and 
      // retrieve from it the original meta-data.
      return null;
   }
   
   public boolean writeImageToOutputStream(Long idImage, OutputStream outputStream, boolean showProgress) {
      return writeImageToOutputStream(getImageData(idImage), outputStream, showProgress);
   }
   
   public boolean writeImageToOutputStream(ImageData image, OutputStream outputStream, boolean showProgress) {
      FileFrame progress = null;
      
      if ( image == null ) {
         return false;
      }
      
      if ( showProgress ) {
         progress = new FileFrame("Downloading", image.getName());
      }
      
      String query;
      List<?> filesets;
      try {
         IQueryPrx service = gateway_.getQueryService(ctx_);
         ParametersI param = new ParametersI();
         long id;
         
         if ( image.isFSImage() ) {
            id = image.getId();
            List<RType> l = new ArrayList<>();
            l.add(omero.rtypes.rlong(id));
            param.add("imageIds", omero.rtypes.rlist(l));
            query = createFileSetQuery();
         }
         
         else {//Prior to FS
            if (image.isArchived()) {
               StringBuffer buffer = new StringBuffer();
               id = image.getDefaultPixels().getId();
               buffer.append("select ofile from OriginalFile as ofile ");
               buffer.append("join fetch ofile.hasher ");
               buffer.append("left join ofile.pixelsFileMaps as pfm ");
               buffer.append("left join pfm.child as child ");
               buffer.append("where child.id = :id");
               param.map.put("id", omero.rtypes.rlong(id));
               query = buffer.toString();
            } else {
               
               if ( showProgress ) {
                  progress.close();
               }
               
               return false;
            }
         }
         
         filesets = service.findAllByQuery(query, param);
      } catch (Exception e) {
         System.out.println("#1: " + e.getMessage());
         return false;
      }
      
      // if (CollectionUtils.isEmpty(filesets)) {
      if ( filesets.size() == 0 ) {
         if ( showProgress ) {
            progress.close();
         }
         
         return false;
      }
      
      Iterator<?> i;
      List<OriginalFile> values = new ArrayList<>();
      if (image.isFSImage()) {
         i = filesets.iterator();
         Fileset set;
         List<FilesetEntry> entries;
         Iterator<FilesetEntry> j;
         while (i.hasNext()) {
            set = (Fileset) i.next();
            entries = set.copyUsedFiles();
            j = entries.iterator();
            while (j.hasNext()) {
               FilesetEntry fs = j.next();
               values.add(fs.getOriginalFile());
            }
         }
      } else {
         values.addAll((List<OriginalFile>) filesets);
      }
      
      RawFileStorePrx store = null;
      long size = 0;
      long offset = 0;
      i = values.iterator();
      
      while (i.hasNext()) {
         final OriginalFile of = (OriginalFile) i.next();
         
         try {
            store = gateway_.getRawFileService(ctx_);
            store.setFileId(of.getId().getValue());
            size = of.getSize().getValue();
            
            try {
               for (offset = 0; (offset + INC) < size;) {
                  if ( progress != null) {
                     if ( progress.isCancelRequested() ) {
                        progress.close();
                        return false;
                     }
                     
                     progress.setPosition(offset*100/size);
                  }
                  
                  outputStream.write(store.read(offset, INC));
                  offset += INC;
               }
            } finally {
               outputStream.write(store.read(offset, (int) (size - offset)));
               outputStream.close();
            }
            
            store.close();
            
            if ( progress != null ) {
               progress.close();
            }
         }
         
         catch (Exception e) {
            if (outputStream != null) {
               try {
                  outputStream.close();
               } catch (IOException e1) {
                  e1.printStackTrace();
               }
               
               if ( showProgress ) {
                  progress.close();
               }
               
               return false;
            }
         }
         
         return true; // we consider just the first element.
      }
      
      if ( showProgress ) {
         progress.close();
      }
      
      return false;
   }
   
   public Collection<DatasetData> getAllDatasetsByExperimenter(long experimenterId) {
      
      try {
         final ParametersI param = new ParametersI();
         param.exp(omero.rtypes.rlong(currentUserData_.getId()));
         param.allGrps();
         param.allExps();
         param.leaves();
         param.grp(omero.rtypes.rlong(-1L));
         param.orphan();
         
         return getBrowser().getDatasets(ctx_, experimenterId);
         
      } catch (DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      } catch (OMEClientException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   public Collection<ImageData> getOrphanedImages(long userID) {
     try {
         IQueryPrx svc = gateway_.getQueryService(ctx_);
         StringBuilder sb = new StringBuilder();
         sb.append("select img from Image as img ");
         sb.append("left outer join fetch img.details.owner ");
         sb.append("left outer join fetch img.pixels as pix ");
         sb.append("left outer join fetch pix.pixelsType as pt ");
         sb.append("where not exists (select obl from "
                 + "DatasetImageLink as obl where obl.child = img.id)");
         sb.append(" and not exists (select ws from WellSample as "
                 + "ws where ws.image = img.id)");
         ParametersI param = new ParametersI();
         if (userID >= 0) {
             sb.append(" and img.details.owner.id = :userID");
             param.addLong("userID", userID);
         }
         
         final Map<IObject, IObject> map = new HashMap<>();
         final List<IObject> lst = svc.findAllByQuery(sb.toString(), param);
         
         for ( IObject obj : lst ) 
            map.put(obj, null);
         
         return PojoMapper.asDataObjects(map).keySet();
         
     } catch (Throwable t) {
        System.out.println("Could not load orphaned images" + t.getMessage());
     }
     
     return Collections.emptySet();
   }
   
   public boolean createDatasetAtUserHome(long experimenterId, String name, String description) 
         throws CannotCreateSessionException, PermissionDeniedException, ServerError {
      final DatasetI dataset = new DatasetI();
      dataset.setName(omero.rtypes.rstring(name));
      dataset.setDescription(omero.rtypes.rstring(description));
      
      final client cl = new client(credentials_.getServer().getHostname(), credentials_.getServer().getPort());
      cl.createSession(credentials_.getUser().getUsername(), credentials_.getUser().getPassword());
      
      // if you want to have the data transfer encrypted then you can
      // use the entry variable otherwise use the following
      final client unsecureClient = cl.createClient(false);
      final ServiceFactoryPrx entryUnencrypted = unsecureClient.getSession();
      final IObject r = entryUnencrypted.getUpdateService().saveAndReturnObject(dataset);
      
      if ( r != null ) {
         return true;
      }
      
      cl.closeSession();
      
      if (unsecureClient != null) {
         unsecureClient.closeSession();
      }
      
      return false;
   }
   
   public boolean linkImageToDataset(long idImage, long idDatasetTo)
         throws CannotCreateSessionException, PermissionDeniedException, ServerError {
      
      final ImageData image = getImageData(idImage);
      final DatasetData dataset = getDataset(idDatasetTo);
      final DatasetImageLinkI link = new DatasetImageLinkI();
      link.setParent(dataset.asDataset());
      link.setChild(image.asImage());
      
      final client cl = new client(credentials_.getServer().getHostname(), credentials_.getServer().getPort());
      cl.createSession(credentials_.getUser().getUsername(), credentials_.getUser().getPassword());
      
      final client unsecureClient = cl.createClient(false);
      final ServiceFactoryPrx entryUnencrypted = unsecureClient.getSession();
      final IObject r = entryUnencrypted.getUpdateService().saveAndReturnObject(link);
      
      if ( r != null ) {
         return true;
      }
      
      return false;
   }
   
   public boolean unlinkImageOfDataset(Long idImage, Long idDataset) 
         throws DSOutOfServiceException, ServerError, CannotCreateSessionException, PermissionDeniedException {
      
      final StringBuilder sb = new StringBuilder();
      sb.append("select i from Image i join fetch i.datasetLinks dil ");
      sb.append("where i.id = :id ");
      IQueryPrx query = gateway_.getQueryService(ctx_);
      ParametersI param = new ParametersI();
      param.addId(idImage);
      omero.model.Image image = (omero.model.Image) query.findByQuery(sb.toString(), param);
      
      for ( Dataset linkedDataset : image.linkedDatasetList() ) {
         if ( linkedDataset.getId().getValue() == idDataset ) {
            image.unlinkDataset(linkedDataset);
            final client cl = new client(credentials_.getServer().getHostname(), credentials_.getServer().getPort());
            cl.createSession(credentials_.getUser().getUsername(), credentials_.getUser().getPassword());
            final client unsecureClient = cl.createClient(false);
            final ServiceFactoryPrx entryUnencrypted = unsecureClient.getSession();
            final IObject r = entryUnencrypted.getUpdateService().saveAndReturnObject(image);
            
            if ( r != null ) {
               return true;
            }
         }
      }
      
      return false;
   }
   
   public boolean addRoi(Long idImage, icy.roi.ROI roi) 
         throws CannotCreateSessionException, PermissionDeniedException, ServerError, DSOutOfServiceException {
      
      final List<ROIData> rois = new ArrayList<>();
      final ROIData roiData = ROIUtils.IcyRoiToOmero(roi);
      
      if ( roiData == null ) {
         return false;
      }
      
      rois.add(roiData);
      
      try {
          Collection<ROIData> r = getRoiFacility().saveROIs(ctx_, idImage, getCurrentUserId(), rois);
          if ( r.size() > 0 ) {
             return true;
          }
      } catch (DSAccessException | OMEClientException e) {
         e.printStackTrace();
      }
      
      return false;
   }
   
   public ROI[] getRois(Long idImage) throws DSOutOfServiceException, DSAccessException, OMEClientException {
      List<ROIResult> results = getRoiFacility().loadROIs(ctx_, idImage);
      List<ROI> ret = new ArrayList<>();
      
      final GeometriesBuilder builder = new GeometriesBuilder();
      
      for ( final ROIResult result : results ) {
         final ImageData imageData = getImageData(idImage);
         int T = imageData.getDefaultPixels().getSizeT();
         int Z = imageData.getDefaultPixels().getSizeZ();
         
         for ( int t = 0; t < T ; t++ ) {
            for ( int z = 0; z < Z; z++ ) {
               for( final ROIData roiData : result.getROIs() ) {
                  List<ShapeData> shapes = roiData.getShapes(z, t);
                  
                  for ( ShapeData shape : shapes ) {
                     ret.add(builder.createShape(shape));
                  }
               }
            }
         }
      }
      
      return ret.toArray(new ROI[ret.size()]);
   }
   
   public Long getDatasetIdOfImage(Long idImage) {
      final ImageData imageData = getImageData(idImage);
      Long ret = -1L; 
      
      Iterator<DatasetData> iter = imageData.getDatasets().iterator();
      
      if ( iter.hasNext() ) {
         ret = iter.next().getId();
      }
      
      return ret;
   }
   
   public boolean isConnected() {
      boolean ret = false;
      try {
         return gateway_.isAlive(ctx_);
      } catch (DSOutOfServiceException e) {
         e.printStackTrace();
      }
      
      return ret;
   }
   
   public List<AnnotationData> getAnnotationsImageMap(Long idImage) {
      final ImageData imageData = getImageData(idImage);
      
      try {
         MetadataFacility mdf = gateway_.getFacility(MetadataFacility.class);
         // Test if exception is thrown when types are mixed.
         final List<DataObject> objs = new ArrayList<>();
         objs.add(imageData);
         final List<Class<? extends AnnotationData>> types = new ArrayList<>();
         types.add(MapAnnotationData.class);
         Map<DataObject, List<AnnotationData>> ans = mdf.getAnnotations(ctx_, objs, types, null);
         
         if ( ans.size() > 0 ) {
            return ans.get(imageData);
         }
         
      } catch (ExecutionException |DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   public boolean attachAnnotationToImage(MapAnnotationData map, Long idImage) {
      
      try {
         final ImageData imageData = this.getImageData(idImage);
         final DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         
         if ( dm.attachAnnotation(ctx_, map, imageData) != null ) {
            return true;
         }
         
      } catch (ExecutionException | DSOutOfServiceException | DSAccessException e1) {
         e1.printStackTrace();
      }
      
      return false;
   }
   
   public boolean attachFile(Long id, String filePath, String name, String notes) {
      ImageData image = getImageData(id);
      final File file = new File(filePath);
      final String filename = FileUtil.getFileName(filePath);
      final String path = filePath.substring(0, filePath.length()-filename.length());
      
      //create the original file object.
      OriginalFile originalFile = new OriginalFileI();
      originalFile.setName(omero.rtypes.rstring(filename));
      originalFile.setPath(omero.rtypes.rstring(path));
      originalFile.setSize(omero.rtypes.rlong(file.length()));
      
      final ChecksumAlgorithm checksumAlgorithm = new ChecksumAlgorithmI();
      checksumAlgorithm.setValue(omero.rtypes.rstring(ChecksumAlgorithmSHA1160.value));
      originalFile.setHasher(checksumAlgorithm);
      originalFile.setMimetype(omero.rtypes.rstring("application/octet-stream"));
      //Now we save the originalFile object
      
      try {
         DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         originalFile = (OriginalFile) dm.saveAndReturnObject(ctx_, originalFile);
         //Initialize the service to load the raw data
         RawFileStorePrx rawFileStore = gateway_.getRawFileService(ctx_);
         
         long pos = 0;
         int rlen;
         byte[] buf = new byte[INC];
         ByteBuffer bbuf;
         //Open file and read stream
         try (FileInputStream stream = new FileInputStream(file)) {
             rawFileStore.setFileId(originalFile.getId().getValue());
             while ((rlen = stream.read(buf)) > 0) {
                 rawFileStore.write(buf, pos, rlen);
                 pos += rlen;
                 bbuf = ByteBuffer.wrap(buf);
                 bbuf.limit(rlen);
             }
             originalFile = rawFileStore.save();
         } catch (FileNotFoundException e) {
            e.printStackTrace();
         } catch (IOException e) {
            e.printStackTrace();
         } catch (ServerError e) {
            e.printStackTrace();
         } finally {
            try {
               rawFileStore.close();
            } catch (ServerError e) {
               e.printStackTrace();
            }
         }
         
         //now we have an original File in DB and raw data uploaded.
         //We now need to link the Original file to the image using
         //the File annotation object. That's the way to do it.
         FileAnnotation fa = new FileAnnotationI();
         fa.setFile(originalFile);
         fa.setDescription(omero.rtypes.rstring(notes)); // The description set above e.g. PointsModel
         fa.setNs(omero.rtypes.rstring(name)); // The name space you have set to identify the file annotation.
         
         //save the file annotation.
         fa = (FileAnnotation) dm.saveAndReturnObject(ctx_, fa);
         
         //now link the image and the annotation
         ImageAnnotationLink link = new ImageAnnotationLinkI();
         link.setChild(fa);
         link.setParent(image.asImage());
         //save the link back to the server.
         link = (ImageAnnotationLink) dm.saveAndReturnObject(ctx_, link);
         // o attach to a Dataset use DatasetAnnotationLink;
         return true;
         
      } catch (ExecutionException | DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
         return false;
      }
   }
   
   public File getAttachment(Long id, String name) {
      final ImageData imageData = getImageData(id);
      
      try {
         MetadataFacility mdf = gateway_.getFacility(MetadataFacility.class);
         final List<DataObject> objs = new ArrayList<>();
         objs.add(imageData);
         final List<Class<? extends AnnotationData>> types = new ArrayList<>();
         types.add(FileAnnotationData.class);
         Map<DataObject, List<AnnotationData>> ans = mdf.getAnnotations(ctx_, objs, types, null);
         
         for (List<AnnotationData> adata : ans.values()) {
            for ( AnnotationData aadata : adata ) {
               if ( aadata.getNameSpace().equals(name) && aadata instanceof FileAnnotationData) {
                  FileAnnotationData fa = (FileAnnotationData) aadata;
                  RawFileStorePrx store = gateway_.getRawFileService(ctx_);
                  File file = File.createTempFile("temp-file-name_", FileUtil.getFileExtension(fa.getFileName(), true));
                  
                  try (FileOutputStream stream = new FileOutputStream(file)) {
                     store.setFileId(fa.getFileID());
                     int offset = 0;
                     long size = fa.getFileSize();
                     
                     try {
                        for (offset = 0; (offset+INC) < size;) {
                           stream.write(store.read(offset, INC));
                           offset += INC;
                        }
                     } finally {
                        stream.write(store.read(offset, (int) (size-offset)));
                     }
                     
                     return file;
                     
                  }  catch (ServerError e) {
                     e.printStackTrace();
                  }
               }
            }
         }
         
         return null;
         
      } catch (ExecutionException | DSOutOfServiceException | DSAccessException | IOException e) {
         e.printStackTrace();
      }
      
      return null;
   }
   
   public boolean deleteAttach(Long id, String name) {
      return false;
   }
   
   public boolean commentImage(Long idImage, String commentary) {
      try {
         DataManagerFacility dm = gateway_.getFacility(DataManagerFacility.class);
         CommentAnnotation comment = new CommentAnnotationI();
         comment.setTextValue(omero.rtypes.rstring(commentary));
         comment = (CommentAnnotation) dm.saveAndReturnObject(ctx_, comment);
         
         ImageAnnotationLink link = new ImageAnnotationLinkI();
         link.setParent(getImageData(idImage).asImage());
         link.setChild(comment);
         dm.saveAndReturnObject(ctx_, link);
         return true;
      
      } catch (ExecutionException | DSOutOfServiceException | DSAccessException e) {
         e.printStackTrace();
      }
      return false;
   }
}

