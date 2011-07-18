package edu.brown.cs.bubbles.bedu.chat;

import java.awt.BorderLayout;
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

import edu.brown.cs.bubbles.bass.BassFactory;
import edu.brown.cs.bubbles.bass.BassName;
import edu.brown.cs.bubbles.bass.BassConstants.BassRepository;
import edu.brown.cs.bubbles.buda.BudaBubble;
import edu.brown.cs.bubbles.buda.BudaConstants;

class BeduManageCoursesBubble extends BudaBubble
{
	private JComboBox combo;
	
   private static final long serialVersionUID = 1L;
   private static final Dimension DEFAULT_DIMENSION = new Dimension(400, 200);
   private static final String ADD_STUDENT_STR = "Add course as student";
   private static final String ADD_TA_STR = "Add course as TA";
   BeduManageCoursesBubble()
   {
     setContentPane(new ContentPane()); 
   }


   private class ContentPane extends JPanel implements ItemListener
   {
		private static final long serialVersionUID = 1L;
		private ConfigPane cur_config_pane;
      
      private ContentPane()
      {
         setPreferredSize(DEFAULT_DIMENSION);
         BassRepository course_repo = BassFactory.getRepository(BudaConstants.SearchType.SEARCH_COURSES);
         setLayout(new BorderLayout());
         combo = new JComboBox();
         combo.addItemListener(this);
         combo.addItem(ADD_STUDENT_STR);
         combo.addItem(ADD_TA_STR);
         
         add(combo, BorderLayout.PAGE_START); 
         
         for(BassName n : course_repo.getAllNames())
         {
            if(n.toString().length() >= 0 && n.toString().charAt(0) != '@')
               combo.addItem(n);
         }
         
      }
      
      @Override
      public void itemStateChanged(ItemEvent e)
      {
         if(cur_config_pane != null)
            remove(cur_config_pane);
         if(e.getItem() instanceof String)
         {
         	if(((String)e.getItem()).equals(ADD_STUDENT_STR))
         	{
         		cur_config_pane = new ConfigPane(new BeduCourse.StudentCourse("", ""));
         	}
         	else if(((String)e.getItem()).equals(ADD_TA_STR))
         	{
         		cur_config_pane = new ConfigPane(new BeduCourse.TACourse("", "", "", ""));
         	}
         }
         else
         	cur_config_pane = new ConfigPane((BeduCourse)e.getItem());
         add(cur_config_pane, BorderLayout.CENTER);
      }
   }

   private class ConfigPane extends JPanel implements ActionListener
   {
		private static final long serialVersionUID = 1L;
		private final String delete_action = "delete";
   	private final String save_action = "save";
   	
      private JTextField name_field;
      private JTextField jid_field;
      private JTextField password_field;
      private JTextField server_field;
      private BeduCourse course;
      
      private ConfigPane(BeduCourse c)
      {
      	course = c;
      	JButton deleteButton = new JButton("Delete");
      	deleteButton.setActionCommand(delete_action);
      	deleteButton.addActionListener(this);
      	
      	JButton saveButton = new JButton("Save");
      	saveButton.setActionCommand(save_action);
      	saveButton.addActionListener(this);
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
         name_field.setText(c.getCourseName());
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
            add(saveButton, gbc);
            
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.weighty = 0.5;
            gbc.gridy = 4;
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.EAST;
            
            add(deleteButton, gbc);
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
            add(saveButton, gbc);
                       
            gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.9;
            gbc.weighty = 0.5;
            gbc.gridy = 4;
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.EAST;
            add(deleteButton, gbc);
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
				r.addCourse(new_course);
				BeduManageCoursesBubble.this.combo.addItem(new_course);
				BeduManageCoursesBubble.this.combo.setSelectedItem(new_course);
			}
			else if(e.getActionCommand().equals(delete_action)){
				try {
					r.removeCourse(course);
					BeduManageCoursesBubble.this.combo.removeItem(course);
					BeduManageCoursesBubble.this.combo.setSelectedIndex(0);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
   }
}
