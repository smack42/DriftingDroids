/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011-2014 Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package driftingdroids.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CancellationException;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ToolTipUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import net.java.dev.designgridlayout.DesignGridLayout;
import driftingdroids.model.Board;
import driftingdroids.model.Move;
import driftingdroids.model.Solution;
import driftingdroids.model.Solver;



public class SwingGUI implements ActionListener {
    private static final Color COL_BACKGROUND = new Color(190, 190, 190);
    private static final Color COL_CELL1 = new Color(210, 210, 210);
    private static final Color COL_CELL2 = new Color(245, 245, 245);
    private static final Color COL_WALL = new Color(85, 85, 85);
    private static final Color[] COL_ROBOT = {
        new Color(245, 99, 99), //Color.RED,
        new Color(99, 245, 99), //Color.GREEN,
        new Color(99, 99, 245), //Color.BLUE,
        new Color(245, 245, 99),//Color.YELLOW,
        new Color(245, 245, 245) //light grey
    };

    private static final int ICON_SIZE = 29;

    private static final String AC_BOARD_ROBOTS   = "robots";
    private static final String AC_PLACE_ROBOT    = "placerobot";
    private static final String AC_GAME_ID        = "gameid";
    private static final String AC_SELECT_SOLUTION= "selectsolution";
    private static final String AC_SHOW_COLOR_NAMES = "showcolornames";
    private static final String AC_SHOW_ACTIVE_GOAL = "showactivegoal";
    
    private static final ResourceBundle L10N = ResourceBundle.getBundle("driftingdroids-localization-ui");  //L10N = Localization
    
    private volatile Board board = null;
    private BoardCell[] boardCells = new BoardCell[0]; // init placeholder
    private int boardCellsWidth = 0, boardCellsHeight = 0; // init placeholder
    private int[] currentPosition;
    
    private volatile SolverTask solverTask = null;                  //only set while SolverTask is working
    private volatile List<Solution> computedSolutionList = null;    //result of SolverTask -> solver.execute()
    private int computedSolutionIndex = 0;
    private final List<Move> moves;
    private int hintCounter = 0;
    private int placeRobot = -1;    //default: false
    private boolean selectGoal = false;
    private boolean doRefreshJlistQuadrants = true;
    
    private final JFrame frame = new JFrame();
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final JTabbedPane jtabEditBoard = new JTabbedPane();
    private final JList[] jlistQuadrants = new JList[4];
    private final JComboBox jcomboRobots = new JComboBox();
    private final JSpinner jspinWidth = new JSpinner();
    private final JSpinner jspinHeight = new JSpinner();

    private class JListGoalToolTip extends JList {
        private static final long serialVersionUID = 3257436257447585359L;
        @Override
        public JToolTip createToolTip() {
            final String[] values = this.getToolTipText().split(";");
            return new GoalToolTip(this, Integer.parseInt(values[0]), Integer.parseInt(values[1]));
        };
    }
    private final JList jlistGoalRobots = new JListGoalToolTip();
    private final JList jlistGoalShapes = new JListGoalToolTip();

    private final JComboBox jcomboOptSolutionMode = new JComboBox();
    private final JCheckBox jcheckOptAllowRebounds = new JCheckBox();
    private final JCheckBox jcheckOptShowColorNames = new JCheckBox();
    private final JCheckBox jcheckOptShowOnlyActiveGoal = new JCheckBox();
    private final JCheckBox jcheckOptShowSolutions = new JCheckBox();
    private final JTabbedPane jtabPreparePlay = new JTabbedPane();
    private final JComboBox jcomboPlaceRobot = new JComboBox();
    private final JComboBox jcomboGameIDs = new JComboBox();
    private final JComboBox jcomboSelectSolution = new JComboBox();
    private final JButton jbutSolutionHint = new JButton();
    private final JButton jbutNextMove = new JButton();
    private final JButton jbutAllMoves = new JButton();
    private final JButton jbutPrevMove = new JButton();
    private final JButton jbutNoMoves = new JButton();
    private final JTextPane jtextSolution = new JTextPane();
    
    public SwingGUI(final String windowTitle) throws InterruptedException, InvocationTargetException {
        this.board = Board.createBoardRandom(5);
        this.board.setRobotsRandom();
        this.board.setGoalRandom();
        //this.board = Board.createBoardGameID("0765+41+2E21BD0F+1C");   //1A 4B 3B 2B
        this.moves = new ArrayList<Move>();
        
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createAndShowGUI(windowTitle);
            }
        });
    }
    
    private void makeBoardQuadrants() {
        this.board = Board.createBoardQuadrants(
                this.jlistQuadrants[0].getSelectedIndex(),
                this.jlistQuadrants[1].getSelectedIndex(),
                this.jlistQuadrants[2].getSelectedIndex(),
                this.jlistQuadrants[3].getSelectedIndex(),
                this.jcomboRobots.getSelectedIndex() + 1 );
        this.refreshBoard();
    }
    
    private void makeRandomBoardQuadrants() {
        this.board = Board.createBoardRandom(this.jcomboRobots.getSelectedIndex() + 1);
        this.refreshBoard();
    }
    
    private void updateBoardRandomRobots() {
        this.board.setRobotsRandom();
        this.updateBoardGetRobots();
    }
    
    private void updateBoardRandomGoal() {
        this.board.setRobots(this.currentPosition);
        this.board.setGoalRandom();
        this.updateBoardGetRobots();
    }
    
    private void updateBoardGetRobots() {
        this.currentPosition = this.board.getRobotPositions().clone();
        this.runSolverTask();
    }
    
    private void removeSolution() {
        final SolverTask st = this.solverTask;
        if (null != st) {
            this.solverTask = null;
            st.cancel(true);
        }
        this.computedSolutionList = null;
        this.computedSolutionIndex = 0;
        this.moves.clear();
        this.jcomboSelectSolution.setSelectedIndex(0);
        while (this.jcomboSelectSolution.getItemCount() > 1) {
            this.jcomboSelectSolution.removeItemAt(1);
        }
        this.refreshButtons();
    }
    
    private void appendSolutionText(final String str, final Color bgCol) {
        final StyledDocument doc = this.jtextSolution.getStyledDocument();
        Style style = null;
        if (null != bgCol) {
            style = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
            StyleConstants.setForeground(style, Color.BLACK);
            StyleConstants.setBackground(style, bgCol);
        }
        try {
            doc.insertString(doc.getLength(), str, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendSolutionTextCurrentGoal() {
        final Board.Goal goal = this.board.getGoal();
        final Icon goalIcon = new GoalIcon(goal, this.jcheckOptShowColorNames.isSelected());
        final String goalStr = "  " + L10N.getString("txt.Goal.text")
                + " - " + Board.getColorLongL10N(goal.robotNumber)
                + " - " + Board.getGoalShapeL10N(goal.shape) + "\n\n";
        this.jtextSolution.setCaretPosition(this.jtextSolution.getStyledDocument().getLength());
        this.jtextSolution.insertIcon(goalIcon);
        this.appendSolutionText(goalStr, null);
    }

    private String getSolverOptionsString(final Solver solver) {
        StringBuilder sb = new StringBuilder();
        sb.append(L10N.getString("txt.Options.text")).append('\n')
          .append("- ")
          .append(L10N.getString("lbl.PreferSolutionWith.text"))
          .append(" [").append(solver.getOptionSolutionMode().toString()).append("] ")
          .append("\n   ").append(L10N.getString("lbl.NumberOfRobotsMoved.text"))
          .append("\n- [")
          .append(L10N.getString(solver.getOptionAllowRebounds() ? "txt.Yes.text" : "txt.No.text"))
          .append("] ").append(L10N.getString("chk.AllowReboundMoves.text"))
          .append("\n\n");
        return sb.toString();
    }
    
    private void setSolution(final Solver solver) {
        this.computedSolutionList = solver.get();
        this.computedSolutionIndex = 0;
        for (int i = 0;  i < this.computedSolutionList.size();  ++i) {
            this.jcomboSelectSolution.addItem((i+1) + ")  " + this.computedSolutionList.get(i).toString());
        }
        this.hintCounter = 0;
        this.jtextSolution.setText(null);
        this.appendSolutionText(this.getSolverOptionsString(solver), null);
        this.appendSolutionTextCurrentGoal();
        if (this.computedSolutionList.get(this.computedSolutionIndex).size() > 0) {
            final int seconds = (int)((solver.getSolutionMilliSeconds() + 999) / 1000);
            final int solutions = this.computedSolutionList.size();
            // message: found 1 solution(s) in 3 second(s).
            this.appendSolutionText(
                    MessageFormat.format(L10N.getString("msg.FoundSolutions.pattern"), Integer.valueOf(solutions), Integer.valueOf(seconds))
                    + "\n\n", null);
        } else {
            this.appendSolutionText(L10N.getString("txt.NoSolutionFound.text") + "\n", null);
        }
        System.out.println(this.computedSolutionList.get(this.computedSolutionIndex).toMovelistString() + "  (" + solver.toString() + ")");
        this.refreshButtons();
        if (this.jcheckOptShowSolutions.isSelected()) {
            while (this.jbutNextMove.isEnabled()) {
                this.showNextMove(true);
            }
        }
    }
    
    private void selectSolution(final int solutionIndex, final String solutionString) {
        if ( (null != this.computedSolutionList) &&
                (solutionIndex -1 != this.computedSolutionIndex) &&
                (solutionIndex > 0) &&
                (solutionIndex <= this.computedSolutionList.size()) ) {
            //reset moves
            final int oldMovesSize = this.moves.size();
            while (this.jbutPrevMove.isEnabled()) {
                this.showPrevMove(false);
            }
            //set new solution
            this.computedSolutionIndex = solutionIndex - 1;
            this.appendSolutionText("\n" + L10N.getString("txt.SelectSolution.text") + " " + solutionString + "\n", null);
            this.computedSolutionList.get(this.computedSolutionIndex).resetMoves();
            //show moves
            for (int i = 0;  i < oldMovesSize;  ++i) {
                this.showNextMove(true);
            }
        }
    }
    
    private void showHint() {
        final Integer numMoves = Integer.valueOf(this.computedSolutionList.get(this.computedSolutionIndex).size());
        final Set<Integer> robotsMoved = this.computedSolutionList.get(this.computedSolutionIndex).getRobotsMoved();
        final Integer numRobots = Integer.valueOf(robotsMoved.size());
        if (0 == this.hintCounter) {
            //first hint: number of moves
            this.appendSolutionText(MessageFormat.format(L10N.getString("msg.Hint.1.pattern"), numMoves) + "\n", null);
        } else if (1 == this.hintCounter) {
            //second hint: number of moves + number of robots moved
            this.appendSolutionText(MessageFormat.format(L10N.getString("msg.Hint.2.pattern"), numMoves, numRobots) + "\n", null);
        } else if (2 == this.hintCounter) {
            //third hint: number of moves + number of robots moved + which robots moved
            this.appendSolutionText(MessageFormat.format(L10N.getString("msg.Hint.3.pattern"), numMoves, numRobots) + " ", null);
            for (Integer robot : robotsMoved) {
                this.appendSolutionText(Board.getColorShortL10N(robot.intValue()), COL_ROBOT[robot.intValue()]);
            }
            this.appendSolutionText(".\n", null);
        } else {
            //fourth hint: number of moves + which robots moved + last move
            this.appendSolutionText(MessageFormat.format(L10N.getString("msg.Hint.4.pattern"), numMoves, numRobots) + " ", null);
            for (Integer robot : robotsMoved) {
                this.appendSolutionText(Board.getColorShortL10N(robot.intValue()), COL_ROBOT[robot.intValue()]);
            }
            this.appendSolutionText(L10N.getString("msg.Hint.4.LastMove.text") + " ", null);
            final Move lastMove = this.computedSolutionList.get(this.computedSolutionIndex).getLastMove();
            this.appendSolutionText(Board.getColorShortL10N(lastMove.robotNumber), COL_ROBOT[lastMove.robotNumber]);
            this.appendSolutionText(lastMove.strDirectionL10N() + ".\n", null);
        }
        ++this.hintCounter;
    }
    
    private void runSolverTask() {
        this.refreshBoard();
        this.removeSolution();
        this.solverTask = new SolverTask();
        this.solverTask.execute();
    }
    
    private class SolverTask extends SwingWorker<Solver, Object> {
        @Override
        protected Solver doInBackground() throws Exception {
            final Solver solver = Solver.createInstance(Board.createClone(board));
            solver.setOptionSolutionMode((Solver.SOLUTION_MODE)jcomboOptSolutionMode.getSelectedItem());
            solver.setOptionAllowRebounds(jcheckOptAllowRebounds.isSelected());
            jtextSolution.setText(null);
            appendSolutionText(getSolverOptionsString(solver), null);
            appendSolutionTextCurrentGoal();
            appendSolutionText(L10N.getString("txt.ComputingSolutions.text") + "\n\n", null);
            solver.execute();
            return solver;
        }
        @Override
        protected void done() {
            String errorMsg = "";
            final SolverTask st = solverTask;
            if (this == st) {
                solverTask = null;
                try {
                    setSolution(this.get());
                }
                catch (CancellationException e) {
                    System.err.println(e.toString());
                }
                catch (Exception e) {
                    errorMsg = e.toString();
                    e.printStackTrace();
                }
            }
            if ( ! errorMsg.isEmpty()) {
                appendSolutionText("error:\n" + errorMsg + "\n", null);
            }
        }
    }

    
    
    private String addKeyBindingTooltip(AbstractButton button, String keyCodeStr, String tooltip, Action action) {
        final int keyCode = KeyStroke.getKeyStroke(keyCodeStr).getKeyCode();
        final int keyModifiers = KeyStroke.getKeyStroke(keyCodeStr).getModifiers();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, keyModifiers | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        final String actionMapKey = KeyEvent.getKeyText(keyCode) + "_action_key";
        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionMapKey);
        button.getActionMap().put(actionMapKey, action);
        button.addActionListener(action);
        
        String acceleratorDelimiter = UIManager.getString("MenuItem.acceleratorDelimiter");
        if (null == acceleratorDelimiter) { acceleratorDelimiter = "-"; }
        String acceleratorText = ""; 
        int modifiers = keyStroke.getModifiers();
        if (modifiers > 0) {
            acceleratorText = KeyEvent.getKeyModifiersText(modifiers);
            acceleratorText += acceleratorDelimiter;
        }
        acceleratorText += KeyEvent.getKeyText(keyCode);
        button.setToolTipText("<html>" + tooltip + " &nbsp; <small><strong>" + acceleratorText + "</strong></small></html>");
        return acceleratorText;
    }
    
    
    @SuppressWarnings("serial")
    private void createAndShowGUI(String windowTitle) {
        this.frame.setTitle(windowTitle);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final JPanel preparePanel = new JPanel();   //-----------------------------------------------
        final DesignGridLayout prepareLayout = new DesignGridLayout(preparePanel);
        
        final JButton jbutRandomLayout = new JButton();
        jbutRandomLayout.setText(L10N.getString("btn.RandomLayout.text"));
        this.addKeyBindingTooltip(jbutRandomLayout,
                L10N.getString("btn.RandomLayout.acceleratorkey"),
                L10N.getString("btn.RandomLayout.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        makeRandomBoardQuadrants();
                        refreshJComboPlaceRobot();
                        refreshJlistQuadrants();
                    }
                }
        );
        
        for (int i = 0;  i < 4;  ++i) {
            final JList jl = new JList(Board.QUADRANT_NAMES) {
                @Override
                public JToolTip createToolTip() {
                    return new QuadrantGoalsToolTip(this, Integer.parseInt(this.getToolTipText()));
                };
            };
            jl.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    list.setToolTipText(String.valueOf(index));
                    return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                };
            });
            jl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jl.setVisibleRowCount(1);
            jl.setSelectedIndex(this.board.getQuadrantNum(i));
            jl.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (true == doRefreshJlistQuadrants) {
                        makeBoardQuadrants();
                        refreshJComboPlaceRobot();
                    }
                }
            });
            this.jlistQuadrants[i] = jl;
        }
        
        final String[] strRobots = { "1", "2", "3", "4", "5" };
        this.jcomboRobots.setModel(new DefaultComboBoxModel(strRobots));
        this.jcomboRobots.setEditable(false);
        this.jcomboRobots.setActionCommand(AC_BOARD_ROBOTS);
        this.jcomboRobots.addActionListener(this);
        this.refreshJcomboRobots();
        
        final JButton jbutRemoveWalls = new JButton();
        jbutRemoveWalls.setText(L10N.getString("btn.RemoveWalls.text"));
        jbutRemoveWalls.setToolTipText(L10N.getString("btn.RemoveWalls.tooltip"));
        jbutRemoveWalls.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                board.removeWalls();
                board.setFreestyleBoard();
                refreshBoard();
            }
        });

        final JButton jbutRemoveGoals = new JButton();
        jbutRemoveGoals.setText(L10N.getString("btn.RemoveGoals.text"));
        jbutRemoveGoals.setToolTipText(L10N.getString("btn.RemoveGoals.tooltip"));
        jbutRemoveGoals.addActionListener(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                board.removeGoals();
                board.setFreestyleBoard();
                refreshBoard();
            }
        });

        final JButton jbutCopyBoardDumpToClipboard = new JButton();
        jbutCopyBoardDumpToClipboard.setText(L10N.getString("btn.CopyBoardDumpToClipboard.text"));
        this.addKeyBindingTooltip(jbutCopyBoardDumpToClipboard,
                L10N.getString("btn.CopyBoardDumpToClipboard.acceleratorkey"),
                L10N.getString("btn.CopyBoardDumpToClipboard.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                            String data = board.getGameDump();
                            clip.setContents(new StringSelection(data), null);
                            JOptionPane.showMessageDialog(frame,
                                    L10N.getString("msg.CopyBoardDumpToClipboard.OK.message"),
                                    L10N.getString("msg.CopyBoardDumpToClipboard.OK.title"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame,
                                    L10N.getString("msg.CopyBoardDumpToClipboard.Error.message") + "\n" + ex.toString(),
                                    L10N.getString("msg.CopyBoardDumpToClipboard.Error.title"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
        );

        final JButton jbutCreateBoardFromDump = new JButton();
        jbutCreateBoardFromDump.setText(L10N.getString("btn.CreateBoardFromDump.text"));
        this.addKeyBindingTooltip(jbutCreateBoardFromDump,
                L10N.getString("btn.CreateBoardFromDump.acceleratorkey"),
                L10N.getString("btn.CreateBoardFromDump.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            final String data = JOptionPane.showInputDialog(frame,
                                    L10N.getString("msg.CreateBoardFromDump.input.message"),
                                    L10N.getString("msg.CreateBoardFromDump.input.title"),
                                    JOptionPane.PLAIN_MESSAGE);
                            if ((null != data) && (0 != data.length())) {
                                final Board newBoard = Board.createBoardGameDump(data);
                                if (null != newBoard) {
                                    board = newBoard;
                                    if (board.isFreestyleBoard()) {
                                        jtabEditBoard.setSelectedIndex(1);
                                    }
                                    refreshBoard();
                                    refreshJcomboRobots();
                                    refreshJComboPlaceRobot();
                                    refreshJlistQuadrants();
                                    jspinWidth.getModel().setValue(Integer.valueOf(board.width));
                                    jspinHeight.getModel().setValue(Integer.valueOf(board.height));
                                } else {
                                    throw new IllegalArgumentException();   //show error message
                                }
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(frame,
                                    L10N.getString("msg.CreateBoardFromDump.Error.message"),
                                    L10N.getString("msg.CreateBoardFromDump.Error.title"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
        );

        final JButton jbutRotateBoardLeft = new JButton();
        jbutRotateBoardLeft.setText(getAnticlockwiseArrow(jbutRotateBoardLeft.getFont()));
        this.addKeyBindingTooltip(jbutRotateBoardLeft,
                L10N.getString("btn.RotateBoardLeft.acceleratorkey"),
                L10N.getString("btn.RotateBoardLeft.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        board = board.rotate90(false);
                        refreshBoard();
                        refreshJComboPlaceRobot();
                        refreshJlistQuadrants();
                    }
                }
        );

        final JButton jbutRotateBoardRight = new JButton();
        jbutRotateBoardRight.setText(getClockwiseArrow(jbutRotateBoardRight.getFont()));
        this.addKeyBindingTooltip(jbutRotateBoardRight,
                L10N.getString("btn.RotateBoardRight.acceleratorkey"),
                L10N.getString("btn.RotateBoardRight.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        board = board.rotate90(true);
                        refreshBoard();
                        refreshJComboPlaceRobot();
                        refreshJlistQuadrants();
                    }
                }
        );

        this.jspinWidth.setModel(new SpinnerNumberModel(Board.WIDTH_STANDARD, Board.WIDTH_MIN, Board.WIDTH_MAX, 1));
        this.jspinWidth.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final int width = ((SpinnerNumberModel)jspinWidth.getModel()).getNumber().intValue();
                if (width != board.width) {
                    makeFreestyleBoard();
                }
            }
        });
        this.jspinHeight.setModel(new SpinnerNumberModel(Board.HEIGHT_STANDARD, Board.HEIGHT_MIN, Board.HEIGHT_MAX, 1));
        this.jspinHeight.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final int height = ((SpinnerNumberModel)jspinHeight.getModel()).getNumber().intValue();
                if (height != board.height) {
                    makeFreestyleBoard();
                }
            }
        });

        final Vector<String> dataGoalRobots = new Vector<String>();
        for (int color = -1;  color < 4;  ++color) {
            dataGoalRobots.add(Board.getColorLongL10N(color));
        }
        this.jlistGoalRobots.setListData(dataGoalRobots);
        this.jlistGoalRobots.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.jlistGoalRobots.setSelectedIndex(0 + 1);
        this.jlistGoalRobots.setVisibleRowCount(dataGoalRobots.size());
        this.jlistGoalRobots.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                list.setToolTipText((index - 1) + ";" + jlistGoalShapes.getSelectedIndex());
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            };
        });
        final JScrollPane jscrollGoalRobots = new JScrollPane(this.jlistGoalRobots);

        final Vector<String> dataGoalShapes = new Vector<String>();
        for (int shape = 0;  shape < 4;  ++shape) {
            dataGoalShapes.add(Board.getGoalShapeL10N(shape));
        }
        this.jlistGoalShapes.setListData(dataGoalShapes);
        this.jlistGoalShapes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.jlistGoalShapes.setSelectedIndex(0);
        this.jlistGoalShapes.setVisibleRowCount(dataGoalShapes.size());
        this.jlistGoalShapes.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                list.setToolTipText((jlistGoalRobots.getSelectedIndex() - 1) + ";" + index);
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            };
        });
        final JScrollPane jscrollGoalShapes = new JScrollPane(this.jlistGoalShapes);

        final JPanel editBoardOriginalPanel = new JPanel();
        final DesignGridLayout editBoardOriginalLayout = new DesignGridLayout(editBoardOriginalPanel);
        final JLabel jlabelBoardTiles = new JLabel(L10N.getString("lbl.BoardTiles.text"));
        editBoardOriginalLayout.row().left().add(jlabelBoardTiles);
        editBoardOriginalLayout.row().grid().add(new JScrollPane(this.jlistQuadrants[0]), new JScrollPane(this.jlistQuadrants[1])).add(jbutRandomLayout, 2);
        editBoardOriginalLayout.row().grid().add(new JScrollPane(this.jlistQuadrants[3]), new JScrollPane(this.jlistQuadrants[2])).empty(2);

        final JPanel editBoardFreestylePanel = new JPanel();
        final DesignGridLayout editBoardFreestyleLayout = new DesignGridLayout(editBoardFreestylePanel);
        editBoardFreestyleLayout.row().grid().add(jbutRemoveWalls).add(jbutRemoveGoals);
        editBoardFreestyleLayout.emptyRow();
        editBoardFreestyleLayout.row().grid().add(new JLabel(L10N.getString("lbl.Width.text"))).addMulti(this.jspinWidth);
        editBoardFreestyleLayout.row().grid().add(new JLabel(L10N.getString("lbl.Height.text"))).addMulti(this.jspinHeight);
        editBoardFreestyleLayout.emptyRow();
        editBoardFreestyleLayout.row().grid().add(new JLabel(L10N.getString("lbl.ListGoalColors.text"))).add(new JLabel(L10N.getString("lbl.ListGoalShapes.text")));
        editBoardFreestyleLayout.row().grid().add(jscrollGoalRobots).add(jscrollGoalShapes);
        editBoardFreestyleLayout.emptyRow();
        editBoardFreestyleLayout.row().grid().add(jbutCopyBoardDumpToClipboard).add(jbutCreateBoardFromDump);

        this.jtabEditBoard.addTab(L10N.getString("tab.OriginalBoard.text"),
                null, editBoardOriginalPanel, L10N.getString("tab.OriginalBoard.tooltip"));
        this.jtabEditBoard.addTab(L10N.getString("tab.FreestyleBoard.text"),
                null, editBoardFreestylePanel, L10N.getString("tab.FreestyleBoard.tooltip"));
        this.jtabEditBoard.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                jcomboGameIDs.setEnabled( ! SwingGUI.this.isFreestyleBoard());
            }
        });
        
        prepareLayout.row().grid().add(new JLabel(L10N.getString("lbl.NumberOfRobots.text")), 2).add(this.jcomboRobots).empty();
        prepareLayout.row().grid().add(new JLabel(L10N.getString("lbl.RotateBoard.text")), 2).add(jbutRotateBoardLeft, jbutRotateBoardRight);
        prepareLayout.row().grid().add(this.jtabEditBoard);


        final JPanel optionsPanel = new JPanel();   //-----------------------------------------------
        final DesignGridLayout optionsLayout = new DesignGridLayout(optionsPanel);
        
        final Solver.SOLUTION_MODE[] solModes = Solver.SOLUTION_MODE.values();
        this.jcomboOptSolutionMode.setModel(new DefaultComboBoxModel(solModes));
        this.jcomboOptSolutionMode.setSelectedItem(Solver.SOLUTION_MODE.MAXIMUM);
        
        this.jcheckOptAllowRebounds.setText(L10N.getString("chk.AllowReboundMoves.text"));
        this.jcheckOptAllowRebounds.setSelected(true);

        this.jcheckOptShowColorNames.setText(L10N.getString("chk.ShowColorNames.text"));
        this.jcheckOptShowColorNames.setSelected(false);
        this.jcheckOptShowColorNames.setActionCommand(AC_SHOW_COLOR_NAMES);
        this.jcheckOptShowColorNames.addActionListener(this);

        this.jcheckOptShowOnlyActiveGoal.setText(L10N.getString("chk.ShowOnlyActiveGoal.text"));
        this.jcheckOptShowOnlyActiveGoal.setSelected(true);
        this.jcheckOptShowOnlyActiveGoal.setActionCommand(AC_SHOW_ACTIVE_GOAL);
        this.jcheckOptShowOnlyActiveGoal.addActionListener(this);

        this.jcheckOptShowSolutions.setText(L10N.getString("chk.ShowSolutions.text"));
        this.jcheckOptShowSolutions.setSelected(false);
        
        optionsLayout.row().grid().add(new JLabel(L10N.getString("lbl.SolverOptions.text")));
        optionsLayout.emptyRow();
        optionsLayout.row().grid().addMulti(new JLabel(L10N.getString("lbl.PreferSolutionWith.text")), this.jcomboOptSolutionMode);
        optionsLayout.row().grid().add(new JLabel(L10N.getString("lbl.NumberOfRobotsMoved.text")));
        optionsLayout.row().grid().add(new JLabel(" "));
        optionsLayout.row().grid().add(this.jcheckOptAllowRebounds);
        optionsLayout.emptyRow();
        optionsLayout.row().grid().add(new JSeparator());
        optionsLayout.emptyRow();
        optionsLayout.row().grid().add(new JLabel(L10N.getString("lbl.GUIOptions.text")));
        optionsLayout.emptyRow();
        optionsLayout.row().grid().add(this.jcheckOptShowColorNames);
        optionsLayout.row().grid().add(this.jcheckOptShowOnlyActiveGoal);
        optionsLayout.row().grid().add(this.jcheckOptShowSolutions);
        
        
        final JPanel playPanel = new JPanel();      //-----------------------------------------------
        final DesignGridLayout playLayout = new DesignGridLayout(playPanel);
        
        final JButton jbutRandomRobots = new JButton(L10N.getString("btn.RandomRobots.text"));
        this.addKeyBindingTooltip(jbutRandomRobots,
                L10N.getString("btn.RandomRobots.acceleratorkey"),
                L10N.getString("btn.RandomRobots.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        placeRobot = -1;
                        refreshJComboPlaceRobot();
                        updateBoardRandomRobots();
                    }
                }
        );
        
        final JButton jbutRandomGoal = new JButton(L10N.getString("btn.RandomGoal.text"));
        this.addKeyBindingTooltip(jbutRandomGoal,
                L10N.getString("btn.RandomGoal.acceleratorkey"),
                L10N.getString("btn.RandomGoal.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        selectGoal = false;
                        updateBoardRandomGoal();
                    }
                }
        );
        
        this.refreshJComboPlaceRobot();   //this.jcomboPlaceRobot
        this.jcomboPlaceRobot.setEditable(false);
        this.jcomboPlaceRobot.setActionCommand(AC_PLACE_ROBOT);
        this.jcomboPlaceRobot.addActionListener(this);
        String prtt = "<html>" + L10N.getString("cmb.PlaceRobot.tooltip") + " &nbsp; <small><strong>";
        final JButton jbutPlaceRobot = new JButton("place robot accelerator keys");
        jbutPlaceRobot.setPreferredSize(new Dimension());  //invisible button
        prtt += this.addKeyBindingTooltip(jbutPlaceRobot, L10N.getString("cmb.PlaceRobot.1.acceleratorkey"), "",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (jcomboPlaceRobot.getItemCount() > 1) { jcomboPlaceRobot.setSelectedIndex(1); }
                    }
                }
        );
        prtt += " &nbsp; " + this.addKeyBindingTooltip(jbutPlaceRobot, L10N.getString("cmb.PlaceRobot.2.acceleratorkey"), "",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (jcomboPlaceRobot.getItemCount() > 2) { jcomboPlaceRobot.setSelectedIndex(2); }
                    }
                }
        );
        prtt += " &nbsp; " + this.addKeyBindingTooltip(jbutPlaceRobot, L10N.getString("cmb.PlaceRobot.3.acceleratorkey"), "",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (jcomboPlaceRobot.getItemCount() > 3) { jcomboPlaceRobot.setSelectedIndex(3); }
                    }
                }
        );
        prtt += " &nbsp; " + this.addKeyBindingTooltip(jbutPlaceRobot, L10N.getString("cmb.PlaceRobot.4.acceleratorkey"), "",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (jcomboPlaceRobot.getItemCount() > 4) { jcomboPlaceRobot.setSelectedIndex(4); }
                    }
                }
        );
        prtt += " &nbsp; " + this.addKeyBindingTooltip(jbutPlaceRobot, L10N.getString("cmb.PlaceRobot.5.acceleratorkey"), "",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (jcomboPlaceRobot.getItemCount() > 5) { jcomboPlaceRobot.setSelectedIndex(5); }
                    }
                }
        );
        this.jcomboPlaceRobot.setToolTipText(prtt + "</strong></small></html>");
        
        final JButton jbutSelectGoal = new JButton(L10N.getString("btn.SelectGoal.text"));
        this.addKeyBindingTooltip(jbutSelectGoal,
                L10N.getString("btn.SelectGoal.acceleratorkey"),
                L10N.getString("btn.SelectGoal.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        selectGoal = !selectGoal;
                        refreshBoard();
                    }
                }
        );
        
        this.jcomboGameIDs.setModel(new DefaultComboBoxModel());
        this.jcomboGameIDs.setEditable(true);
        this.jcomboGameIDs.setActionCommand(AC_GAME_ID);
        this.jcomboGameIDs.addActionListener(this);
        
        this.jcomboSelectSolution.setModel(new DefaultComboBoxModel());
        this.jcomboSelectSolution.setPrototypeDisplayValue("99)  99/9/#####");  //longest string possible here
        this.jcomboSelectSolution.addItem(L10N.getString("cmb.SelectSolution.text"));
        this.jcomboSelectSolution.setEditable(false);
        this.jcomboSelectSolution.setActionCommand(AC_SELECT_SOLUTION);
        this.jcomboSelectSolution.addActionListener(this);
        this.jcomboSelectSolution.setToolTipText(L10N.getString("cmb.SelectSolution.tooltip"));
        
        this.jbutSolutionHint.setText(L10N.getString("btn.Hint.text"));
        this.addKeyBindingTooltip(this.jbutSolutionHint,
                L10N.getString("btn.Hint.acceleratorkey"),
                L10N.getString("btn.Hint.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        showHint();
                    }
                }
        );
        this.jbutNextMove.setText(getRightwardsArrow(this.jbutNextMove.getFont()));
        this.addKeyBindingTooltip(this.jbutNextMove,
                L10N.getString("btn.NextMove.acceleratorkey"),
                L10N.getString("btn.NextMove.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        showNextMove(true);
                    }
                }
        );
        this.jbutAllMoves.setText(getRightwardsBarArrow(this.jbutAllMoves.getFont()));
        this.addKeyBindingTooltip(this.jbutAllMoves,
                L10N.getString("btn.AllMoves.acceleratorkey"),
                L10N.getString("btn.AllMoves.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        while (jbutNextMove.isEnabled()) {
                            showNextMove(true);
                        }
                    }
                }
        );
        this.jbutPrevMove.setText(getLeftwardsArrow(this.jbutPrevMove.getFont()));
        this.addKeyBindingTooltip(this.jbutPrevMove,
                L10N.getString("btn.PrevMove.acceleratorkey"),
                L10N.getString("btn.PrevMove.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        showPrevMove(true);
                    }
                }
        );
        this.jbutNoMoves.setText(getLeftwardsBarArrow(this.jbutNoMoves.getFont()));
        this.addKeyBindingTooltip(this.jbutNoMoves,
                L10N.getString("btn.NoMoves.acceleratorkey"),
                L10N.getString("btn.NoMoves.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        while (jbutPrevMove.isEnabled()) {
                            showPrevMove(true);
                        }
                    }
                }
        );
        
        this.jtextSolution.setEditable(false);
        ((DefaultCaret)this.jtextSolution.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // autoscroll
        final JPanel panelSolutionText = new JPanel(new BorderLayout());
        panelSolutionText.add(this.jtextSolution, BorderLayout.CENTER);
        final JScrollPane scrollSolutionText = new JScrollPane(panelSolutionText, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSolutionText.setPreferredSize(new Dimension(10, 10)); //workaround for layout problem ?!?!
        scrollSolutionText.setMinimumSize(new Dimension(10, 10));   //workaround for layout problem ?!?!
        
        playLayout.row().grid().addMulti(new JLabel(L10N.getString("lbl.SetStartingPosition.text")), jbutPlaceRobot);
        playLayout.row().grid().add(jbutRandomRobots).add(jbutRandomGoal);
        playLayout.row().grid().add(this.jcomboPlaceRobot).add(jbutSelectGoal);
        playLayout.row().grid().add(new JLabel(L10N.getString("lbl.GameID.text"))).add(this.jcomboGameIDs, 3);
        playLayout.emptyRow();
        playLayout.row().grid().add(new JSeparator());
        playLayout.row().grid().add(new JLabel(L10N.getString("lbl.ShowComputedSolutions.text")));
        playLayout.row().grid().add(this.jcomboSelectSolution).add(this.jbutSolutionHint);
        playLayout.row().grid().add(this.jbutNoMoves).add(this.jbutPrevMove).add(this.jbutNextMove).add(this.jbutAllMoves);
        playLayout.row().grid().add(scrollSolutionText);
        
        
        this.jtabPreparePlay.addTab(L10N.getString("tab.PrepareBoard.text"), preparePanel);
        this.jtabPreparePlay.addTab(L10N.getString("tab.Options.text"), optionsPanel);
        this.jtabPreparePlay.addTab(L10N.getString("tab.PlayGame.text"), playPanel);
        this.jtabPreparePlay.setSelectedIndex(2);   //Play
        this.jtabPreparePlay.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                SwingGUI.this.refreshBoard(); // repaint the entire board
                if (SwingGUI.this.isModePlay()) {
                    SwingGUI.this.updateBoardGetRobots(); // start solver thread
                } else {
                    SwingGUI.this.removeSolution(); // stop solver thread
                    if (SwingGUI.this.isModeEditBoard()) {
                        SwingGUI.this.board.setRobots(SwingGUI.this.currentPosition);
                    }
                }
            }
        });
        
        this.refreshBoardCells(); // performs frame.pack()
        this.frame.setVisible(true);
        
        try {
            final String defLaf = UIManager.getLookAndFeel().getClass().getName();
            final String sysLaf = UIManager.getSystemLookAndFeelClassName();
            System.out.println("default L&F: " + defLaf);
            System.out.println("system  L&F: " + sysLaf);
            if ((false == defLaf.equals(sysLaf)) && (false == defLaf.toLowerCase().contains("nimbus"))) {
                System.out.println("activating system L&F now.");
                UIManager.setLookAndFeel(sysLaf);
                SwingUtilities.updateComponentTreeUI(this.frame);
                this.frame.pack();
                System.out.println("successfully activated system L&F.");
            }
        } catch (Exception e) {
            System.out.println("ERROR: could not activate system L&F: " + e.toString());
        }
        
        this.updateBoardGetRobots();
    }
    
    private void refreshJComboPlaceRobot() {
        this.jcomboPlaceRobot.removeAllItems();
        this.jcomboPlaceRobot.addItem(L10N.getString("cmb.PlaceRobot.text"));
        final int numRobots = this.board.getRobotPositions().length;
        for (int i = 0;  i < numRobots;  ++i) {
            this.jcomboPlaceRobot.addItem(Board.getColorLongL10N(i));
        }
        this.jcomboPlaceRobot.setSelectedIndex(0);
    }
    
    private boolean isModePlay() {
        return (this.jtabPreparePlay.getSelectedIndex() == 2);
    }
    
    private boolean isModeOptions() {
        return (this.jtabPreparePlay.getSelectedIndex() == 1);
    }
    
    private boolean isFreestyleBoard() {
        return (this.jtabEditBoard.getSelectedIndex() == 1);
    }
    
    private boolean isModeEditBoard() {
        return ((this.jtabPreparePlay.getSelectedIndex() == 0) && this.isFreestyleBoard());
    }
    
    private void refreshJcomboRobots() {
        final String tmp = this.jcomboRobots.getActionCommand();
        this.jcomboRobots.setActionCommand("");
        this.jcomboRobots.setSelectedIndex(this.board.getRobotPositions().length - 1);
        this.jcomboRobots.setActionCommand(tmp);
    }

    private void refreshJlistQuadrants() {
        this.doRefreshJlistQuadrants = false;
        for (int i = 0;  i < 4;  ++i) {
            final JList jl = this.jlistQuadrants[i];
            jl.setSelectedIndex(this.board.getQuadrantNum(i));
            jl.ensureIndexIsVisible(jl.getSelectedIndex());
        }
        this.doRefreshJlistQuadrants = true;
    }

    private void makeFreestyleBoard() {
        final int width = ((SpinnerNumberModel)jspinWidth.getModel()).getNumber().intValue();
        final int height = ((SpinnerNumberModel)jspinHeight.getModel()).getNumber().intValue();
        System.out.println("w=" + width + " h=" + height);
        board = Board.createBoardFreestyle(board, width, height, board.getNumRobots());
        this.refreshBoard();
    }

    private void refreshBoardCells() {
        if ((this.boardCellsWidth != this.board.width) || (this.boardCellsHeight != this.board.height)) {
            this.boardCellsWidth = this.board.width;
            this.boardCellsHeight = this.board.height;
            this.boardCells = new BoardCell[this.board.size];
            final JPanel boardPanel = new JPanel(new GridLayoutSquare(this.boardCellsHeight, this.boardCellsWidth));
            for (int i = 0;  i < this.boardCells.length;  ++i) {
                boardCells[i] = new BoardCell(i);
                boardPanel.add(this.boardCells[i]);
            }
            this.frame.getContentPane().removeAll();
            this.frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
            this.frame.getContentPane().add(this.jtabPreparePlay, BorderLayout.EAST);
            this.frame.pack();
        }
    }

    private void refreshBoard() {
        //repaint board
        this.refreshBoardCells();
        for (JComponent comp : this.boardCells) {
            comp.repaint();
        }
        if ( ! this.isFreestyleBoard()) {
            //manipulate combobox of game IDs
            final String ac = this.jcomboGameIDs.getActionCommand();
            this.jcomboGameIDs.setActionCommand("");
            String newGameID = this.board.getGameID();
            if (this.jcomboGameIDs.getSelectedIndex() < 0) {    //not a list item
                Object item = this.jcomboGameIDs.getSelectedItem();
                if (null != item) {
                    if (null != Board.createBoardGameID(item.toString())) {
                        newGameID = item.toString();
                    }
                }
            }
            final int itemCount = this.jcomboGameIDs.getItemCount();
            boolean itemInList = false;
            for (int i = 0;  i < itemCount;  ++i) {
                final String itemStr =  this.jcomboGameIDs.getItemAt(i).toString();
                if (itemStr.equals(newGameID)) {
                    itemInList = true;
                    break;
                }
            }
            if (false == itemInList) {
                this.jcomboGameIDs.addItem(newGameID);
            }
            this.jcomboGameIDs.setSelectedItem(newGameID);
            this.jcomboGameIDs.setActionCommand(ac);
        }
    }
    
    
    private void refreshBoard(final Move step) {
        for (int pos : step.pathMap.keySet()) {
            this.boardCells[pos].repaint();
        }
    }
    
    private void refreshButtons() {
        if (null == this.computedSolutionList) {
            this.jcomboSelectSolution.setEnabled(false);
            this.jbutSolutionHint.setEnabled(false);
            this.jbutNextMove.setEnabled(false);
            this.jbutPrevMove.setEnabled(false);
            this.jbutAllMoves.setEnabled(false);
            this.jbutNoMoves.setEnabled(false);
        } else {
            if (this.computedSolutionList.get(this.computedSolutionIndex).size() > 0) {
                this.jcomboSelectSolution.setEnabled((this.computedSolutionList.size() > 1));
                this.jbutSolutionHint.setEnabled(true);
            }
            this.jbutNextMove.setEnabled(true);
            this.jbutPrevMove.setEnabled(true);
            this.jbutAllMoves.setEnabled(true);
            this.jbutNoMoves.setEnabled(true);
            if (this.moves.size() == this.computedSolutionList.get(this.computedSolutionIndex).size()) {
                this.jbutNextMove.setEnabled(false);
                this.jbutAllMoves.setEnabled(false);
            }
            if (this.moves.isEmpty()) {
                this.jbutPrevMove.setEnabled(false);
                this.jbutNoMoves.setEnabled(false);
            }
        }
    }
    
    private void showNextMove(final boolean doPrint) {
        final Move step = this.computedSolutionList.get(this.computedSolutionIndex).getNextMove();
        if (null != step) {
            this.moves.add(step);
            this.currentPosition[step.robotNumber] = step.newPosition;
            this.showMove(step, doPrint);
            if (doPrint) {
                if (step.equals(this.computedSolutionList.get(this.computedSolutionIndex).getLastMove())) {
                    showSolutionShort(this.computedSolutionList.get(this.computedSolutionIndex));
                }
            }
        }
    }
    private void showPrevMove(final boolean doPrint) {
        final Move step = this.computedSolutionList.get(this.computedSolutionIndex).getPrevMove();
        if (null != step) {
            this.moves.remove(step);
            this.currentPosition[step.robotNumber] = step.oldPosition;
            this.showMove(step, doPrint);
        }
    }
    private void showMove(final Move step, final boolean doPrint) {
        if (doPrint) {
            this.appendSolutionText((step.stepNumber + 1) + ": ", null);
            final Font font = this.jtextSolution.getFont();
            final String arrow;
            switch (step.direction) {
            case Board.EAST:    arrow = getRightwardsArrow(font);   break;
            case Board.WEST:    arrow = getLeftwardsArrow(font);    break;
            case Board.NORTH:   arrow = getUpwardsArrow(font);      break;
            case Board.SOUTH:   arrow = getDownwardsArrow(font);    break;
            default:            arrow = "?"; break; // this should never happen
            }
            this.appendSolutionText(" " + arrow + " ", COL_ROBOT[step.robotNumber]);
            this.appendSolutionText(" " + Board.getColorLongL10N(step.robotNumber) + " " + step.strDirectionL10Nlong()
                    + (this.computedSolutionList.get(this.computedSolutionIndex).isRebound(step) ? " " + L10N.getString("txt.Rebound.text") : "")
                    + "\n", null);
            //System.out.println(step.toString());
        }
        this.refreshButtons();
        this.refreshBoard(step);
    }
    
    private void showSolutionShort(final Solution solution) {
        solution.resetMoves();
        int robot = -1;
        Move move;
        while (null != (move = solution.getNextMove())) {
            if (robot != move.robotNumber) {
                this.appendSolutionText("\n", null);
                this.appendSolutionText(Board.getColorLongL10N(move.robotNumber), COL_ROBOT[move.robotNumber]);
                this.appendSolutionText(":", null);
                robot = move.robotNumber;
            }
            this.appendSolutionText(" " + move.strDirectionL10N(), null);
        }
        this.appendSolutionText("\n", null);
    }
    
    
    private void handleGameID() {
        final String myGameID = this.board.getGameID();
        final String newGameID = this.jcomboGameIDs.getSelectedItem().toString();
        if (!newGameID.equals(myGameID)) {
            final Board newBoard = Board.createBoardGameID(newGameID);
            if (null != newBoard) {
                this.board = newBoard;
                this.refreshJComboPlaceRobot();
                this.refreshJcomboRobots();
                this.refreshJlistQuadrants();
                this.updateBoardGetRobots();
            } else {
                appendSolutionText(MessageFormat.format(L10N.getString("msg.ErrorGameID.pattern"), newGameID) + "\n", null);
            }
        }
    }
    
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (AC_BOARD_ROBOTS.equals(e.getActionCommand())) {
                this.board.setRobots(this.jcomboRobots.getSelectedIndex() + 1);
                this.refreshJComboPlaceRobot();
        } else if (AC_SELECT_SOLUTION.equals(e.getActionCommand())) {
            this.selectSolution(this.jcomboSelectSolution.getSelectedIndex(), this.jcomboSelectSolution.getSelectedItem().toString());
        } else if (AC_PLACE_ROBOT.equals(e.getActionCommand())) {
            this.placeRobot = this.jcomboPlaceRobot.getSelectedIndex() - 1;     //item #0 is "Place robot"
        } else if (AC_GAME_ID.equals(e.getActionCommand())) {
            this.handleGameID();
        } else if (AC_SHOW_COLOR_NAMES.equals(e.getActionCommand())) {
            this.refreshBoard();
        } else if (AC_SHOW_ACTIVE_GOAL.equals(e.getActionCommand())) {
            this.refreshBoard();
        }
    }
    
    private class BoardCell extends JPanel implements MouseListener, MouseMotionListener {
        private static final long serialVersionUID = 1L;
        private static final int PREF_WIDTH = SwingGUI.ICON_SIZE + 2 + 2;   // preferred width
        private static final int PREF_HEIGHT = SwingGUI.ICON_SIZE + 2 + 2;  // preferred height
        private static final int H_WALL_DIVISOR = 12;   // horizontal walls: height / H_WALL_DIVISOR
        private static final int V_WALL_DIVISOR = 12;   // vertical walls: width / vWallDivisor
        
        private final int boardPosition;
        
        private boolean isMouseInside;
        private boolean isMouseNorth, isMouseEast, isMouseSouth, isMouseWest;
        
        public BoardCell(int boardPosition) {
            super();
            this.boardPosition = boardPosition;
            this.isMouseInside = false;
            this.setPreferredSize(new Dimension(PREF_WIDTH, PREF_HEIGHT));
            this.setMinimumSize(new Dimension(10, 10));
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }
        
        @Override
        protected void paintComponent(Graphics graphics) {
            final Graphics2D g2d = (Graphics2D) graphics.create();
            final Dimension size = this.getSize();
            final int height = size.height;
            final int width  = size.width;
            final int hWallWidth = height / H_WALL_DIVISOR;
            final int vWallWidth = width / V_WALL_DIVISOR;
            
            this.setToolTipText(null);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            
            // fill background (walls and center)
            g2d.setColor(COL_BACKGROUND);
            g2d.fillRect(0, 0, width, height);
            
            // fill center
            g2d.setPaint(new GradientPaint(0, height-1, COL_CELL1, width-1, 0, COL_CELL2));
            g2d.fillRect(vWallWidth, hWallWidth, width - vWallWidth * 2, height - hWallWidth * 2);
            
            // fill the 4 walls
            g2d.setColor(COL_WALL);
            if (true == board.isWall(this.boardPosition, Board.NORTH)) {
                g2d.fillRect(0, 0, width, hWallWidth);
            }
            if (true == board.isWall(this.boardPosition, Board.EAST)) {
                g2d.fillRect(width - vWallWidth, 0, width, height);
            }
            if (true == board.isWall(this.boardPosition, Board.SOUTH)) {
                g2d.fillRect(0, height - hWallWidth, width, height);
            }
            if (true == board.isWall(this.boardPosition, Board.WEST)) {
                g2d.fillRect(0, 0, vWallWidth, height);
            }
            
            // fill walls highlighted by mouse
            if ((true == isModeEditBoard()) && (true == this.isMouseInside)) {
                g2d.setColor(Color.WHITE);
                if (true == this.isMouseNorth) {
                    g2d.fillRect(0, 0, width, hWallWidth);
                }
                if (true == this.isMouseEast) {
                    g2d.fillRect(width - vWallWidth, 0, width, height);
                }
                if (true == this.isMouseSouth) {
                    g2d.fillRect(0, height - hWallWidth, width, height);
                }
                if (true == this.isMouseWest) {
                    g2d.fillRect(0, 0, vWallWidth, height);
                }
            }
            
            // paint the goal
            final Board.Goal goal;
            if ((isModePlay() || isModeOptions()) && !selectGoal && jcheckOptShowOnlyActiveGoal.isSelected()) {
                if (board.getGoal().position == this.boardPosition) {
                    goal = board.getGoal();
                } else {
                    goal = null;
                }
            } else {
                goal = board.getGoalAt(this.boardPosition);
            }
            if (null != goal) {
                final Icon goalIcon = new GoalIcon(
                        width - 2 * vWallWidth,
                        height - 2 * hWallWidth,
                        goal, jcheckOptShowColorNames.isSelected());
                goalIcon.paintIcon(this, g2d, vWallWidth, hWallWidth);
                this.setToolTipText(L10N.getString("txt.Goal.text") + " - " + Board.getColorLongL10N(goal.robotNumber) + " - " + Board.getGoalShapeL10N(goal.shape));
            }
            
            // paint the robot paths
            if (isModePlay()) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                final Stroke oldStroke = g2d.getStroke();
                final int pathWidth = Math.min(hWallWidth, vWallWidth);
                g2d.setStroke(new BasicStroke(pathWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
                for (Move step : moves) {
                    final Integer path = step.pathMap.get(Integer.valueOf(this.boardPosition));
                    if (null != path) {
                        g2d.setColor(COL_ROBOT[step.robotNumber]);
                        final int halfHeight = (height >> 1) + ((step.robotNumber - 2) * pathWidth);
                        final int halfWidth = (width >> 1) + ((step.robotNumber - 2) * pathWidth);
                        if ((path.intValue() & Move.PATH_NORTH) != 0) {
                            g2d.drawLine(halfWidth, 0, halfWidth, halfHeight);
                        }
                        if ((path.intValue() & Move.PATH_SOUTH) != 0) {
                            g2d.drawLine(halfWidth, halfHeight, halfWidth, height-1);
                        }
                        if ((path.intValue() & Move.PATH_EAST) != 0) {
                            g2d.drawLine(halfWidth, halfHeight, width-1, halfHeight);
                        }
                        if ((path.intValue() & Move.PATH_WEST) != 0) {
                            g2d.drawLine(0, halfHeight, halfWidth, halfHeight);
                        }
                    }
                }
                g2d.setStroke(oldStroke);
            }
            
            // paint the robots
            if (isModePlay()) {
                for (int i = 0; i < currentPosition.length; ++i) {
                    if (currentPosition[i] == this.boardPosition) {
                        final Icon robotIcon = new RobotIcon(
                                width - 2 * vWallWidth,
                                height - 2 * hWallWidth,
                                i, jcheckOptShowColorNames.isSelected());
                        robotIcon.paintIcon(this, g2d, vWallWidth, hWallWidth);
                        this.setToolTipText(L10N.getString("txt.Robot.text") + " - " + Board.getColorLongL10N(i));
                        break;
                    }
                }
            }
            g2d.dispose();
        }
        
        private void repaintOther(final int boardPos) {
            if ((boardPos >= 0) && (boardPos < boardCells.length)) {
                boardCells[boardPos].repaint();
            }
        }
        
        //implements MouseListener
        @Override
        public void mouseClicked(MouseEvent e) { /* NO-OP */ }
        @Override
        public void mouseEntered(MouseEvent e) {
            this.isMouseInside = true;
            this.repaint();
        }
        @Override
        public void mouseExited(MouseEvent e) {
            this.isMouseInside = false;
            this.repaint();
        }
        @Override
        public void mousePressed(MouseEvent e) {
            this.maybeShowPopup(e);
            if (placeRobot >= 0) {
                this.doPlaceRobot(placeRobot);
            }
            if (true == selectGoal) {
                this.doSelectGoal();
            }
            if ((true == isModeEditBoard()) && (true == this.isMouseInside)) {
                // set wall/goal : mouse button 1 and NOT shift key down
                // remove wall/goal : other mouse button or shift key down
                final boolean doSet = ((e.getButton() == MouseEvent.BUTTON1) && (0 == (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK)));
                if (true == this.isMouseNorth) {
                    board.setWall(this.boardPosition, "N", doSet);
                    board.setFreestyleBoard();
                    this.repaintOther(this.boardPosition - board.width);
                }
                if (true == this.isMouseEast) {
                    board.setWall(this.boardPosition, "E", doSet);
                    board.setFreestyleBoard();
                    this.repaintOther(this.boardPosition + 1);
                }
                if (true == this.isMouseSouth) {
                    board.setWall(this.boardPosition, "S", doSet);
                    board.setFreestyleBoard();
                    this.repaintOther(this.boardPosition + board.width);
                }
                if (true == this.isMouseWest) {
                    board.setWall(this.boardPosition, "W", doSet);
                    board.setFreestyleBoard();
                    this.repaintOther(this.boardPosition - 1);
                }
                if ((false == this.isMouseNorth) && (false == this.isMouseEast) && (false == this.isMouseSouth) && (false == this.isMouseWest)) {
                    if (true == doSet) {
                        board.addGoal(this.boardPosition, jlistGoalRobots.getSelectedIndex()-1, jlistGoalShapes.getSelectedIndex());
                    } else {
                        board.removeGoal(this.boardPosition);
                    }
                }
                this.repaint();
            }
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            this.maybeShowPopup(e);
        }
        @Override
        public void mouseDragged(MouseEvent e) { /* NO-OP */ }
        @Override
        public void mouseMoved(MouseEvent e) {
            if ((true == isModeEditBoard()) && (true == this.isMouseInside)) {
                final Dimension size = this.getSize();
                final int mouseX = e.getX();
                this.isMouseWest = false;
                this.isMouseEast = false;
                if (mouseX <= (size.width >> 2)) {
                    this.isMouseWest = true;
                } else if (mouseX >= ((size.width * 3) >> 2)) {
                    this.isMouseEast = true;
                }
                final int mouseY = e.getY();
                this.isMouseNorth = false;
                this.isMouseSouth = false;
                if (mouseY <= (size.height >> 2)) {
                    this.isMouseNorth = true;
                } else if (mouseY >= ((size.height * 3) >> 2)) {
                    this.isMouseSouth = true;
                }
                this.repaint();
            }
        }
        
        private void maybeShowPopup(MouseEvent e) {
            if (isModePlay() && e.isPopupTrigger()) {
                popupMenu.removeAll();
                final int numRobots = board.getNumRobots();
                for (int i = 0;  i < numRobots;  ++i) {
                    final int robot = i;
                    final Action action = new AbstractAction(
                            L10N.getString("txt.Place.text") + " "
                                    + L10N.getString("txt.Robot.text") + " - "
                                    + Board.getColorLongL10N(i),
                            new RobotIcon(robot, jcheckOptShowColorNames.isSelected())) {
                        private static final long serialVersionUID = 5584260141986571387L;
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            doPlaceRobot(robot);
                        }
                    };
                    popupMenu.add(action);
                }
                final Board.Goal goal = board.getGoalAt(this.boardPosition);
                if (null != goal) {
                    popupMenu.addSeparator();
                    final Action action = new AbstractAction(
                            L10N.getString("txt.Select.text") + " "
                                    + L10N.getString("txt.Goal.text") + " - "
                                    + Board.getColorLongL10N(goal.robotNumber)
                                    + " - "
                                    + Board.getGoalShapeL10N(goal.shape),
                            new GoalIcon(goal, jcheckOptShowColorNames.isSelected())) {
                        private static final long serialVersionUID = 2813443733253766305L;
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            doSelectGoal();
                        }
                    };
                    popupMenu.add(action);
                }
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        private void doPlaceRobot(final int robot) {
            board.setRobots(currentPosition);
            if (board.setRobot(robot, this.boardPosition, true)) {
                updateBoardGetRobots();
            }
            placeRobot = -1;
            refreshJComboPlaceRobot();
        }

        private void doSelectGoal() {
            if (board.setGoal(this.boardPosition)) {
                selectGoal = false;
                board.setRobots(currentPosition);
                updateBoardGetRobots();
            }
        }
    }

    public static class GoalIcon implements Icon {
        private final int width, height;
        private final Board.Goal goal;
        private final boolean drawColorNames;

        public GoalIcon(final Board.Goal goal, final boolean drawColorNames) {
            this(SwingGUI.ICON_SIZE, SwingGUI.ICON_SIZE, goal, drawColorNames);
        }

        public GoalIcon(final int width, final int height, final Board.Goal goal, final boolean drawColorNames) {
            this.width = width;
            this.height = height;
            this.goal = goal;
            this.drawColorNames = drawColorNames;
        }

        @Override
        public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
            final Graphics2D g2d = (Graphics2D) g.create();
            g2d.translate(x, y);
            final Paint thePaint;
            if (this.goal.robotNumber < 0) {
                final float fStep = 1.0f / (COL_ROBOT.length-1);
                final float[] fractions = new float[COL_ROBOT.length];
                for (int i = 1; i < fractions.length; ++i) {
                    fractions[i] = fractions[i - 1] + fStep;
                }
                thePaint = new LinearGradientPaint(0, 0, 0, this.height, fractions, COL_ROBOT, MultipleGradientPaint.CycleMethod.REPEAT);
            } else {
                thePaint = new GradientPaint(0, 0, COL_ROBOT[this.goal.robotNumber], 0, this.height, Color.DARK_GRAY);
            }
            g2d.setPaint(thePaint);
            final Shape outerShape = new Rectangle2D.Double(0, 0, this.width, this.height);
            final Shape innerShape;
            switch (goal.shape) {
            case Board.GOAL_SQUARE:
                innerShape = new Rectangle2D.Double(
                        Math.round(this.width * (1.0d/4.0d)), Math.round(this.height * (1.0d/4.0d)),
                        Math.round(this.width * (2.0d/4.0d) - 1), Math.round(this.height * (2.0d/4.0d) - 1) );
                break;
            case Board.GOAL_TRIANGLE:
                final Polygon triangle = new Polygon();
                triangle.addPoint(this.width     / 5, this.height * 4 / 5);
                triangle.addPoint(this.width * 4 / 5, this.height * 4 / 5);
                triangle.addPoint(this.width    >> 1, this.height     / 5);
                innerShape = triangle;
                break;
            case Board.GOAL_HEXAGON:
                final Polygon hexagon = new Polygon();
                hexagon.addPoint(this.width     / 6, this.height >> 1);
                hexagon.addPoint(this.width * 2 / 6, this.height     / 5);
                hexagon.addPoint(this.width * 4 / 6, this.height     / 5);
                hexagon.addPoint(this.width * 5 / 6, this.height >> 1);
                hexagon.addPoint(this.width * 4 / 6, this.height * 4 / 5);
                hexagon.addPoint(this.width * 2 / 6, this.height * 4 / 5);
                innerShape = hexagon;
                break;
            default:    //case Board.GOAL_CIRCLE:
                innerShape = new Ellipse2D.Double(
                        this.width * (1.0d/5.0d), this.height * (1.0d/5.0d),
                        this.width * (3.0d/5.0d), this.height * (3.0d/5.0d) );
                break;
            }
            final Area area = new Area(outerShape);
            area.subtract(new Area(innerShape));
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fill(area);
            if (this.drawColorNames) {
                final String goalColorShort = Board.getColorShortL10N(goal.robotNumber);
                g2d.setColor(Color.BLACK);
                g2d.drawChars(goalColorShort.toCharArray(), 0, 1, width / 2 - 3, height / 2 + 3);
            }
            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return this.width;
        }
        @Override
        public int getIconHeight() {
            return this.height;
        }
    }

    public static class RobotIcon implements Icon {
        private final int width, height, robot;
        private final boolean drawColorNames;

        public RobotIcon(final int robot, final boolean drawColorNames) {
            this(SwingGUI.ICON_SIZE, SwingGUI.ICON_SIZE, robot, drawColorNames);
        }

        public RobotIcon(final int width, final int height, final int robot, final boolean drawColorNames) {
            this.width = width;
            this.height = height;
            this.robot = robot;
            this.drawColorNames = drawColorNames;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            final Graphics2D g2d = (Graphics2D) g.create();
            g2d.translate(x, y);
            final Paint fillPaint = new GradientPaint(0, 0, COL_ROBOT[this.robot], 0, height-1, Color.DARK_GRAY);
            final Color outlineColor = Color.BLACK;
            Polygon shapeFoot = new Polygon();
            shapeFoot.addPoint(this.width / 2 - 1, this.height * 3 / 4 - 1);
            shapeFoot.addPoint(0, this.height - 1);
            shapeFoot.addPoint(this.width - 1, this.height - 1);
            final Ellipse2D.Double shapeBody = new Ellipse2D.Double(
                    this.width / 5.5,  0,  this.width / 5.5 * 3.5,  this.height - 1);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setPaint(fillPaint);    g2d.fill(shapeFoot);
            g2d.setColor(outlineColor); g2d.draw(shapeFoot);
            g2d.setPaint(fillPaint);    g2d.fill(shapeBody);
            g2d.setColor(outlineColor); g2d.draw(shapeBody);
            if (this.drawColorNames) {
                g2d.setColor(Color.WHITE);
                g2d.drawChars(Board.getColorShortL10N(this.robot).toCharArray(), 0, 1, width / 2 - 3, height / 2 + 3);
            }
            g2d.dispose();
        }

        @Override
        public int getIconWidth() {
            return this.width;
        }
        @Override
        public int getIconHeight() {
            return this.height;
        }
    }

    private class QuadrantGoalsToolTip extends JToolTip {
        private static final long serialVersionUID = -8631408017675104144L;

        public QuadrantGoalsToolTip(final JComponent c, final int staticQuadrantNumber) {
            super();
            this.setComponent(c);

            final JPanel iconPanel = new JPanel(null);
            iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.X_AXIS));
            final List<Board.Goal> goals = Board.getStaticQuadrantGoals(staticQuadrantNumber);
            for(Board.Goal goal : goals) {
                final Icon icon = new GoalIcon(goal, SwingGUI.this.jcheckOptShowColorNames.isSelected());
                iconPanel.add(new JLabel(icon));
            }
            this.setLayout(new BorderLayout());
            this.add(iconPanel);
            this.setUI(new ToolTipUI() {
                @Override
                public Dimension getMinimumSize(JComponent c) {
                    return c.getLayout().minimumLayoutSize(c);
                }
                @Override
                public Dimension getPreferredSize(JComponent c) {
                    return c.getLayout().preferredLayoutSize(c);
                }
                @Override
                public Dimension getMaximumSize(JComponent c) {
                    return this.getPreferredSize(c);
                }
            });
        }
    }

    private class GoalToolTip extends JToolTip {
        private static final long serialVersionUID = 669015603188699556L;

        public GoalToolTip(final JComponent c, final int robotNumber, final int shape) {
            super();
            this.setComponent(c);

            final JPanel iconPanel = new JPanel(null);
            iconPanel.setLayout(new BoxLayout(iconPanel, BoxLayout.X_AXIS));
            final Icon icon = new GoalIcon(board.new Goal(0, 0, robotNumber, shape), SwingGUI.this.jcheckOptShowColorNames.isSelected());
            iconPanel.add(new JLabel(icon));
            this.setLayout(new BorderLayout());
            this.add(iconPanel);
            this.setUI(new ToolTipUI() {
                @Override
                public Dimension getMinimumSize(JComponent c) {
                    return c.getLayout().minimumLayoutSize(c);
                }
                @Override
                public Dimension getPreferredSize(JComponent c) {
                    return c.getLayout().preferredLayoutSize(c);
                }
                @Override
                public Dimension getMaximumSize(JComponent c) {
                    return this.getPreferredSize(c);
                }
            });
        }
    }

    private static String getLeftwardsArrow(final Font font) {
        if (font.canDisplay('\u2190'))  return "\u2190";
        else                            return "<";
    }
    private static String getUpwardsArrow(final Font font) {
        if (font.canDisplay('\u2191'))  return "\u2191";
        else                            return "^";
    }
    private static String getRightwardsArrow(final Font font) {
        if (font.canDisplay('\u2192'))  return "\u2192";
        else                            return ">";
    }
    private static String getDownwardsArrow(final Font font) {
        if (font.canDisplay('\u2193'))  return "\u2193";
        else                            return "v";
    }
    private static String getLeftwardsBarArrow(final Font font) {
        if (font.canDisplay('\u21e4'))  return "\u21e4";
        else                            return "|<";
    }
    private static String getRightwardsBarArrow(final Font font) {
        if (font.canDisplay('\u21e5'))  return "\u21e5";
        else                            return ">|";
    }
    private static String getAnticlockwiseArrow(final Font font) {
        if (font.canDisplay('\u21ba'))  return "\u21ba";
        else                            return "<";
    }
    private static String getClockwiseArrow(final Font font) {
        if (font.canDisplay('\u21bb'))  return "\u21bb";
        else                            return ">";
    }
}
