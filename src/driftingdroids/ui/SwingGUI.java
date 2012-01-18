/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011  Michael Henke

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
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CancellationException;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import net.java.dev.designgridlayout.DesignGridLayout;
import driftingdroids.model.Board;
import driftingdroids.model.Move;
import driftingdroids.model.Solution;
import driftingdroids.model.SolverBFS;



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
        Color.WHITE
    };
    
    private static final String AC_BOARD_QUADRANTS= "quadrants";
    private static final String AC_PLACE_ROBOT    = "placerobot";
    private static final String AC_PLACE_GOAL     = "placegoal";
    private static final String AC_GAME_ID        = "gameid";
    private static final String AC_SELECT_SOLUTION= "selectsolution";
    private static final String AC_SHOW_COLOR_NAMES = "showcolornames";
    
    private Board board = null;
    private final BoardCell[] boardCells;
    private int[] currentPosition;
    
    private SolverTask solverTask = null;   //only set while SolverTask is working
    private List<Solution> computedSolutionList = null;       //result of SolverTask -> solver.execute()
    private int computedSolutionIndex = 0;
    private final List<Move> moves;
    private int hintCounter = 0;
    private int placeRobot = -1;    //default: false
    private boolean placeGoal = false;
    
    private final JComboBox[] jcomboQuadrants = { new JComboBox(), new JComboBox(), new JComboBox(), new JComboBox() };
    private final JComboBox jcomboRobots = new JComboBox();
    private final JComboBox jcomboOptSolutionMode = new JComboBox();
    private final JCheckBox jcheckOptAllowRebounds = new JCheckBox();
    private final JCheckBox jcheckOptShowColorNames = new JCheckBox();
    private final JTabbedPane jtabPreparePlay = new JTabbedPane();
    private final JComboBox jcomboPlaceRobot = new JComboBox();
    private final JButton jbutPlaceGoal = new JButton("Place goal");
    private final JComboBox jcomboGameIDs = new JComboBox();
    private final JComboBox jcomboSelectSolution = new JComboBox();
    private final JButton jbutSolutionHint = new JButton("Hint");
    private final JButton jbutNextMove = new JButton(">");
    private final JButton jbutAllMoves = new JButton(">>|");
    private final JButton jbutPrevMove = new JButton("<");
    private final JButton jbutNoMoves = new JButton("|<<");
    private final JTextPane jtextSolution = new JTextPane();
    
    public SwingGUI(final String windowTitle) throws InterruptedException, InvocationTargetException {
        this.board = Board.createBoardGameID("0765+41+2E21BD0F+1C");   //1A 4B 3B 2B
        this.moves = new ArrayList<Move>();
        
        this.boardCells = new BoardCell[this.board.size];
        for (int i = 0;  i < this.board.size;  ++i) {
            boardCells[i] = new BoardCell(i);
        }
        
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createAndShowGUI(windowTitle);
            }
        });
    }
    
    private void makeBoardQuadrants() {
        this.board = Board.createBoardQuadrants(
                this.jcomboQuadrants[0].getSelectedIndex(),
                this.jcomboQuadrants[1].getSelectedIndex(),
                this.jcomboQuadrants[2].getSelectedIndex(),
                this.jcomboQuadrants[3].getSelectedIndex(),
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
        if (null != this.solverTask) {
            this.solverTask.cancel(true);
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
    
    private void appendSolutionText(String str, Color bgCol) {
        StyledDocument doc = this.jtextSolution.getStyledDocument();
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
    
    private void setSolution(final SolverBFS solver) {
        this.computedSolutionList = solver.get();
        this.computedSolutionIndex = 0;
        for (int i = 0;  i < this.computedSolutionList.size();  ++i) {
            this.jcomboSelectSolution.addItem((i+1) + ")  " + this.computedSolutionList.get(i).toString());
        }
        this.hintCounter = 0;
        this.jtextSolution.setText(null);
        this.appendSolutionText("options:\n" + solver.getOptionsAsString2() + "\n", null);
        if (this.computedSolutionList.get(this.computedSolutionIndex).size() > 0) {
            final long seconds = (solver.getSolutionMilliSeconds() + 999) / 1000;
            final int solutions = this.computedSolutionList.size();
            this.appendSolutionText("found " + solutions + " solution" + (1==solutions ? "" : "s") +
                    " in " + seconds + " second" + (1==seconds ? "" : "s") + ".\n\n", null);
        } else {
            this.appendSolutionText("no solution found!\n", null);
        }
        System.out.println(this.computedSolutionList.get(this.computedSolutionIndex).toMovelistString() + "  (" + solver.toString() + ")");
        this.refreshButtons();
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
            this.appendSolutionText("\nselected solution " + solutionString + "\n", null);
            this.computedSolutionList.get(this.computedSolutionIndex).resetMoves();
            this.hintCounter = 3;
            //show moves
            for (int i = 0;  i < oldMovesSize;  ++i) {
                this.showNextMove(true);
            }
        }
    }
    
    private void showHint() {
        final int moves = this.computedSolutionList.get(this.computedSolutionIndex).size();
        final String movesStr = Integer.toString(moves) + " move" + (1==moves ? "" : "s");
        final Set<Integer> robotsMoved = this.computedSolutionList.get(this.computedSolutionIndex).getRobotsMoved();
        final String robotsMovedStr = Integer.toString(robotsMoved.size()) + " robot" + (1==robotsMoved.size() ? "" : "s");
        if (0 == this.hintCounter) {
            //first hint: number of moves
            this.appendSolutionText("hint: solution contains " + movesStr + ".\n", null);
        } else if (1 == this.hintCounter) {
            //second hint: number of moves + number of robots moved
            this.appendSolutionText("hint: " + movesStr + ", " + robotsMovedStr + " moved.\n", null);
        } else {
            //last hint: number of moves + number of robots moved + which robots moved
            this.appendSolutionText("last hint: " + movesStr + ", " + robotsMovedStr + ": ", null);
            for (Integer robot : robotsMoved) {
                this.appendSolutionText(Board.ROBOT_COLOR_NAMES_SHORT[robot.intValue()], COL_ROBOT[robot.intValue()]);
            }
            this.appendSolutionText(".\n", null);
        }
        ++this.hintCounter;
    }
    
    private void runSolverTask() {
        this.refreshBoard();
        this.removeSolution();
        this.solverTask = new SolverTask();
        this.solverTask.execute();
    }
    
    private class SolverTask extends SwingWorker<SolverBFS, Object> {
        @Override
        protected SolverBFS doInBackground() throws Exception {
            final SolverBFS solver = new SolverBFS(board);
            solver.setOptionSolutionMode((SolverBFS.SOLUTION_MODE)jcomboOptSolutionMode.getSelectedItem());
            solver.setOptionAllowRebounds(jcheckOptAllowRebounds.isSelected());
            jtextSolution.setText(null);
            appendSolutionText("options:\n" + solver.getOptionsAsString2() + "\n", null);
            appendSolutionText("computing solutions...\n\n", null);
            solver.execute();
            return solver;
        }
        @Override
        protected void done() {
            String errorMsg = "";
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
            if ( ! errorMsg.isEmpty()) {
                appendSolutionText("error:\n" + errorMsg + "\n", null);
            }
        }
    }
    
    private class TabListener implements ChangeListener {
        private Board oldBoard = null;
        @Override
        public void stateChanged(ChangeEvent e) {
            refreshBoard(); //repaint the entire board
            if (isModePlay()) {
                if (this.oldBoard != board) {
                    updateBoardRandomGoal();    //set random goal and start solver thread
                } else {
                    updateBoardGetRobots();     //start solver thread
                }
            } else {
                removeSolution();   //stop solver thread
                this.oldBoard = board;
            }
        }
    }
    
    
    private void addKeyBindingTooltip(AbstractButton button, int keyCode, String tooltip, Action action) {
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
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
    }
    
    
    @SuppressWarnings("serial")
    private void createAndShowGUI(String windowTitle) {
        final JFrame frame = new JFrame(windowTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final JButton jbutRandomLayout = new JButton("Random layout");
        this.addKeyBindingTooltip(jbutRandomLayout, KeyEvent.VK_L,
                "place the board tiles at random positions",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        makeRandomBoardQuadrants();
                        refreshJComboPlaceRobot();
                        refreshJcomboQuadrants();
                    }
                }
        );
        
        for (int i = 0;  i < 4;  ++i) {
            this.jcomboQuadrants[i].setModel(new DefaultComboBoxModel(Board.QUADRANT_NAMES));
            this.jcomboQuadrants[i].setEditable(false);
            this.jcomboQuadrants[i].setSelectedIndex(this.board.getQuadrantNum(i));
            this.jcomboQuadrants[i].setActionCommand(AC_BOARD_QUADRANTS);
            this.jcomboQuadrants[i].addActionListener(this);
        }
        
        final String[] strRobots = { "1", "2", "3", "4", "5" };
        this.jcomboRobots.setModel(new DefaultComboBoxModel(strRobots));
        this.jcomboRobots.setEditable(false);
        this.jcomboRobots.setActionCommand(AC_BOARD_QUADRANTS);
        this.jcomboRobots.addActionListener(this);
        this.refreshJcomboRobots();
        
        final SolverBFS.SOLUTION_MODE[] solModes = SolverBFS.SOLUTION_MODE.values();
        this.jcomboOptSolutionMode.setModel(new DefaultComboBoxModel(solModes));
        this.jcomboOptSolutionMode.setSelectedItem(SolverBFS.SOLUTION_MODE.MINIMUM);
        
        this.jcheckOptAllowRebounds.setText("allow rebound moves");
        this.jcheckOptAllowRebounds.setSelected(true);

        this.jcheckOptShowColorNames.setText("show color names (robots and goals)");
        this.jcheckOptShowColorNames.setSelected(true);
        this.jcheckOptShowColorNames.setActionCommand(AC_SHOW_COLOR_NAMES);
        this.jcheckOptShowColorNames.addActionListener(this);

        final JPanel preparePanel = new JPanel();
        final DesignGridLayout prepareLayout = new DesignGridLayout(preparePanel);
        prepareLayout.row().grid().add(new JLabel("board tiles"));
        prepareLayout.row().grid().addMulti(this.jcomboQuadrants[0], this.jcomboQuadrants[1]).add(jbutRandomLayout);
        prepareLayout.row().grid().addMulti(this.jcomboQuadrants[3], this.jcomboQuadrants[2]).empty();
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JLabel("number of robots")).addMulti(this.jcomboRobots);
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.row().grid().add(new JLabel("solver options"));
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JLabel("prefer solution with")).addMulti(this.jcomboOptSolutionMode);
        prepareLayout.row().grid().add(new JLabel("number of robots moved"));
        prepareLayout.row().grid().add(new JLabel(" "));
        prepareLayout.row().grid().add(this.jcheckOptAllowRebounds);
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.row().grid().add(new JLabel("GUI options"));
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(this.jcheckOptShowColorNames);
        
        final JButton jbutRandomRobots = new JButton("Random robots");
        this.addKeyBindingTooltip(jbutRandomRobots, KeyEvent.VK_R,
                "place the robots randomly on the board",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        placeRobot = -1;
                        refreshJComboPlaceRobot();
                        updateBoardRandomRobots();
                    }
                }
        );
        
        final JButton jbutRandomGoal = new JButton("Random goal");
        this.addKeyBindingTooltip(jbutRandomGoal, KeyEvent.VK_G,
                "pick a goal at random",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        placeGoal = false;
                        updateBoardRandomGoal();
                    }
                }
        );
        
        this.refreshJComboPlaceRobot();   //this.jcomboPlaceRobot
        this.jcomboPlaceRobot.setEditable(false);
        this.jcomboPlaceRobot.setActionCommand(AC_PLACE_ROBOT);
        this.jcomboPlaceRobot.addActionListener(this);
        this.jcomboPlaceRobot.setToolTipText("first select a robot here and then click on its new board position");
        
        this.jbutPlaceGoal.setActionCommand(AC_PLACE_GOAL);
        this.jbutPlaceGoal.addActionListener(this);
        this.jbutPlaceGoal.setToolTipText("first click here and then select a goal on the board");
        
        this.jcomboGameIDs.setModel(new DefaultComboBoxModel());
        this.jcomboGameIDs.setEditable(true);
        this.jcomboGameIDs.setActionCommand(AC_GAME_ID);
        this.jcomboGameIDs.addActionListener(this);
        
        this.jcomboSelectSolution.setModel(new DefaultComboBoxModel());
        this.jcomboSelectSolution.setPrototypeDisplayValue("99)  99/9/#####");  //longest string possible here
        this.jcomboSelectSolution.addItem("Select solution");
        this.jcomboSelectSolution.setEditable(false);
        this.jcomboSelectSolution.setActionCommand(AC_SELECT_SOLUTION);
        this.jcomboSelectSolution.addActionListener(this);
        this.jcomboSelectSolution.setToolTipText("SPOILER WARNING. clicking this reveals hints about the solutions");
        
        this.addKeyBindingTooltip(this.jbutSolutionHint, KeyEvent.VK_H,
                "click 3 times to get more detailed hints",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        showHint();
                    }
                }
        );
        
        this.addKeyBindingTooltip(this.jbutNextMove, KeyEvent.VK_N,
                "show next move",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        showNextMove(true);
                    }
                }
        );
        
        this.addKeyBindingTooltip(this.jbutAllMoves, KeyEvent.VK_M,
                "show all moves",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        while (jbutNextMove.isEnabled()) {
                            showNextMove(true);
                        }
                    }
                }
        );
        
        this.addKeyBindingTooltip(this.jbutPrevMove, KeyEvent.VK_B,
                "undo last move",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        showPrevMove(true);
                    }
                }
        );
        
        this.addKeyBindingTooltip(this.jbutNoMoves, KeyEvent.VK_V,
                "undo all moves",
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        while (jbutPrevMove.isEnabled()) {
                            showPrevMove(true);
                        }
                    }
                }
        );
        
        this.jtextSolution.setEditable(false);
        final JPanel panelSolutionText = new JPanel(new BorderLayout());
        panelSolutionText.add(this.jtextSolution, BorderLayout.CENTER);
        final JScrollPane scrollSolutionText = new JScrollPane(panelSolutionText, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSolutionText.setPreferredSize(new Dimension(10, 10)); //workaround for layout problem ?!?!
        scrollSolutionText.setMinimumSize(new Dimension(10, 10));   //workaround for layout problem ?!?!
        
        final JPanel playPanel = new JPanel();
        final DesignGridLayout playLayout = new DesignGridLayout(playPanel);
        playLayout.row().grid().add(new JLabel("set starting position"));
        playLayout.row().grid().add(jbutRandomRobots).add(jbutRandomGoal);
        playLayout.row().grid().add(this.jcomboPlaceRobot).add(this.jbutPlaceGoal);
        playLayout.row().grid().add(new JLabel("game ID")).add(this.jcomboGameIDs, 3);
        playLayout.emptyRow();
        playLayout.row().grid().add(new JSeparator());
        playLayout.row().grid().add(new JLabel("show computed solutions"));
        playLayout.row().grid().add(this.jcomboSelectSolution).add(this.jbutSolutionHint);
        playLayout.row().grid().add(this.jbutNoMoves).add(this.jbutPrevMove).add(this.jbutNextMove).add(this.jbutAllMoves);
        playLayout.row().grid().add(scrollSolutionText);
        
        this.jtabPreparePlay.addTab("Prepare board / Options", preparePanel);
        this.jtabPreparePlay.addTab("Play game", playPanel);
        this.jtabPreparePlay.setSelectedIndex(1);   //Play
        this.jtabPreparePlay.addChangeListener(new TabListener());
        
        final JPanel boardPanel = new JPanel(new GridLayout(this.board.height, this.board.width));
        for (int i = 0; i < this.board.size; ++i) {
            boardPanel.add(this.boardCells[i]);
        }
        
        frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
        frame.getContentPane().add(this.jtabPreparePlay, BorderLayout.EAST);
        frame.pack();
        frame.setVisible(true);
        
        try {
            final String defLaf = UIManager.getLookAndFeel().getClass().getName();
            final String sysLaf = UIManager.getSystemLookAndFeelClassName();
            System.out.println("default L&F: " + defLaf);
            System.out.println("system  L&F: " + sysLaf);
            if ((false == defLaf.equals(sysLaf)) && (false == defLaf.toLowerCase().contains("nimbus"))) {
                System.out.println("activating system L&F now.");
                UIManager.setLookAndFeel(sysLaf);
                SwingUtilities.updateComponentTreeUI(frame);
                frame.pack();
                System.out.println("successfully activated system L&F.");
            }
        } catch (Exception e) {
            System.out.println("ERROR: could not activate system L&F: " + e.toString());
        }
        
        this.updateBoardGetRobots();
    }
    
    private void refreshJComboPlaceRobot() {
        final Vector<String> vec = new Vector<String>();
        vec.add("Place robot");
        final int numRobots = this.board.getRobotPositions().length;
        for (int i = 0;  i < numRobots;  ++i) {
            vec.add(Board.ROBOT_COLOR_NAMES_LONG[i]);
        }
        this.jcomboPlaceRobot.setModel(new DefaultComboBoxModel(vec));
        this.jcomboPlaceRobot.setSelectedIndex(0);
    }
    
    private boolean isModePlay() {
        return (this.jtabPreparePlay.getSelectedIndex() == 1);
    }
    
    private void refreshJcomboRobots() {
        final String tmp = this.jcomboRobots.getActionCommand();
        this.jcomboRobots.setActionCommand("");
        this.jcomboRobots.setSelectedIndex(this.board.getRobotPositions().length - 1);
        this.jcomboRobots.setActionCommand(tmp);
    }

    private void refreshJcomboQuadrants() {
        for (int i = 0;  i < 4;  ++i) {
            final String tmp = this.jcomboQuadrants[i].getActionCommand();
            this.jcomboQuadrants[i].setActionCommand("");
            this.jcomboQuadrants[i].setSelectedIndex(this.board.getQuadrantNum(i));
            this.jcomboQuadrants[i].setActionCommand(tmp);
        }
    }

    
    private void refreshBoard() {
        //repaint board
        for (JComponent comp : this.boardCells) {
            comp.repaint();
        }
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
                this.jcomboSelectSolution.setEnabled(true);
                this.jbutSolutionHint.setEnabled(true);
            }
            this.jbutNextMove.setEnabled(true);
            this.jbutPrevMove.setEnabled(true);
            this.jbutAllMoves.setEnabled(true);
            this.jbutNoMoves.setEnabled(true);
            if (this.board.getGoal().robotNumber < 0) {
                for (int pos : this.currentPosition) {
                    if ((pos == this.board.getGoal().position) || (this.computedSolutionList.get(this.computedSolutionIndex).size() < 1)) {
                        this.jbutNextMove.setEnabled(false);
                        this.jbutAllMoves.setEnabled(false);
                        break;
                    }
                }
            } else if ((this.currentPosition[this.board.getGoal().robotNumber] == this.board.getGoal().position)
                    || (this.computedSolutionList.get(this.computedSolutionIndex).size() < 1)){
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
            this.appendSolutionText(step.strRobotDirection(), COL_ROBOT[step.robotNumber]);
            this.appendSolutionText(" " + step.strOldNewPosition() + (this.computedSolutionList.get(this.computedSolutionIndex).isRebound(step) ? " rebound" : "") + "\n", null);
            //System.out.println(step.toString());
        }
        this.refreshButtons();
        this.refreshBoard(step);
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
                this.refreshJcomboQuadrants();
                this.updateBoardGetRobots();
            } else {
                appendSolutionText("error: this game ID '" + newGameID + "' is not valid.\n", null);
            }
        }
    }
    
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (AC_BOARD_QUADRANTS.equals(e.getActionCommand())) {
            this.makeBoardQuadrants();
            this.refreshJComboPlaceRobot();
        } else if (AC_SELECT_SOLUTION.equals(e.getActionCommand())) {
            this.selectSolution(this.jcomboSelectSolution.getSelectedIndex(), this.jcomboSelectSolution.getSelectedItem().toString());
        } else if (AC_PLACE_ROBOT.equals(e.getActionCommand())) {
            this.placeRobot = this.jcomboPlaceRobot.getSelectedIndex() - 1;     //item #0 is "Place robot"
        } else if (AC_PLACE_GOAL.equals(e.getActionCommand())) {
            this.placeGoal = !this.placeGoal;
            this.refreshBoard();
        } else if (AC_GAME_ID.equals(e.getActionCommand())) {
            this.handleGameID();
        } else if (AC_SHOW_COLOR_NAMES.equals(e.getActionCommand())) {
            this.refreshBoard();
        }
    }
    
    private class BoardCell extends JPanel implements MouseListener {
        private static final long serialVersionUID = 1L;
        
        private static final int PREF_WIDTH = 33;       // preferred width
        private static final int PREF_HEIGHT = 33;      // preferred height
        private static final int H_WALL_DIVISOR = 12;   // horizontal walls: height / H_WALL_DIVISOR
        private static final int V_WALL_DIVISOR = 12;   // vertical walls: width / vWallDivisor
        
        
        private final int boardPosition;
        
        public BoardCell(int boardPosition) {
            super();
            this.boardPosition = boardPosition;
            this.setPreferredSize(new Dimension(PREF_WIDTH, PREF_HEIGHT));
            this.setMinimumSize(new Dimension(10, 10));
            this.addMouseListener(this);
        }
        
        @Override
        protected void paintComponent(Graphics graphics) {
            final Graphics2D g2d = (Graphics2D) graphics.create();
            final int height = this.getSize().height;
            final int width  = this.getSize().width;
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
            final byte[] walls = board.getWalls(this.boardPosition);
            g2d.setColor(COL_WALL);
            if (walls[Board.NORTH] != 0) {
                g2d.fillRect(0, 0, width, hWallWidth);
            }
            if (walls[Board.EAST] != 0) {
                g2d.fillRect(width - vWallWidth, 0, width, height);
            }
            if (walls[Board.SOUTH] != 0) { 
                g2d.fillRect(0, height - hWallWidth, width, height);
            }
            if (walls[Board.WEST] != 0) {
                g2d.fillRect(0, 0, vWallWidth, height);
            }
            
            // paint the goal
            if (!isModePlay() || placeGoal || (board.getGoal().position == this.boardPosition)) {
                final Board.Goal goal;
                if (isModePlay() && !placeGoal) {
                    goal = board.getGoal();
                } else {
                    goal = board.getGoalAt(this.boardPosition);
                }
                if (null != goal) {
                    final Paint thePaint;
                    if (goal.robotNumber < 0) {
                        final float fStep = 1.0f / (COL_ROBOT.length-1);
                        final float[] fractions = new float[COL_ROBOT.length];
                        for (int i = 1; i < fractions.length; ++i) {
                            fractions[i] = fractions[i - 1] + fStep;
                        }
                        thePaint = new LinearGradientPaint(vWallWidth*2, hWallWidth*2, vWallWidth*2, height-1-hWallWidth*2, fractions, COL_ROBOT, MultipleGradientPaint.CycleMethod.REPEAT);
                    } else {
                        thePaint = new GradientPaint(0, 0, COL_ROBOT[goal.robotNumber], 0, height-1, Color.DARK_GRAY);
                    }
                    g2d.setPaint(thePaint);
                    final Shape outerShape = new Rectangle2D.Double(
                            vWallWidth, hWallWidth,
                            width-vWallWidth-vWallWidth, height-hWallWidth-hWallWidth);
                    final Shape innerShape;
                    switch (goal.shape) {
                    case Board.GOAL_SQUARE:
                        innerShape = new Rectangle2D.Double(
                                Math.round(width * (1.0d/4.0d)), Math.round(height * (1.0d/4.0d)),
                                Math.round(width * (2.0d/4.0d) - 1), Math.round(height * (2.0d/4.0d) - 1) );
                        break;
                    case Board.GOAL_TRIANGLE:
                        final Polygon triangle = new Polygon();
                        triangle.addPoint(width     / 5, height * 4 / 5);
                        triangle.addPoint(width * 4 / 5, height * 4 / 5);
                        triangle.addPoint(width    >> 1, height     / 5);
                        innerShape = triangle;
                        break;
                    case Board.GOAL_HEXAGON:
                        final Polygon hexagon = new Polygon();
                        hexagon.addPoint(width     / 6, height >> 1);
                        hexagon.addPoint(width * 2 / 6, height     / 5);
                        hexagon.addPoint(width * 4 / 6, height     / 5);
                        hexagon.addPoint(width * 5 / 6, height >> 1);
                        hexagon.addPoint(width * 4 / 6, height * 4 / 5);
                        hexagon.addPoint(width * 2 / 6, height * 4 / 5);
                        innerShape = hexagon;
                        break;
                    default:    //case Board.GOAL_CIRCLE:
                        innerShape = new Ellipse2D.Double(
                                width * (1.0d/5.0d), height * (1.0d/5.0d),
                                width * (3.0d/5.0d), height * (3.0d/5.0d) );
                        break;
                    }
                    final Area area = new Area(outerShape);
                    area.subtract(new Area(innerShape));
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.fill(area);
                    if (jcheckOptShowColorNames.isSelected()) {
                        final String goalColorShort = ((goal.robotNumber < 0) ? "*" : Board.ROBOT_COLOR_NAMES_SHORT[goal.robotNumber]);
                        g2d.setColor(Color.BLACK);
                        g2d.drawChars(goalColorShort.toCharArray(), 0, 1, width / 2 - 3, height / 2 + 3);
                        final String goalColorLong = ((goal.robotNumber < 0) ? "wildcard" : Board.ROBOT_COLOR_NAMES_LONG[goal.robotNumber]);
                        this.setToolTipText(goalColorLong + " " + Board.GOAL_SHAPE_NAMES[goal.shape] + " goal");
                    }
                }
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
                        final Paint fillPaint = new GradientPaint(0, 0, COL_ROBOT[i], 0, height-1, Color.DARK_GRAY);
                        final Color outlineColor = Color.BLACK;
                        Polygon shapeFoot = new Polygon();
                        shapeFoot.addPoint(width / 2 - 1, height * 3 / 4 - 1);
                        shapeFoot.addPoint(vWallWidth, height - 1 - hWallWidth);
                        shapeFoot.addPoint(width - 1 - vWallWidth, height - 1 - hWallWidth);
                        final Ellipse2D.Double shapeBody = new Ellipse2D.Double(
                                vWallWidth * 3, hWallWidth,
                                width - 1 - vWallWidth * 6,
                                height - 1 - hWallWidth * 2
                                );
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setPaint(fillPaint);    g2d.fill(shapeFoot);
                        g2d.setColor(outlineColor); g2d.draw(shapeFoot);
                        g2d.setPaint(fillPaint);    g2d.fill(shapeBody);
                        g2d.setColor(outlineColor); g2d.draw(shapeBody);
                        if (jcheckOptShowColorNames.isSelected()) {
                            g2d.setColor(Color.WHITE);
                            g2d.drawChars(Board.ROBOT_COLOR_NAMES_SHORT[i].toCharArray(), 0, 1, width / 2 - 3, height / 2 + 3);
                            this.setToolTipText(Board.ROBOT_COLOR_NAMES_LONG[i] + " robot");
                        }
                        break;
                    }
                }
            }
        }
        
        //implements MouseListener
        @Override
        public void mouseClicked(MouseEvent e) {
            if (placeRobot >= 0) {
                board.setRobots(currentPosition);
                if (board.setRobot(placeRobot, this.boardPosition, true)) {
                    updateBoardGetRobots();
                }
                placeRobot = -1;
                refreshJComboPlaceRobot();
            }
            if (placeGoal) {
                if (board.setGoal(this.boardPosition)) {
                    placeGoal = false;
                    board.setRobots(currentPosition);
                    updateBoardGetRobots();
                }
            }
        }
        @Override
        public void mouseEntered(MouseEvent e) { /* NO-OP */ }
        @Override
        public void mouseExited(MouseEvent e) { /* NO-OP */ }
        @Override
        public void mousePressed(MouseEvent e) { /* NO-OP */ }
        @Override
        public void mouseReleased(MouseEvent e) { /* NO-OP */ }
    }

}
