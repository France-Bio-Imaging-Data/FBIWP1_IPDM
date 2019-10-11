/**
 * 
 */
package plugins.ofuica.viewers;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListDataListener;

import icy.util.StringUtil;
import plugins.adufour.vars.gui.swing.SwingVarEditor;
import plugins.adufour.vars.lang.Var;
import plugins.ofuica.metro.Context;
import plugins.ofuica.metro.ContextEvent;
import plugins.ofuica.metro.ContextListener;
import plugins.ofuica.metro.Metro;
import plugins.ofuica.metro.Session;
import plugins.ofuica.metro.protocols.VarSession;

/**
 * @author osvaldo
 */

public final class SessionChooser extends SwingVarEditor<Session> {
   
   private final class JComboSessionBoxListener implements ActionListener
   {
       @Override
       public void actionPerformed(ActionEvent e)
       {
          final JComboBox<Object> jComboSequences = getEditorComponent();
          Object o = jComboSequences.getSelectedItem();
          
          if (o == variable.getValue()) 
             return;
          
          if (o == null) {
             variable.setValue(null);
          }
          
          else if (o == VarSession.NO_SESSION) {
             ((VarSession) variable).setValue(null);
          }
          
          else {
             Session newValue = (Session) o;
             jComboSequences.setToolTipText(newValue.getId());
             variable.setValue(newValue);
          }
      }
   }
   
   private final class ChooserContextListener implements ContextListener {
      @Override
      public void contextChanged(Context context, ContextEvent e) {
         getEditorComponent().repaint();
         getEditorComponent().updateUI();
      }
   }
   
   private JComboSessionBoxListener jComboSessionBoxListener_;
   private ChooserContextListener chooserContextListener_;
   
   private final class SessionChooserModel extends DefaultComboBoxModel<Object> {
      
      private static final long serialVersionUID = 1L;
      public SessionChooserModel() {
         setSelectedItem(VarSession.NO_SESSION);
      }
      
      @Override
      public int getSize() {
         // index 0 : no session.
         return  1 + Metro.getSessions(variable).size();
      }
      
      @Override
      public Object getElementAt(int index)
      {
          if (index <= 0) return VarSession.NO_SESSION;
          return Metro.getSessions(variable).get(index - 1);
      }
      
      @Override
      public void addListDataListener(ListDataListener l)
      {
          // don't register anything... 
      }
   }
   
   public SessionChooser(Var<Session> variable) {
      super(variable);
   }
   
   @Override
   protected JComponent createEditorComponent() {
      
      jComboSessionBoxListener_ = new JComboSessionBoxListener();
      chooserContextListener_ = new ChooserContextListener();
      
      final JComboBox<Object> jComboSessions = new JComboBox<>(new SessionChooserModel());
      
      jComboSessions.setRenderer(new ListCellRenderer<Object>()
      {
         public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index, 
               boolean isSelected, boolean cellHasFocus)
         {
            if (value == null || value.equals(VarSession.NO_SESSION) )
               return new JLabel(VarSession.NO_SESSION);
            
            if (value instanceof Session) {
               final String s = ((Session) value).getId();
               final JLabel label = new JLabel(StringUtil.limit(s, 24));
               return label;
            }
            
            throw new IllegalArgumentException(value.toString());
         }
      });
      
      return jComboSessions;
   }
   
   @Override
   protected void activateListeners() {
      getEditorComponent().addActionListener(jComboSessionBoxListener_);
      Metro.getContext(variable).addContextListener(chooserContextListener_);
   }
   
   @Override
   protected void deactivateListeners() {
      getEditorComponent().removeActionListener(jComboSessionBoxListener_);
      Metro.getContext(variable).removeContextListener(chooserContextListener_);
   }
   
   @Override
   protected void updateInterfaceValue() {
      
      if (variable.getReference() != null) {
         getEditorComponent().setSelectedItem(variable.getValue());
         getEditorComponent().repaint();
      }
      
      if ( variable.getValue() != null ) {
         getEditorComponent().setSelectedItem(variable.getValue());
         getEditorComponent().repaint();
      }
      
      getEditorComponent().setToolTipText("<html><pre><font size=3>" + variable.getValueAsString(true) + "</font></pre></html>");
   }
   
   @Override
   public JComboBox<Object> getEditorComponent() {
      return (JComboBox<Object>) super.getEditorComponent();
   }
   
   @Override
   public Dimension getPreferredSize() {
      Dimension dim = super.getPreferredSize();
      dim.height = 20;
      return dim;
   }
}
