package edu.brown.cs.bubbles.bedu.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

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
   private static final Dimension DEFAULT_DIMENSION = new Dimension(200, 300);
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

   private class ConfigPane extends JPanel implements ActionListener
   {
   	private final String delete_action = "delete";
   	private final String save_action = "save";
   	
      private JTextField name_field;
      private JTextField jid_field;
      private JTextField password_field;
      private JTextField server_field;
      private BeduCourse course;
      
      public ConfigPane(BeduCourse c)
      {
      	course = c;
      	JButton deleteButton = new JButton("Delete");
      	deleteButton.setActionCommand(delete_action);

      	JButton saveButton = new JButton("Save");
      	saveButton.setActionCommand(save_action);
      	
         setLayout(new GridBagLayout());
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridx = 0;
         gbc.gridy = 0;
         gbc.weightx = 0.1;
         gbc.weighty = 0.5;
         gbc.anchor = GridBagConstraints.WEST;
         add(new JLabel("Name: "), gbc);
         
         gbc = new GridBagConstraints();
         name_field = new JTextField();
         gbc = new GridBagConstraints();
         gbc.fill = GridBagConstraints.HORIZONTAL;
         gbc.gridx = 1;
         gbc.weightx = .9;
         gbc.weighty = 0.5;
         gbc.gridy = 0;
         name_field.setText(c.getTAJID());
         add(name_field, gbc);
         
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
         jid_field = new JTextField();
         jid_field.setText(c.getTAJID());
         add(jid_field, gbc);
         
         if(c instanceof BeduCourse.TACourse)
         {
         	BeduCourse.TACourse tc = (BeduCourse.TACourse)c;
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.1;
            gbc.weighty = 0.5;
            gbc.gridy = 2;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            add(new JLabel("TA Password: "), gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.weighty = 0.5;
            gbc.gridy = 2;
            gbc.gridx = 1;
            password_field = new JTextField();
            password_field.setText(tc.getXMPPPassword());
            add(password_field, gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.1;
            gbc.weighty = 0.5;
            gbc.gridy = 3;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            add(new JLabel("XMPP Server: "), gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.weighty = 0.5;
            gbc.gridy = 3;
            gbc.gridx = 1;
            server_field = new JTextField();
            server_field.setText(tc.getXMPPServer());
            add(server_field, gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.weighty = 0.5;
            gbc.gridy = 4;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            add(new JButton("Save"), gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.weighty = 0.5;
            gbc.gridy = 4;
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.EAST;
            
            add(new JButton("Delete"), gbc);
         }
         else 
         {
         	gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.weighty = 0.5;
            gbc.gridy = 4;
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.WEST;
            add(new JButton("Save"), gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.weighty = 0.5;
            gbc.gridy = 4;
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.EAST;
            add(new JButton("Delete"), gbc);
         }
      }

		public void actionPerformed(ActionEvent e) {
			BeduCourseRepository r = (BeduCourseRepository)(BassFactory.getRepository(BudaConstants.SearchType.SEARCH_COURSES));
			if(e.getActionCommand().equals(save_action)){
				BeduCourse new_course = null;
				if(course instanceof BeduCourse.StudentCourse)
				{
					new_course = new BeduCourse.StudentCourse(name_field.getText(), jid_field.getText());
				}
				else if(course instanceof BeduCourse.TACourse)
				{
					new_course = new BeduCourse.TACourse(name_field.getText(), jid_field.getText(), password_field.getText(), server_field.getText());
				}
				
				try {
					r.removeCourse(course);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				r.addCourse(course);
			}
			else if(e.getActionCommand().equals(delete_action)){
				try {
					r.removeCourse(course);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
   }
}
