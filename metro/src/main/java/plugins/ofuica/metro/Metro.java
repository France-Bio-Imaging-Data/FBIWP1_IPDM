/**
 * 
 */
package plugins.ofuica.metro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.util.ReflectionUtil;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.protocols.gui.MainFrame;
import plugins.adufour.protocols.gui.ProtocolPanel;
import plugins.adufour.vars.lang.Var;

/**
 * The idea of this plug-in is to integrate, as much as possible, all kind of projects
 * related to Metrology science and generate performance's indicators of microscopes
 * automatically using very large bio-images data bases and finally dispose the information
 * available for the community in open data bases.
 * 
 * The defined workflow in fbi-metrology.pptx is going to be followed.
 * (Perrine Paul Gilloteaux, Ferreol Soulez, Fabrice de Chaumont, Cedric Matthews
 * Paris 2015 - France BioImaging)
 * 
 * All the process must be automatic to be doing in a transparent way to the user, but
 * also it could be launching manually by the advanced users.
 * 
 * One application could be:
 * ++++++++++++++++++++++++
 * 
 * Generating the performance's indicators for bio-images and relating
 * the obtained indicators with these bio-images, using the annotation system available in
 * several open data manage systems. (ex: openImadis, Omero, etc).
 * 
 * To do this a common interface "Client" has been defined which defines generic operations
 * universe and allows to separate each different implementation.
 * 
 * This plugin has defined several blocks that allow create a protocol in Icy and
 * be executed by an OpenImadis publisher, Omero or by a user by command line.
 * 
 * Protocol's blocks available:
 * 
 * 1) Metro Login
 * 2) Metro GetSequence
 * 3) Metro Upload
 * 4) Metro ROI
 * 5) Metro Annotate
 * 6) Metro Attach Sequence
 * 7) Metro Batch
 * 8) Metro Comment
 * 9) Metro Search
 * 10) Metro TimeChart
 * ...
 * You will find a description in each block class file.
 * 
 * Some code copied verbatim from code done by Perrine Paul Gilloteaux. (OpenImadisUtils)
 * 
 * @author osvaldo
 */

public class Metro {
   static final String MAIN_CONTEXT = "main_context";
   
   static private Map<String, Context> contexts_ = new HashMap<>();
   
   static public Context getContext(Var<?> var) {
      
      // This is only a pokayoke for avoid graphical bad-uses potencially by userland.
      
      if (!Icy.getMainInterface().isHeadLess()) {
         final String str = getBlockPanelUID(var);
         
         if ( contexts_.containsKey(str) ) {
            return contexts_.get(str);
         }
         
         final Context ctx = new Context(str);
         contexts_.put(str, ctx);
         return ctx;
      }
      
      if ( contexts_.containsKey(MAIN_CONTEXT) ) {
         return contexts_.get(MAIN_CONTEXT);
      }
      
      final Context ctx = new Context(MAIN_CONTEXT);
      contexts_.put(MAIN_CONTEXT, ctx);
      return ctx;
   }
   
   static public Session createSession(Var<?> var) {
      final Context ctx = getContext(var);
      
      if ( ctx == null ) 
         return null;
      
      return ctx.createSession();
   }
   
   static public void destroySession(Session session) {
      
      for ( Context ctx : contexts_.values() ) {
         ctx.destroySession(session);
      }
   }
   
   public static Session getSession(Var<?> var, String text) {
      final List<Session> sessions = getContext(var).getSessions();
      
      for ( Session session : sessions ) {
         if ( session.getId().equals(text) ) 
            return session;
      }
      
      return null;
   }
   
   public static List<Session> getSessions(Var<?> var) {
      return getContext(var).getSessions();
   }
   
   private static String getBlockPanelUID(Var<?> var) {
      final ArrayList<Plugin> plugins = Icy.getMainInterface().getActivePlugins();
      
      for (Plugin plugin : plugins ) {
         if ( plugin.getName().equals("Protocols") ) {
            
            try {
               final MainFrame mainFrame = (MainFrame) ReflectionUtil.getFieldObject(plugin , "mainFrame", true);
               final List<ProtocolPanel> listPanels = mainFrame.getProtocolPanels();
               
               for ( ProtocolPanel panel : listPanels ) {
                  final WorkFlow wf = panel.getWorkFlow();
                  
                  if ( wf.getInputOwner(var) != null ) {
                     return panel.getUI().toString();
                  }
                  
                  else if (wf.getOutputOwner(var) != null ) {
                     return panel.getUI().toString();
                  }
                  
                  else {
                  }
               }
            
            } catch (IllegalArgumentException | IllegalAccessException | 
                  SecurityException | NoSuchFieldException e) {
            }
         }
      }
      
      return "";
   }
   
}
