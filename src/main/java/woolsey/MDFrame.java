package woolsey;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.markdown4j.*;

/**
 * Created by admin on 2016/12/10.
 */

class MDFrame extends JFrame {
    private Markdown4jProcessor markdown4jProcessor = new Markdown4jProcessor();
    private JFrame frame;
    private JTextArea editor;
    private JScrollPane editorPane;
    private JEditorPane previewer;
    private JScrollPane previewerPane;
    private DefaultTreeModel treeData;
    private JTree MDTree;
    private DefaultMutableTreeNode root;
    private final String rootString = "Article";
    private final String frameTitle = "MarkDown by Woolsey 1.0";
    private final int filePathLimit = 30;


    MDFrame(){
        //UI print
        super();
        frame = this;
        this.setSize(1000,500);
        this.setLayout(new BorderLayout());
        this.setTitle(frameTitle);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (Throwable e) {
            e.printStackTrace();

        }

        //MarkDown Trees in the left to jump to title
        JTree MDTree = createMDTree();
        MDTree.setPreferredSize(new Dimension(200, 500));
        MDTree.setMinimumSize(new Dimension(100, 200));
        this.add(MDTree, BorderLayout.WEST);

        //MarkDown edit area
        editorPane = createEditor();
        editorPane.setPreferredSize(new Dimension(380, 500));
        editorPane.setMinimumSize(new Dimension(200,200));
        //this.add(editorPane, BorderLayout.CENTER);

        //MarkDown Preview
        previewerPane = createPreviewer();
        previewerPane.setPreferredSize(new Dimension(380, 500));
        previewerPane.setMinimumSize(new Dimension(200,200));
        //this.add(previewerPane, BorderLayout.EAST);

        //splitPane of editor and previewer
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                editorPane,
                previewerPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(new EmptyBorder(0,0,0,0));
        //splitPane.setBackground(new Color(215, 215, 215));
        BasicSplitPaneDivider divider = ( (BasicSplitPaneUI)splitPane.getUI() ).getDivider();
        divider.setBorder(BorderFactory.createLineBorder(new Color(215, 215, 215), 3));
        this.add(splitPane, BorderLayout.CENTER);

        //tool area
        JPanel toolArea = createToolArea();
        toolArea.setPreferredSize(new Dimension(1000, 40));
        toolArea.setMinimumSize(new Dimension(500, 30));
        this.add(toolArea, BorderLayout.NORTH);
    }

    private JTree createMDTree(){
        root = new DefaultMutableTreeNode(rootString);
        treeData = new DefaultTreeModel(root);
        MDTree = new JTree(treeData);

        //style
        MDTree.setBackground(new Color(245, 245, 245));
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer)(MDTree.getCellRenderer());
        renderer.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        renderer.setBackgroundNonSelectionColor(null);
        renderer.setTextNonSelectionColor(new Color(40, 40, 40));
        renderer.setBackgroundSelectionColor(new Color(128, 128, 128));
        renderer.setTextSelectionColor(new Color(255, 255, 255));
        renderer.setBorderSelectionColor(null);
        MDTree.addTreeSelectionListener(new TreeNodeSelect());

        return MDTree;
    }

    private JScrollPane createEditor(){
        //initialize and describe
        editor = new JTextArea();
        editor.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        editor.setForeground(new Color(40, 40, 40));
        editor.setEditable(true);
        editor.setMargin(new Insets(10, 5, 5, 5));

        //connect to document listener
        editor.getDocument().addDocumentListener(new dataChanged());

        //add into scrollPane and return
        JScrollPane editorScrollPane = new JScrollPane(editor);
        editorScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        editorScrollPane.setBorder(null);

        return editorScrollPane;
    }

    private JScrollPane createPreviewer(){
        //initialize and describe
        previewer = new JEditorPane();
        previewer.setContentType("text/html");
        previewer.setBackground(new Color(245, 245, 245));
        previewer.setEditable(false);

        //add into scrollPane and return
        JScrollPane previewerScrollPane = new JScrollPane(previewer);
        previewerScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        previewerScrollPane.setBorder(null);

        return previewerScrollPane;
    }

    private JPanel createToolArea(){
        JPanel toolArea = new JPanel();
        //style
        FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
        layout.setHgap(20);
        layout.setVgap(0);
        toolArea.setLayout(layout);
        toolArea.setBackground(new Color(215, 215, 215));

        //open button
        JButton openButton = new JButton();
        buttonCommonStyle(openButton, "./src/main/resources/open.png", "./src/main/resources/open_enter.png", "open file");
        openButton.addActionListener(new openButtonListener());
        toolArea.add(openButton);

        //save button
        JButton saveButton = new JButton();
        buttonCommonStyle(saveButton, "./src/main/resources/save.png", "./src/main/resources/save_enter.png", "save");
        saveButton.addActionListener(new SaveButtonListener());
        toolArea.add(saveButton);

        //html button
        JButton htmlButton = new JButton();
        buttonCommonStyle(htmlButton, "./src/main/resources/html.png", "./src/main/resources/html_enter.png", "export to html");
        htmlButton.addActionListener(new HtmlButtonListener());
        toolArea.add(htmlButton);

        //doc button
        JButton docButton = new JButton();
        buttonCommonStyle(docButton, "./src/main/resources/doc.png", "./src/main/resources/doc_enter.png", "export to doc");
        docButton.addActionListener(new DocButtonListener());
        toolArea.add(docButton);

        return toolArea;
    }

    private class dataChanged implements DocumentListener{
        public void changedUpdate(DocumentEvent e) {
            //no thing
        }
        public void insertUpdate(DocumentEvent e) {
            try {
                previewer.setText(markdown4jProcessor.process(editor.getText()));
                TreeUpdate(editor.getText());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        public void removeUpdate(DocumentEvent e){
            try {
                previewer.setText(markdown4jProcessor.process(editor.getText()));
                TreeUpdate(editor.getText());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private class DocumentTitle {
        private String content;
        private int degree;
        private String plainString;

        DocumentTitle(){
            this.content = null;
            this.degree = 0;
            this.plainString = null;

        }

        DocumentTitle(String content){
            this.content = content;
            this.degree = 0;
            this.plainString = null;

        }

        DocumentTitle(String content, int degree, String plainString){
            this.content = content;
            this.degree = degree;
            this.plainString = plainString;
        }

        public String toString(){
            return content;
        }

        String getContent(){
            return content;
        }
        int getDegree(){
            return degree;
        }

        String getPlainString(){
            return plainString;
        }
    }

    private void TreeUpdate(String article){
        ArrayList<DocumentTitle> titleArrayList = new ArrayList<DocumentTitle>();
        DefaultMutableTreeNode pointer;

        //split article into [<h?>..</h?>]
        String regex = "<h[1-9].*(</h[1-9]>|\n)";
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(article);
        ArrayList<String> plainTitleArray = new ArrayList<String>();
        while(m.find()){
            plainTitleArray.add(m.group());
        }

        //delete the old tree
        root.removeAllChildren();

        //generate DocumentTitle Array
        for (String plainTitle:plainTitleArray) {
            titleArrayList.add(new DocumentTitle(
                    plainTitle.replaceAll("<h[1-9]>|</h[1-9]>|\n", ""),
                    plainTitle.charAt(2) - '0',
                    plainTitle
            ));
        }

        //generate tree
        pointer = root;

        for (DocumentTitle currentTitle :titleArrayList ) {
            while( !pointer.toString().equals(rootString)
                    && ((DocumentTitle)pointer.getUserObject()).getDegree() >= currentTitle.getDegree() ){
                pointer = (DefaultMutableTreeNode) pointer.getParent();
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(currentTitle);
            pointer.add(node);
            pointer = node;
        }

        treeData.reload();
    }

    private class TreeNodeSelect implements TreeSelectionListener{

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if(MDTree.getSelectionPath()==null || MDTree.getSelectionPath().getPath().length==1){
                return;
            }

            Object[] nodeArray = MDTree.getSelectionPath().getPath();
            ArrayList<DocumentTitle> titles = new ArrayList<DocumentTitle>();
            String[] editorContent = editor.getText().split("\n");
            String[] previewerContent = editor.getText().split("\n+|<br>|<br/>");
            int index = 0;

            //initial titles
            for(int i=1; i<nodeArray.length; i++){
                DefaultMutableTreeNode node  = (DefaultMutableTreeNode)nodeArray[i];
                titles.add((DocumentTitle) node.getUserObject());
            }

            //find the column of the element in editor raw by raw
            for (int raw=0; raw<editorContent.length; raw++){
                //check
                if((editorContent[raw]+"\n").contains(titles.get(index).getPlainString())){
                    if(index == titles.size()-1){//leaf
                        raw = (raw>0)?raw-1:raw;//prevent the raw show half
                        setVerticalScrollBarPos(editorPane, 1.0*raw/editorContent.length);
                        break;
                    }
                    else
                    {
                        index++;
                    }
                }
            }

            //find the column of the element in previewer raw by raw
            for (int raw=0; raw<previewerContent.length; raw++){
                //check
                if((previewerContent[raw]+"\n").contains(titles.get(index).getPlainString())){
                    if(index == titles.size()-1){//leaf
                        raw = (raw>0)?raw-1:raw;//prevent the raw show half
                        setVerticalScrollBarPos(previewerPane, 1.0*raw/editorContent.length);
                        break;
                    }
                    else
                    {
                        index++;
                    }
                }
            }
        }
    }

    private void setVerticalScrollBarPos(JScrollPane target, double percentage){
        JScrollBar bar = target.getVerticalScrollBar();
        int max = bar.getMaximum();
        int min = bar.getMinimum();
        int value = min + (int) (percentage*(max-min));
        bar.setValue(value);
    }

    private void buttonCommonStyle(final JButton btn, String iconPath, String iconPath_enter, String tip){
        Image iconImg = new ImageIcon(iconPath).getImage().getScaledInstance(20, 20, Image.SCALE_FAST);
        Image iconImg_enter = new ImageIcon(iconPath_enter).getImage().getScaledInstance(20, 20, Image.SCALE_FAST);
        final ImageIcon icon = new ImageIcon(iconImg);
        final ImageIcon icon_enter = new ImageIcon(iconImg_enter);

        btn.setOpaque(false);
        btn.setBorder(BorderFactory.createEmptyBorder(0,0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setRolloverEnabled(true);
        btn.setBackground(null);
        btn.setPreferredSize(new Dimension(30, 40));
        btn.setIcon(icon);
        btn.setToolTipText(tip);
        //btn.updateUI();

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                JButton src = (JButton)e.getSource();
                if (src.isRolloverEnabled()){
                    src.setBackground(new Color(117, 117, 117));
                    src.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    src.setIcon(icon_enter);
                    //btn.updateUI();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JButton src = (JButton)e.getSource();
                if (src.isRolloverEnabled()){
                    src.setBackground(null);
                    src.setIcon(icon);
                    //btn.updateUI();
                }
            }
        });
    }

    private class openButtonListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            String filePath;
            //open file
            FileDialog fileDialog = new FileDialog(frame, "Open MarkDown File", FileDialog.LOAD);
            fileDialog.setFile("*.md");
            fileDialog.setVisible(true);
            if (fileDialog.getFile() != null){
                if(fileDialog.getFile().matches(".*\\.md"))// if md type
                {
                    filePath = fileDialog.getDirectory() + fileDialog.getFile() + ( ( fileDialog.getFile().contains(".md") )?"":".md" );
                    try {
                        FileInputStream in = new FileInputStream(filePath);
                        int size = in.available();
                        byte[] buffer = new byte[size];
                        in.read(buffer);
                        in.close();
                        editor.setText(new String(buffer, "UTF8"));
                        frame.setTitle(fileDialog.getFile() + " - [" + stringCut(filePath, filePathLimit) + "]" + " - " + frameTitle);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                else{
                    JOptionPane.showMessageDialog(null, "请打开MarkDown格式文件", "文件格式错误", JOptionPane.PLAIN_MESSAGE);
                }
            }

        }
    }

    private String stringCut(String str, int limit) {
        if(str.length() <= limit){
            return  str;
        }
        else{
            return "..." + str.substring(str.length()-limit-1, str.length()-1);
        }
    }

    private class SaveButtonListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            String filePath;
            //save file
            FileDialog fileDialog = new FileDialog(frame, "Save File", FileDialog.SAVE);
            fileDialog.setFile("*.md");
            fileDialog.setVisible(true);
            if (fileDialog.getFile() != null){
                filePath = fileDialog.getDirectory() + fileDialog.getFile() + ( ( fileDialog.getFile().contains(".md") )?"":".md" );
                try {
                    FileOutputStream out = new FileOutputStream(filePath);
                    String content = editor.getText();
                    out.write(content.getBytes());
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
    }

    private class HtmlButtonListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            String filePath;
            //save file as HTML
            FileDialog fileDialog = new FileDialog(frame, "Save as Html", FileDialog.SAVE);
            fileDialog.setFile("*.html");
            fileDialog.setVisible(true);
            if (fileDialog.getFile() != null){
                filePath = fileDialog.getDirectory() + fileDialog.getFile() + ( ( fileDialog.getFile().contains(".html") )?"":".html" );
                try {
                    FileOutputStream out = new FileOutputStream(filePath);
                    String content = previewer.getText();
                    out.write(content.getBytes());
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private class DocButtonListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            String filePath;
            //save file as Doc
            FileDialog fileDialog = new FileDialog(frame, "Save as Doc", FileDialog.SAVE);
            fileDialog.setFile("*.doc");
            fileDialog.setVisible(true);
            if (fileDialog.getFile() != null){
                filePath = fileDialog.getDirectory() + fileDialog.getFile() + ( ( fileDialog.getFile().contains(".doc") )?"":".doc" );
                try {
                    byte contentByte[] = previewer.getText().getBytes();
                    ByteArrayInputStream bais = new ByteArrayInputStream(contentByte);
                    POIFSFileSystem poifs = new POIFSFileSystem();
                    DirectoryEntry directory = poifs.getRoot();
                    DocumentEntry documentEntry = directory.createDocument("WordDocument", bais);

                    FileOutputStream out = new FileOutputStream(filePath);
                    poifs.writeFilesystem(out);
                    bais.close();
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

}
