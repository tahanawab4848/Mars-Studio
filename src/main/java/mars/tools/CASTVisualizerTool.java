package mars.tools;

import mars.ccompiler.builtin.CASTNode;
import mars.ccompiler.builtin.CLexer;
import mars.ccompiler.builtin.CParser;
import mars.ccompiler.builtin.CToken;
import mars.ccompiler.builtin.CTranspileException;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.MemoryAccessNotice;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;
import java.util.Observable;

/**
 * C AST Visualizer Tool for MARS.
 *
 * This tool allows users to view the Abstract Syntax Tree (AST) generated
 * by the built-in C transpiler.
 */
public class CASTVisualizerTool extends AbstractMarsToolAndApplication {

    private static final String NAME    = "C AST Visualizer";
    private static final String VERSION = "Version 1.0";
    private static final String HEADING = "Abstract Syntax Tree Visualizer";

    private JTree tree;
    private DefaultTreeModel treeModel;

    public CASTVisualizerTool(String title, String heading) {
        super(title, heading);
    }

    public CASTVisualizerTool() {
        super(NAME + ", " + VERSION, HEADING);
    }

    @Override
    public String getName() { return NAME; }

    @Override
    protected JComponent buildMainDisplayArea() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create the JTree
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("AST Root (Click Generate)");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        JScrollPane scrollPane = new JScrollPane(tree);
        root.add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton generateBtn = new JButton("Generate AST from Active Editor");
        generateBtn.addActionListener(e -> generateAST());
        controlPanel.add(generateBtn);

        root.add(controlPanel, BorderLayout.NORTH);

        return root;
    }

    private void generateAST() {
        String source = "";
        try {
            if (mars.Globals.getGui() == null || mars.Globals.getGui().getMainPane() == null) {
                JOptionPane.showMessageDialog(null, "No active editor found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            mars.venus.MainPane mainPane = (mars.venus.MainPane) mars.Globals.getGui().getMainPane();
            java.awt.Component selectedComponent = mainPane.getSelectedComponent();

            if (selectedComponent instanceof mars.visualization.VisualizationPanel) {
                mars.visualization.VisualizationPanel vizPanel = (mars.visualization.VisualizationPanel) selectedComponent;
                source = vizPanel.getCEditor().getCSourceCode();
            } else if (mainPane.getEditPane() != null) {
                source = mainPane.getEditPane().getSource();
            } else {
                JOptionPane.showMessageDialog(null, "No active editor found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (source == null || source.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "The editor is empty. Please enter some C code.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            CLexer lexer = new CLexer(source);
            List<CToken> tokens = lexer.tokenize();
            CParser parser = new CParser(tokens);
            CASTNode.Program prog = parser.parse();

            DefaultMutableTreeNode newRoot = buildTree(prog, "Program");
            treeModel.setRoot(newRoot);
            
            // Expand the first level
            for (int i = 0; i < tree.getRowCount(); i++) {
                tree.expandRow(i);
            }

        } catch (CTranspileException ex) {
            String preview = source.length() > 50 ? source.substring(0, 50).replace("\n", "\\n") + "..." : source.replace("\n", "\\n");
            JOptionPane.showMessageDialog(null, "Compilation Error:\n" + ex.getMessage() + "\n\nSource being parsed:\n" + preview + "\n\n(Make sure a C file is active in the MARS editor!)", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Unexpected Error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DefaultMutableTreeNode buildTree(Object nodeObj, String label) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(label);
        if (nodeObj == null) {
            treeNode.setUserObject("null");
            return treeNode;
        }

        if (nodeObj instanceof CASTNode.Program) {
            treeNode.setUserObject("Program");
            for (CASTNode decl : ((CASTNode.Program)nodeObj).decls) {
                treeNode.add(buildTree(decl, "Decl"));
            }
        } else if (nodeObj instanceof CASTNode.FuncDecl) {
            CASTNode.FuncDecl f = (CASTNode.FuncDecl) nodeObj;
            treeNode.setUserObject("FuncDecl: " + f.retType + " " + f.name);
            for (CASTNode.Param p : f.params) {
                treeNode.add(new DefaultMutableTreeNode("Param: " + p.type + " " + p.name));
            }
            treeNode.add(buildTree(f.body, "Body"));
        } else if (nodeObj instanceof CASTNode.VarDecl) {
            CASTNode.VarDecl v = (CASTNode.VarDecl) nodeObj;
            treeNode.setUserObject("VarDecl: " + v.type + " " + v.name);
            if (v.init != null) treeNode.add(buildTree(v.init, "Init"));
            if (v.arraySize != null) treeNode.add(buildTree(v.arraySize, "ArraySize"));
        } else if (nodeObj instanceof CASTNode.Block) {
            treeNode.setUserObject("Block");
            for (CASTNode.Stmt s : ((CASTNode.Block)nodeObj).stmts) {
                treeNode.add(buildTree(s, "Stmt"));
            }
        } else if (nodeObj instanceof CASTNode.IfStmt) {
            CASTNode.IfStmt i = (CASTNode.IfStmt) nodeObj;
            treeNode.setUserObject("IfStmt");
            treeNode.add(buildTree(i.cond, "Cond"));
            treeNode.add(buildTree(i.thenStmt, "Then"));
            if (i.elseStmt != null) treeNode.add(buildTree(i.elseStmt, "Else"));
        } else if (nodeObj instanceof CASTNode.WhileStmt) {
            CASTNode.WhileStmt w = (CASTNode.WhileStmt) nodeObj;
            treeNode.setUserObject("WhileStmt");
            treeNode.add(buildTree(w.cond, "Cond"));
            treeNode.add(buildTree(w.body, "Body"));
        } else if (nodeObj instanceof CASTNode.ForStmt) {
            CASTNode.ForStmt f = (CASTNode.ForStmt) nodeObj;
            treeNode.setUserObject("ForStmt");
            if (f.init != null) treeNode.add(buildTree(f.init, "Init"));
            if (f.cond != null) treeNode.add(buildTree(f.cond, "Cond"));
            if (f.step != null) treeNode.add(buildTree(f.step, "Step"));
            treeNode.add(buildTree(f.body, "Body"));
        } else if (nodeObj instanceof CASTNode.ExprStmt) {
            treeNode.setUserObject("ExprStmt");
            treeNode.add(buildTree(((CASTNode.ExprStmt)nodeObj).expr, "Expr"));
        } else if (nodeObj instanceof CASTNode.ReturnStmt) {
            treeNode.setUserObject("ReturnStmt");
            CASTNode.ReturnStmt r = (CASTNode.ReturnStmt) nodeObj;
            if (r.expr != null) treeNode.add(buildTree(r.expr, "Val"));
        } else if (nodeObj instanceof CASTNode.BreakStmt) {
            treeNode.setUserObject("BreakStmt");
        } else if (nodeObj instanceof CASTNode.ContinueStmt) {
            treeNode.setUserObject("ContinueStmt");
        } else if (nodeObj instanceof CASTNode.IntLit) {
            treeNode.setUserObject("IntLit: " + ((CASTNode.IntLit)nodeObj).value);
        } else if (nodeObj instanceof CASTNode.Ident) {
            treeNode.setUserObject("Ident: " + ((CASTNode.Ident)nodeObj).name);
        } else if (nodeObj instanceof CASTNode.BinaryOp) {
            CASTNode.BinaryOp b = (CASTNode.BinaryOp) nodeObj;
            treeNode.setUserObject("BinaryOp: " + b.op);
            treeNode.add(buildTree(b.left, "Left"));
            treeNode.add(buildTree(b.right, "Right"));
        } else if (nodeObj instanceof CASTNode.Assign) {
            CASTNode.Assign a = (CASTNode.Assign) nodeObj;
            treeNode.setUserObject("Assign");
            treeNode.add(buildTree(a.target, "Target"));
            treeNode.add(buildTree(a.value, "Value"));
        } else if (nodeObj instanceof CASTNode.Call) {
            CASTNode.Call c = (CASTNode.Call) nodeObj;
            treeNode.setUserObject("Call: " + c.funcName);
            for (CASTNode.Expr arg : c.args) {
                treeNode.add(buildTree(arg, "Arg"));
            }
        } else if (nodeObj instanceof CASTNode.ArrayAccess) {
            CASTNode.ArrayAccess a = (CASTNode.ArrayAccess) nodeObj;
            treeNode.setUserObject("ArrayAccess: " + a.name);
            treeNode.add(buildTree(a.index, "Index"));
        } else if (nodeObj instanceof CASTNode.AddressOf) {
            CASTNode.AddressOf a = (CASTNode.AddressOf) nodeObj;
            treeNode.setUserObject("AddressOf");
            treeNode.add(buildTree(a.expr, "Expr"));
        } else if (nodeObj instanceof CASTNode.Dereference) {
            CASTNode.Dereference d = (CASTNode.Dereference) nodeObj;
            treeNode.setUserObject("Dereference");
            treeNode.add(buildTree(d.expr, "Expr"));
        } else {
            treeNode.setUserObject(label + " (Unknown type: " + nodeObj.getClass().getSimpleName() + ")");
        }
        return treeNode;
    }

    @Override
    protected void addAsObserver() {
        // Not used for visualization
    }

    @Override
    protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
        // Not used
    }
}
