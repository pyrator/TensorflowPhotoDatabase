package uk.khall.ui.interact;

import uk.khall.sql.ClassTotals;
import uk.khall.sql.OpenImageJdbiUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class OpenImageSelectionUI extends JPanel {
    JList outputList;
    JList list;
    ListSelectionModel listSelectionModel;
    SortedListModel outputListSelectionModel;
    JButton jImageView;
    TreeMap<String, Integer> classTotals;
    ArrayList<String> files;
    String selectedClassName;
    public OpenImageSelectionUI() {
        super(new BorderLayout());
        ArrayList<ClassTotals> classTotalsList = OpenImageJdbiUtils.getClassTotals();
        classTotals = toTreeMap(classTotalsList);

        String[] listData = classTotals.keySet().toArray(new String[classTotals.size()]);

        String[] columnNames = {"Total"};
        list = new JList(listData);

        listSelectionModel = list.getSelectionModel();
        listSelectionModel.addListSelectionListener(
                new SharedListSelectionHandler());
        listSelectionModel.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        JScrollPane listPane = new JScrollPane(list);


        //Build output area.
        outputListSelectionModel = new SortedListModel();
        outputList = new JList(outputListSelectionModel);

        JScrollPane outputPane = new JScrollPane(outputList,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        //Do the layout.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        add(splitPane, BorderLayout.CENTER);

        JPanel topHalf = new JPanel();
        topHalf.setLayout(new BoxLayout(topHalf, BoxLayout.LINE_AXIS));
        JPanel listContainer = new JPanel(new GridLayout(1, 1));
        listContainer.setBorder(BorderFactory.createTitledBorder(
                "List"));
        listContainer.add(listPane);

        topHalf.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        topHalf.add(listContainer);

        topHalf.setMinimumSize(new Dimension(100, 50));
        topHalf.setPreferredSize(new Dimension(100, 110));
        splitPane.add(topHalf);

        JPanel bottomHalf = new JPanel(new BorderLayout());
        jImageView = new JButton();
        jImageView.setText("View Image");
        jImageView.setToolTipText("View the JPEG image");
        jImageView.setEnabled(true);
        jImageView.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                MouseButtonClicked(evt);
            }
        });
        bottomHalf.add(outputPane, BorderLayout.PAGE_START);
        bottomHalf.add(jImageView, BorderLayout.CENTER);

        bottomHalf.setPreferredSize(new Dimension(450, 170));
        splitPane.add(bottomHalf);
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("OpenImageSelectionUI");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        OpenImageSelectionUI demo = new OpenImageSelectionUI();
        demo.setOpaque(true);
        frame.setContentPane(demo);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private void MouseButtonClicked(java.awt.event.MouseEvent evt) {
        String evtName = evt.getComponent().getName();
        String file = (String)outputList.getSelectedValue();
        ViewOpenImageClass vfc = new ViewOpenImageClass();
        vfc.view(file, selectedClassName,1920, 1080, this);
    }
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    class SharedListSelectionHandler implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (!lsm.isSelectionEmpty()) {
                int minIndex = lsm.getMinSelectionIndex();
                if (lsm.isSelectedIndex(minIndex)) {
                    outputListSelectionModel.clear();
                    ArrayList<String> classes = new ArrayList<>(classTotals.keySet());
                    files = OpenImageJdbiUtils.getFilesContainingClass(classes.get(minIndex));
                    selectedClassName = classes.get(minIndex);
                    String[] imageList = files.toArray(new String[files.size()]);
                    outputListSelectionModel.addAll(imageList);
                }
            }
        }
    }

    static class SortedListModel extends AbstractListModel {
        SortedSet<Object> model;

        public SortedListModel() {
            model = new TreeSet<Object>();
        }

        public int getSize() {
            return model.size();
        }

        public Object getElementAt(int index) {
            return model.toArray()[index];
        }

        public void add(Object element) {
            if (model.add(element)) {
                fireContentsChanged(this, 0, getSize());
            }
        }

        public void addAll(Object elements[]) {
            Collection<Object> c = Arrays.asList(elements);
            model.addAll(c);
            fireContentsChanged(this, 0, getSize());
        }

        public void clear() {
            model.clear();
            fireContentsChanged(this, 0, getSize());
        }

        public boolean contains(Object element) {
            return model.contains(element);
        }

        public Object firstElement() {
            return model.first();
        }

        public Iterator iterator() {
            return model.iterator();
        }

        public Object lastElement() {
            return model.last();
        }

        public boolean removeElement(Object element) {
            boolean removed = model.remove(element);
            if (removed) {
                fireContentsChanged(this, 0, getSize());
            }
            return removed;
        }
    }
    private TreeMap<String, Integer> toTreeMap(ArrayList<ClassTotals> classTotalsList){
        TreeMap<String, Integer> classTotalMap = new TreeMap<String, Integer>();
        for (ClassTotals classTotal: classTotalsList){
            classTotalMap.put(classTotal.getClassName(),classTotal.getTotal());
        }
        return classTotalMap;
    }

}