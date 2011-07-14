package edu.brown.cs.bubbles.bedu.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;

class BeduManageCoursesBubble extends BudaBubble
{
   private static final long serialVersionUID = 1L;
   private static final Dimension DEFAULT_DIMENSION = new Dimension(150, 100);
   private static final String ADD_COURSE_STR = "Add course";
   BeduManageCoursesBubble()
   {
     setContentPane(new ContentPane()); 
   }


   private class ContentPane extends JPanel implements ItemListener
   {
      private JComboBox combo;
      private ConfigPane cur_config_pane;
      
      private ContentPane()
      {
         setPreferredSize(DEFAULT_DIMENSION);
         BassRepository course_repo = BassFactory.getRepository(BudaConstants.SearchType.SEARCH_COURSES);
         setLayout(new BorderLayout());
         combo = new JComboBox();
         combo.addItemListener(this);
         add(combo, BorderLayout.PAGE_START); 
         
         for(BassName n : course_repo.getAllNames())
         {
            if(n.toString().charAt(0) != '@')
               combo.addItem(n);
         }
         
         combo.addItem(ADD_COURSE_STR);

      }
      
      @Override
      public void itemStateChanged(ItemEvent e)
      {
         if(cur_config_pane != null)
            remove(cur_config_pane);
         cur_config_pane = new ConfigPane((BeduCourse)e.getItem());
         add(cur_config_pane, BorderLayout.CENTER);
      }
   }

   private class ConfigPane extends JPanel
   {
      private JTextField username_field;
      public ConfigPane(BeduCourse c)
      {
         this.setBackground(Color.white);
         this.setBorder(new LineBorder(Color.PINK));
         setLayout(new GridBagLayout());
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.weightx = 0.1;
         gbc.weighty = 0.5;
         gbc.anchor = GridBagConstraints.WEST;
         add(new JLabel("Name: "), gbc);
         
         gbc = new GridBagConstraints();
         username_field = new JTextField();
         gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.gridx = 1;
         gbc.weightx = .9;
         gbc.weighty = 0.5;
         gbc.gridy = 0;
         add(username_field, gbc);
         
         gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.NONE;
         gbc.weightx = 0.1;
         gbc.weighty = 0.5;
         gbc.gridy = 1;
         gbc.gridx = 0;
         gbc.anchor = GridBagConstraints.WEST;
         add(new JLabel("TA Chat Username: "), gbc);
         
         gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.weightx = 0.9;
         gbc.weighty = 0.5;
         gbc.gridy = 1;
         gbc.gridx = 1;
         add(new JTextField(), gbc);
         
         if(c instanceof BeduCourse.TACourse)
         {
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.1;
            gbc.gridy = 2;
            gbc.gridx = 0;
            add(new JLabel("TA Password: "), gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.gridy = 2;
            gbc.gridx = 1;
            add(new JTextField(), gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.1;
            gbc.gridy = 3;
            gbc.gridx = 0;
            add(new JLabel("XMPP Server: "), gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.gridy = 3;
            gbc.gridx = 1;
            add(new JTextField(), gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.gridy = 4;
            gbc.gridx = 1;
            add(new JButton("Save"));
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.gridy = 4;
            gbc.gridx = 0;
            add(new JButton("Delete"));
         }
         else 
         {
            gbc = new GridBagConstraints();
            gbc.gridy = 2;
            gbc.weightx = 0.5;
            gbc.weighty = 0.5;
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.NONE;
            add(new JButton("Save"));
            
            gbc = new GridBagConstraints();
            gbc.gridy = 2;
            gbc.gridx = 1;
            gbc.weightx = 0.5;
            gbc.weighty = 0.5;
            gbc.fill = GridBagConstraints.NONE;
            add(new JButton("Delete"));
         }
      }
   }
}
