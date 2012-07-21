/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011, 2012  Michael Henke

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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
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
import driftingdroids.model.Solver;
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
    private static final String AC_GAME_ID        = "gameid";
    private static final String AC_SELECT_SOLUTION= "selectsolution";
    private static final String AC_SHOW_COLOR_NAMES = "showcolornames";
    
    private static final ResourceBundle L10N = ResourceBundle.getBundle("driftingdroids-localization-ui");  //L10N = Localization
    
    private Board board = null;
    private final BoardCell[] boardCells;
    private int[] currentPosition;
    
    private volatile SolverTask solverTask = null;                  //only set while SolverTask is working
    private volatile List<Solution> computedSolutionList = null;    //result of SolverTask -> solver.execute()
    private int computedSolutionIndex = 0;
    private final List<Move> moves;
    private int hintCounter = 0;
    private int placeRobot = -1;    //default: false
    private boolean selectGoal = false;
    
    private final JComboBox[] jcomboQuadrants = { new JComboBox(), new JComboBox(), new JComboBox(), new JComboBox() };
    private final JComboBox jcomboRobots = new JComboBox();
    private final JComboBox jcomboOptSolutionMode = new JComboBox();
    private final JCheckBox jcheckOptAllowRebounds = new JCheckBox();
    private final JCheckBox jcheckOptShowColorNames = new JCheckBox();
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
            this.appendSolutionText(lastMove.strRobotDirectionL10N(), COL_ROBOT[lastMove.robotNumber]);
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
    
    private class SolverTask extends SwingWorker<Solver, Object> {
        @Override
        protected Solver doInBackground() throws Exception {
            final Solver solver = new SolverBFS(board);
            solver.setOptionSolutionMode((Solver.SOLUTION_MODE)jcomboOptSolutionMode.getSelectedItem());
            solver.setOptionAllowRebounds(jcheckOptAllowRebounds.isSelected());
            jtextSolution.setText(null);
            appendSolutionText(getSolverOptionsString(solver), null);
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
        final JFrame frame = new JFrame(windowTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final JButton jbutRandomLayout = new JButton(L10N.getString("btn.RandomLayout.text"));
        this.addKeyBindingTooltip(jbutRandomLayout,
                L10N.getString("btn.RandomLayout.acceleratorkey"),
                L10N.getString("btn.RandomLayout.tooltip"),
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
        
        final Solver.SOLUTION_MODE[] solModes = Solver.SOLUTION_MODE.values();
        this.jcomboOptSolutionMode.setModel(new DefaultComboBoxModel(solModes));
        this.jcomboOptSolutionMode.setSelectedItem(Solver.SOLUTION_MODE.MINIMUM);
        
        this.jcheckOptAllowRebounds.setText(L10N.getString("chk.AllowReboundMoves.text"));
        this.jcheckOptAllowRebounds.setSelected(true);

        this.jcheckOptShowColorNames.setText(L10N.getString("chk.ShowColorNames.text"));
        this.jcheckOptShowColorNames.setSelected(true);
        this.jcheckOptShowColorNames.setActionCommand(AC_SHOW_COLOR_NAMES);
        this.jcheckOptShowColorNames.addActionListener(this);

        this.jcheckOptShowSolutions.setText(L10N.getString("chk.ShowSolutions.text"));
        this.jcheckOptShowSolutions.setSelected(false);

        final JPanel preparePanel = new JPanel();
        final DesignGridLayout prepareLayout = new DesignGridLayout(preparePanel);
        prepareLayout.row().grid().add(new JLabel(L10N.getString("lbl.BoardTiles.text")));
        prepareLayout.row().grid().addMulti(this.jcomboQuadrants[0], this.jcomboQuadrants[1]).add(jbutRandomLayout);
        prepareLayout.row().grid().addMulti(this.jcomboQuadrants[3], this.jcomboQuadrants[2]).empty();
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JLabel(L10N.getString("lbl.NumberOfRobots.text"))).addMulti(this.jcomboRobots);
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.row().grid().add(new JLabel(L10N.getString("lbl.SolverOptions.text")));
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JLabel(L10N.getString("lbl.PreferSolutionWith.text"))).addMulti(this.jcomboOptSolutionMode);
        prepareLayout.row().grid().add(new JLabel(L10N.getString("lbl.NumberOfRobotsMoved.text")));
        prepareLayout.row().grid().add(new JLabel(" "));
        prepareLayout.row().grid().add(this.jcheckOptAllowRebounds);
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.row().grid().add(new JLabel(L10N.getString("lbl.GUIOptions.text")));
        prepareLayout.emptyRow();
        prepareLayout.row().grid().add(this.jcheckOptShowColorNames);
        prepareLayout.row().grid().add(this.jcheckOptShowSolutions);
        
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
        this.jbutNextMove.setText(L10N.getString("btn.NextMove.text"));
        this.addKeyBindingTooltip(this.jbutNextMove,
                L10N.getString("btn.NextMove.acceleratorkey"),
                L10N.getString("btn.NextMove.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        showNextMove(true);
                    }
                }
        );
        this.jbutAllMoves.setText(L10N.getString("btn.AllMoves.text"));
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
        this.jbutPrevMove.setText(L10N.getString("btn.PrevMove.text"));
        this.addKeyBindingTooltip(this.jbutPrevMove,
                L10N.getString("btn.PrevMove.acceleratorkey"),
                L10N.getString("btn.PrevMove.tooltip"),
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        showPrevMove(true);
                    }
                }
        );
        this.jbutNoMoves.setText(L10N.getString("btn.NoMoves.text"));
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
        final JPanel panelSolutionText = new JPanel(new BorderLayout());
        panelSolutionText.add(this.jtextSolution, BorderLayout.CENTER);
        final JScrollPane scrollSolutionText = new JScrollPane(panelSolutionText, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSolutionText.setPreferredSize(new Dimension(10, 10)); //workaround for layout problem ?!?!
        scrollSolutionText.setMinimumSize(new Dimension(10, 10));   //workaround for layout problem ?!?!
        
        final JPanel playPanel = new JPanel();
        final DesignGridLayout playLayout = new DesignGridLayout(playPanel);
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
        
        this.jtabPreparePlay.addTab(L10N.getString("tab.PrepareBoardOptions.text"), preparePanel);
        this.jtabPreparePlay.addTab(L10N.getString("tab.PlayGame.text"), playPanel);
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
        this.jcomboPlaceRobot.removeAllItems();
        this.jcomboPlaceRobot.addItem(L10N.getString("cmb.PlaceRobot.text"));
        final int numRobots = this.board.getRobotPositions().length;
        for (int i = 0;  i < numRobots;  ++i) {
            this.jcomboPlaceRobot.addItem(Board.getColorLongL10N(i));
        }
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
                this.jcomboSelectSolution.setEnabled((this.computedSolutionList.size() > 1));
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
            this.appendSolutionText(step.strRobotDirectionL10N(), COL_ROBOT[step.robotNumber]);
            this.appendSolutionText(" " + step.strOldNewPosition()
                    + (this.computedSolutionList.get(this.computedSolutionIndex).isRebound(step) ? " " + L10N.getString("txt.Rebound.text") : "")
                    + "\n", null);
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
                appendSolutionText(MessageFormat.format(L10N.getString("msg.ErrorGameID.pattern"), newGameID) + "\n", null);
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
            if (!isModePlay() || selectGoal || (board.getGoal().position == this.boardPosition)) {
                final Board.Goal goal;
                if (isModePlay() && !selectGoal) {
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
                        final String goalColorShort = Board.getColorShortL10N(goal.robotNumber);
                        g2d.setColor(Color.BLACK);
                        g2d.drawChars(goalColorShort.toCharArray(), 0, 1, width / 2 - 3, height / 2 + 3);
                        final String goalColorLong = Board.getColorLongL10N(goal.robotNumber);
                        this.setToolTipText(L10N.getString("txt.Goal.text") + " - " + goalColorLong + " - " + Board.getGoalShapeL10N(goal.shape));
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
                            g2d.drawChars(Board.getColorShortL10N(i).toCharArray(), 0, 1, width / 2 - 3, height / 2 + 3);
                            this.setToolTipText(L10N.getString("txt.Robot.text") + " - " + Board.getColorLongL10N(i));
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
            if (true == selectGoal) {
                if (board.setGoal(this.boardPosition)) {
                    selectGoal = false;
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
