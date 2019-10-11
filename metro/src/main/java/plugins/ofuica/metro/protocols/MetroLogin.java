/**
 * 
 */
package plugins.ofuica.metro.protocols;

import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.system.thread.ThreadUtil;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;
import plugins.adufour.vars.util.VarListener;
import plugins.adufour.vars.util.VarReferencingPolicy;
import plugins.ofuica.metro.Metro;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.client.Client;
import plugins.ofuica.metro.client.ClientException;

/**
 * Metro Login block.
 * 
 * Allows you to connect with remote server and also dispose a valid client instance.
 * 
 * @author osvaldo
 */

public final class MetroLogin extends Plugin implements Block {
   
   public enum ServicesType { OpenImadis, Omero }
   
   private VarEnum<ServicesType> service_ = null;
   private VarString url_ = new VarString("url", "cid.curie.fr:443");
   private VarString accessKey_ = new VarString("access key", "");
   private VarString user_ = new VarString("user", "");
   private VarString passwd_ = new VarString("password", "");
   private VarString sessionName_ = new VarString("session name", null);
   private VarSession session_ = new VarSession("session", null);
   
   private VarString status_ = null; // shortcut
   private Session sessionBackup_ = null;
   
   public MetroLogin() {
      super();
      
      // Very ugly hack. It should exist other good thing to do something similar.
      if ( !Icy.getMainInterface().isHeadLess() ) {
         
         ThreadUtil.bgRun(new Runnable() {
            @Override
            public void run() {
               ThreadUtil.invokeLater(new Runnable() {
                  @Override
                  public void run() {
                     Session session = null;
                     do {
                        ThreadUtil.sleep(100);
                     }
                     while( (session = Metro.createSession(url_)) == null);
                     sessionBackup_ = session;
                     session_.setValue(session);
                     status_ = session_.getValue().getStatus();
                     sessionName_.setValue(session_.getValue().getId());
                  }
               });
            }
         });
      } else {
         
         final Session session = Metro.createSession(url_);
         sessionBackup_ = session;
         session_.setValue(session);
         status_ = session_.getValue().getStatus();
         sessionName_.setValue(session_.getValue().getId());
      }
   }
   
   private void initServiceEnum(VarList inputMap) {
      
      user_.setOptional(true);
      passwd_.setOptional(true);
      accessKey_.setOptional(true);
      
      final VarListener<ServicesType> listener = new VarListener<ServicesType>() {
         @Override
         public void valueChanged(Var<ServicesType> source, ServicesType oldValue, ServicesType newValue) {
            if ( newValue == ServicesType.Omero ) {
               inputMap.add(user_.getName(), user_);
               inputMap.add(passwd_.getName(), passwd_);
               if (inputMap.contains(accessKey_))
                  inputMap.remove(accessKey_);
            }
            
            else if ( newValue == ServicesType.OpenImadis ) {
               if (inputMap.contains(user_) ) 
                  inputMap.remove(user_);
               if (inputMap.contains(passwd_))
                  inputMap.remove(passwd_);
               inputMap.add(accessKey_.getName(), accessKey_);
            }
            
            else {
               ;
            }
         }
         
         @Override
         public void referenceChanged(Var<ServicesType> source, Var<? extends ServicesType> oldReference,
               Var<? extends ServicesType> newReference) {
         }
      };
      
      service_ = new VarEnum<>("service", ServicesType.OpenImadis, listener);
      service_.setReferencingPolicy(VarReferencingPolicy.NONE);
      
      sessionName_.setReferencingPolicy(VarReferencingPolicy.NONE);
      sessionName_.addListener(new VarListener<String> () {
         
         @Override
         public void valueChanged(Var<String> source, String oldValue, String newValue) {
            session_.getValue().setId(newValue);
         }
         
         @Override
         public void referenceChanged(Var<String> source, Var<? extends String> oldReference,
               Var<? extends String> newReference) {
         }
      });
   }
   
   @Override
   public void declareInput(VarList inputMap) {
      initServiceEnum(inputMap);
      inputMap.add(service_.getName(), service_);
      inputMap.add(url_.getName(), url_);
      inputMap.add(accessKey_.getName(), accessKey_);
      inputMap.add(sessionName_.getName(), sessionName_);
   }
   
   @Override
   public void declareOutput(VarList outputMap) {
      outputMap.add(session_.getName(), session_);
   }
   
   private void validateInputVariables() {
      if ( url_.getValue().isEmpty() ) 
         throw new VarException(url_, "the url variable should be not empty.");
   }
   
   @Override
   public void run() {
      validateInputVariables();
      try {
         if ( service_.getValue().equals(ServicesType.OpenImadis) ) {
            
            if ( status_ != null )
               status_.setValue("connecting");
            
            final Client client = new plugins.ofuica.metro.openimadis.OpenImadisClient();
            boolean ret = client.login(url_.getValue(), accessKey_.getValue());
            
            if (ret) {
               final Session session = session_.getValue();
               
               if ( session == null ) {
                  final Session s = sessionBackup_;
                  if ( s == null )
                     throw new VarException(session_, "Error with the session. Open and close your protocol file.!.");
                  session_.setValue(s);
               }
               session_.getValue().setClient(client);
               
               if ( status_ != null )
                  status_.setValue("logged");
            }
         }
         
         else if ( service_.getValue().equals(ServicesType.Omero) ) {
            final Client client = new plugins.ofuica.metro.omero.OmeroClient();
            boolean ret = client.login(url_.getValue(), new String[] 
                  {user_.getValue(), passwd_.getValue()});
            
            if (ret) {
               final Session session = session_.getValue();
               if ( session == null ) {
                  final Session s = sessionBackup_;
                  if ( s == null )
                     throw new VarException(session_, "Error with the session. Open and close your protocol file.!.");
                  session_.setValue(s);
               }
               session_.getValue().setClient(client);
            }
         }
         
         else {
         }
         
      } catch (ClientException e) {
         e.printStackTrace();
      }
   }
   
   @Override
   protected void finalize() throws Throwable {
      Metro.destroySession(session_.getValue());
      super.finalize();
   }
}
