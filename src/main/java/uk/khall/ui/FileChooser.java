/*
* FleChooser displays a FileChooser.
*Copyright (C) 2005  Keith Hall
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.

* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
* */
package uk.khall.ui;

/*
 * FileChooser.java
 *
 * Created on 25 August 2002, 08:43
 */


import javax.swing.JDialog;
import javax.swing.JFileChooser;
import java.io.File;

/**
 * A JFileChooser implementation, which accepts a file filter and filter description and returns file path details of the selected file
 * @author keith hall
 */
public class FileChooser extends JDialog {
    private String fileName = "";
    private String filePath = "";
    private String [] filterName ;
    private String filterDesc = "";
    private String openType ="open";
    private String suggestedFileName;
    /** Creates new form FileChooser
     * @param opentype "open" or "save"
     * @param parent The parent frame
     * @param modal The type of frame
     * @param filtername The type of file to be filtered (e.g. jpg)
     * @param filterdesc A freeform description of the file filter
     */
    public FileChooser(java.awt.Frame parent, boolean modal, String opentype , String filtername, String filterdesc, String filepath) {
        super(parent, modal);
        String [] filters = new String[1];
        filters[0] = filtername;
        setFilter(filters, filterdesc);
        openType = opentype;
        initComponents();
        
        if (filepath !=null && !filepath.equals("")){
        	//System.out.println("trying to set filepath to "+ filepath);
        	File file = new File(filepath);
			jFileChooser.setSelectedFile(file);
        }
        
        if (openType.equals("open")){
            jFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        }else{
            jFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        }
    }
    public FileChooser(java.awt.Frame parent, boolean modal, String opentype , String filtername, String filterdesc, String filepath, String suggestedFileName) {
        super(parent, modal);
        String [] filters = new String[1];
        filters[0] = filtername;
        setFilter(filters, filterdesc);
        openType = opentype;
        this.suggestedFileName = suggestedFileName;
        initComponents();

        if (filepath !=null && !filepath.equals("")){
            //System.out.println("trying to set filepath to "+ filepath);
            File file = new File(filepath);
            jFileChooser.setSelectedFile(file);
        }

        if (openType.equals("open")){
            jFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        }else{
            jFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        }
    }
    /** Creates new form FileChooser
     * @param opentype "open" or "save"
     * @param parent The parent frame
     * @param modal The type of frame
     * @param filtername The types of file to be filtered (e.g. jpg)
     * @param filterdesc A freeform description of the file filter
     */    
    public FileChooser(java.awt.Frame parent, boolean modal, String opentype , String[] filtername, String filterdesc, String filepath) {
        super(parent, modal);
        setFilter(filtername, filterdesc);
        openType = opentype;
        initComponents();
		if (!filepath.equals("")){
			//System.out.println("trying to set filepath to "+ filepath);
			jFileChooser.setSelectedFile(new File(filepath));
		}
        if (openType.equals("open")){
            jFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        }else{
            jFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        }
    }
    /** Creates new form DemChooser
     * @param parent The parent frame
     * @param modal The type of frame
     */
    public FileChooser(java.awt.Frame parent, boolean modal, String filepath) {
        super(parent, modal);
        
        initComponentsWithoutFilter();
		if (!filepath.equals("")){
			//System.out.println("trying to set dir filepath to "+ filepath);
			jFileChooser.setSelectedFile(new File(filepath));
		}
		jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // jFileChooser.setDialogType(JFileChooser.DIRECTORIES_ONLY);
    }   
    private void initFilter(){
        // Note: source for ExampleFileFilter can be found in FileChooserDemo,
        // under the demo/jfc directory in the Java 2 SDK, Standard Edition.
        FileFilterImpl filter = new FileFilterImpl();
        
        for (int n =0 ; n < filterName.length; n++){
            //System.out.println("Filter name = " + filterName[n] + " filter desc = " + filterDesc);
            filter.addExtension(filterName[n]);
        }
        filter.setDescription(filterDesc);
        jFileChooser.setFileFilter(filter);
        
    }
    /** Sets the file type filter and description for searching for a file
     * @param filtername The type of file to be filtered (e.g. jpg)
     * @param filterdesc A freeform description of the file filter
     */
    public void setFilter(String filtername, String filterdesc){
        String [] filters = new String[1];
        filters[0] = filtername;
        filterName = filters;
        filterDesc = filterdesc;
    }
    /** Sets the file type filter and description for searching for a file
     * @param filtername The type of file to be filtered (e.g. jpg)
     * @param filterdesc A freeform description of the file filter
     */    
    public void setFilter(String [] filtername, String filterdesc){
        filterName = filtername;
        filterDesc = filterdesc;
    }
    /** returns the file name of the selected file
     * @return the file name
     */
    public String getFileName(){
        return fileName;
    }
    /** returns the full file path of the selected fle
     * @return the file path
     */
    public String getFilePath(){
        return filePath;
    }
    /**
     * 
     *
     */
    private void initChooser(){
        int returnVal = jFileChooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            //System.out.println("You chose to open this file: " +
            //jFileChooser.getSelectedFile().getName());
        }
    }
    /**
     * 
     *
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        jFileChooser = new JFileChooser();
        if (suggestedFileName != null)
            jFileChooser.setSelectedFile(new File(suggestedFileName));
        initFilter();
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        
        jFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFileChooserActionPerformed(evt);
            }
        });
        
        getContentPane().add(jFileChooser, java.awt.BorderLayout.NORTH);
        
        pack();
    }

    /**
     * 
     *
     */
    private void initComponentsWithoutFilter() {
        jFileChooser = new JFileChooser();
          addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        
        jFileChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jFileChooserActionPerformed(evt);
            }
        });
        
        getContentPane().add(jFileChooser, java.awt.BorderLayout.NORTH);
        
        pack();
    }
    /**
     * 
     * @param evt
     */
    private void jFileChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooserActionPerformed
        //System.out.println(evt.getActionCommand());
        if (evt.getActionCommand().equals("ApproveSelection")){
            fileName = jFileChooser.getSelectedFile().getName();
            filePath = jFileChooser.getSelectedFile().getPath();
            //System.out.println(fileName);
            //System.out.println(filePath);
            setVisible(false);
            dispose();
        }
        if (evt.getActionCommand().equals("CancelSelection")){
            setVisible(false);
            dispose();
        }
    }
    
    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        setVisible(false);
        dispose();
    }
    
    
    private JFileChooser jFileChooser;
    
    
}
