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
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CancellationException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import driftingdroids.model.AbstractSolver;
import driftingdroids.model.Board;
import driftingdroids.model.Move;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.RowGroup;



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
    private static final String AC_RANDOM_ROBOTS  = "randrob";
    private static final String AC_RANDOM_GOAL    = "randgoal";
    private static final String AC_PLACE_ROBOT    = "placerobot";
    private static final String AC_PLACE_GOAL     = "placegoal";
    private static final String AC_SHOW_HINT      = "hint";
    private static final String AC_SHOW_NEXT_MOVE = "nextmove";
    private static final String AC_SHOW_PREV_MOVE = "prevmove";
    private static final String AC_SHOW_ALL_MOVES = "allmoves";
    private static final String AC_SHOW_NO_MOVES  = "nomoves";
    
    private Board board = null;
    private final BoardCell[] boardCells;
    private int[] currentPosition;
    
    private SolverTask solverTask = null;   //only set while SolverTask is working
    private AbstractSolver solver = null;           //result of SolverTask
    private final List<Move> moves;
    private int hintCounter = 0;
    private int placeRobot = -1;    //default: false
    private boolean placeGoal = false;
    
    private final JComboBox[] jcomboQuadrants = { new JComboBox(), new JComboBox(), new JComboBox(), new JComboBox() };
    private final JComboBox jcomboRobots = new JComboBox();
    private final JComboBox jcomboOptSolutionMode = new JComboBox();
    private final JCheckBox jcheckOptAllowRebounds = new JCheckBox();
    private final JTabbedPane jtabPreparePlay = new JTabbedPane();
    private final JButton jbutRandomRobots = new JButton("Random robots");
    private final JButton jbutRandomGoal = new JButton("Random goal");
    private final JComboBox jcomboPlaceRobot = new JComboBox();
    private final JButton jbutPlaceGoal = new JButton("Place goal");
    private final JButton jbutSolutionHint = new JButton("Hint");
    private final JButton jbutNextMove = new JButton("+ Next move");
    private final JButton jbutAllMoves = new JButton("All moves");
    private final JButton jbutPrevMove = new JButton("- Prev. move");
    private final JButton jbutNoMoves = new JButton("Reset moves");
    private final JTextPane jtextSolution = new JTextPane();
    
    public SwingGUI(final String windowTitle) throws InterruptedException, InvocationTargetException {
        this.board = Board.createBoard(0, 7, 6, 5, 4);  //1A 4B 3B 2B
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
        this.board = Board.createBoard(
                this.jcomboQuadrants[0].getSelectedIndex(),
                this.jcomboQuadrants[1].getSelectedIndex(),
                this.jcomboQuadrants[2].getSelectedIndex(),
                this.jcomboQuadrants[3].getSelectedIndex(),
                this.jcomboRobots.getSelectedIndex() + 1 );
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
        this.solver = null;
        this.moves.clear();
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
    
    private void setSolution(final AbstractSolver solver) {
        this.solver = solver;
        this.hintCounter = 0;
        this.jtextSolution.setText(null);
        this.appendSolutionText("(options: " + this.solver.getOptionsAsString() + ")\n", null);
        if (this.solver.getSolutionSize() > 0) {
            final long seconds = (this.solver.getSolutionMilliSeconds() + 999) / 1000;
            this.appendSolutionText("found solution in " + seconds + " second" + (1==seconds ? "" : "s") + ".\n", null);
        } else {
            this.appendSolutionText("no solution found!\n", null);
        }
        //System.out.println();
        //System.out.println(this.board.toString());
        System.out.println(this.solver.toString());
        this.refreshButtons();
    }
    
    private void showHint() {
        final int moves = this.solver.getSolutionSize();
        final String movesStr = Integer.toString(moves) + " move" + (1==moves ? "" : "s");
        final Set<Integer> robotsMoved = this.solver.getSolutionRobotsMoved();
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
    
    private class SolverTask extends SwingWorker<AbstractSolver, Object> {
        @Override
        protected AbstractSolver doInBackground() throws Exception {
            final AbstractSolver solver = new driftingdroids.model.SolverBFS(board);
            //final AbstractSolver solver = new driftingdroids.model.SolverIDDFS(board);
            
            solver.setOptionSolutionMode((AbstractSolver.SOLUTION_MODE)jcomboOptSolutionMode.getSelectedItem());
            solver.setOptionAllowRebounds(jcheckOptAllowRebounds.isSelected());
            jtextSolution.setText(null);
            appendSolutionText("(options: " + solver.getOptionsAsString() + ")\n", null);
            appendSolutionText("computing solution...\n", null);
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
    
    private class ShowHideAction implements ItemListener     {
        final private RowGroup group;
        public ShowHideAction(RowGroup group) {
            this.group = group;
        }
        @Override
        public void itemStateChanged(ItemEvent event) {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                this.group.show();
            } else {
                this.group.hide();
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


    private void createAndShowGUI(String windowTitle) {
        final JFrame frame = new JFrame(windowTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        for (int i = 0;  i < 4;  ++i) {
            this.jcomboQuadrants[i].setModel(new DefaultComboBoxModel(Board.QUADRANT_NAMES));
            this.jcomboQuadrants[i].setEditable(false);
            this.jcomboQuadrants[i].setSelectedIndex(this.board.getQuadrantNum(i));
            this.jcomboQuadrants[i].setActionCommand(AC_BOARD_QUADRANTS);
            this.jcomboQuadrants[i].addActionListener(this);
            this.setJComboCenterAlignment(this.jcomboQuadrants[i]);
        }
        final String[] strRobots = { "1", "2", "3", "4", "5" };
        this.jcomboRobots.setModel(new DefaultComboBoxModel(strRobots));
        this.jcomboRobots.setEditable(false);
        this.jcomboRobots.setSelectedIndex(this.board.getRobotPositions().length - 1);
        this.jcomboRobots.setActionCommand(AC_BOARD_QUADRANTS);
        this.jcomboRobots.addActionListener(this);
        this.setJComboCenterAlignment(this.jcomboRobots);
        
        final AbstractSolver.SOLUTION_MODE[] solModes = AbstractSolver.SOLUTION_MODE.values();
        this.jcomboOptSolutionMode.setModel(new DefaultComboBoxModel(solModes));
        this.jcomboOptSolutionMode.setSelectedItem(AbstractSolver.SOLUTION_MODE.MINIMUM);
        this.setJComboCenterAlignment(this.jcomboOptSolutionMode);
        
        this.jcheckOptAllowRebounds.setText("allow rebound moves (reverse)");
        this.jcheckOptAllowRebounds.setSelected(true);
        
        final JPanel preparePanel = new JPanel();
        final DesignGridLayout prepareLayout = new DesignGridLayout(preparePanel);
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.row().grid().add(new JLabel("choose board pieces"));
        prepareLayout.row().grid().add(this.jcomboQuadrants[0]).add(this.jcomboQuadrants[1]);
        prepareLayout.row().grid().add(this.jcomboQuadrants[3]).add(this.jcomboQuadrants[2]);
        prepareLayout.row().grid().add(new JLabel(" "));
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.row().grid().add(new JLabel("number of robots")).add(this.jcomboRobots);
        prepareLayout.row().grid().add(new JLabel(" "));
        prepareLayout.row().grid().add(new JSeparator());
        prepareLayout.row().grid().add(new JLabel("solver algorithm options"));
        prepareLayout.row().grid().add(new JLabel(" "));
        prepareLayout.row().grid().add(new JLabel("choose solution with")).add(this.jcomboOptSolutionMode);
        prepareLayout.row().grid().add(new JLabel("number of robots moved."));
        prepareLayout.row().grid().add(new JLabel(" "));
        prepareLayout.row().grid().add(this.jcheckOptAllowRebounds);
        prepareLayout.row().grid().add(new JLabel(" "));
        prepareLayout.row().grid().add(new JSeparator());
        
        this.jbutRandomRobots.setMnemonic(KeyEvent.VK_R);
        this.jbutRandomRobots.setActionCommand(AC_RANDOM_ROBOTS);
        this.jbutRandomRobots.addActionListener(this);
        this.jbutRandomGoal.setMnemonic(KeyEvent.VK_G);
        this.jbutRandomGoal.setActionCommand(AC_RANDOM_GOAL);
        this.jbutRandomGoal.addActionListener(this);
        this.refreshJComboPlaceRobot();   //this.jcomboPlaceRobot
        this.jcomboPlaceRobot.setEditable(false);
        this.jcomboPlaceRobot.setActionCommand(AC_PLACE_ROBOT);
        this.jcomboPlaceRobot.addActionListener(this);
        this.setJComboCenterAlignment(this.jcomboPlaceRobot);
        this.jbutPlaceGoal.setActionCommand(AC_PLACE_GOAL);
        this.jbutPlaceGoal.addActionListener(this);
        this.jbutSolutionHint.setMnemonic(KeyEvent.VK_H);
        this.jbutSolutionHint.setActionCommand(AC_SHOW_HINT);
        this.jbutSolutionHint.addActionListener(this);
        this.jbutNextMove.setMnemonic(KeyEvent.VK_PLUS);
        this.jbutNextMove.setActionCommand(AC_SHOW_NEXT_MOVE);
        this.jbutNextMove.addActionListener(this);
        this.jbutAllMoves.setMnemonic(KeyEvent.VK_A);
        this.jbutAllMoves.setActionCommand(AC_SHOW_ALL_MOVES);
        this.jbutAllMoves.addActionListener(this);
        this.jbutPrevMove.setMnemonic(KeyEvent.VK_MINUS);
        this.jbutPrevMove.setActionCommand(AC_SHOW_PREV_MOVE);
        this.jbutPrevMove.addActionListener(this);
        this.jbutNoMoves.setMnemonic(KeyEvent.VK_S);
        this.jbutNoMoves.setActionCommand(AC_SHOW_NO_MOVES);
        this.jbutNoMoves.addActionListener(this);
        this.jtextSolution.setEditable(false);
        this.jtextSolution.setPreferredSize(new Dimension(100, 100));   //dummy?!
        final JPanel panelSolutionText = new JPanel(new BorderLayout());
        panelSolutionText.add(this.jtextSolution, BorderLayout.CENTER);
        final JScrollPane scrollSolutionText = new JScrollPane(this.jtextSolution, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollSolutionText.setPreferredSize(new Dimension(100, 100));   //dummy?!
        final RowGroup playSolutionGroup = new RowGroup();
        final JCheckBox groupBox = new JCheckBox("show computed solution");
        groupBox.addItemListener(new ShowHideAction(playSolutionGroup));
        
        final JPanel playPanel = new JPanel();
        final DesignGridLayout playLayout = new DesignGridLayout(playPanel);
        playLayout.row().grid().add(new JSeparator());
        playLayout.row().grid().add(new JLabel("set starting position"));
        playLayout.row().grid().add(this.jbutRandomRobots).add(this.jbutRandomGoal);
        playLayout.row().grid().add(this.jcomboPlaceRobot).add(this.jbutPlaceGoal);
        playLayout.row().grid().add(new JLabel(" "));
        playLayout.row().grid().add(new JSeparator());
        playLayout.row().grid().add(groupBox);
        playLayout.row().group(playSolutionGroup).grid().add(this.jbutSolutionHint).empty();
        playLayout.row().group(playSolutionGroup).grid().add(this.jbutNextMove).add(this.jbutAllMoves);
        playLayout.row().group(playSolutionGroup).grid().add(this.jbutPrevMove).add(this.jbutNoMoves);
        playLayout.row().group(playSolutionGroup).grid().add(scrollSolutionText);
        groupBox.setSelected(true); //false
        //playSolutionGroup.hide();
        
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
        
        this.updateBoardGetRobots();
    }
    
    private void setJComboCenterAlignment(final JComboBox jcombo) {
        final DefaultListCellRenderer renderer = new DefaultListCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        jcombo.setRenderer(renderer);
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
    
    private void refreshBoard() {
        for (JComponent comp : this.boardCells) {
            comp.repaint();
        }
    }
    
    private void refreshBoard(final Move step) {
        for (int pos : step.pathMap.keySet()) {
            this.boardCells[pos].repaint();
        }
    }
    
    private void refreshButtons() {
        if (null == this.solver) {
            this.jbutSolutionHint.setEnabled(false);
            this.jbutNextMove.setEnabled(false);
            this.jbutPrevMove.setEnabled(false);
            this.jbutAllMoves.setEnabled(false);
            this.jbutNoMoves.setEnabled(false);
        } else {
            if (this.solver.getSolutionSize() > 0) { this.jbutSolutionHint.setEnabled(true); }
            this.jbutNextMove.setEnabled(true);
            this.jbutPrevMove.setEnabled(true);
            this.jbutAllMoves.setEnabled(true);
            this.jbutNoMoves.setEnabled(true);
            if (this.board.getGoalRobot() < 0) {
                for (int pos : this.currentPosition) {
                    if (pos == this.board.getGoalPosition()) {
                        this.jbutNextMove.setEnabled(false);
                        this.jbutAllMoves.setEnabled(false);
                        break;
                    }
                }
            } else if ((this.currentPosition[this.board.getGoalRobot()] == this.board.getGoalPosition())
                    || (this.solver.getSolutionSize() < 1)){
                this.jbutNextMove.setEnabled(false);
                this.jbutAllMoves.setEnabled(false);
            }
            if (this.moves.isEmpty()) {
                this.jbutPrevMove.setEnabled(false);
                this.jbutNoMoves.setEnabled(false);
            }
        }
    }
    
    private void showNextMove() {
        final Move step = this.solver.getNextMove();
        if (null != step) {
            this.moves.add(step);
            this.currentPosition[step.robotNumber] = step.newPosition;
            this.showMove(step);
        }
    }
    private void showPrevMove() {
        final Move step = this.solver.getPrevMove();
        if (null != step) {
            this.moves.remove(step);
            this.currentPosition[step.robotNumber] = step.oldPosition;
            this.showMove(step);
        }
    }
    private void showMove(final Move step) {
        this.appendSolutionText((step.stepNumber + 1) + ": ", null);
        this.appendSolutionText(step.strRobotDirection(), COL_ROBOT[step.robotNumber]);
        this.appendSolutionText(" " + step.strOldNewPosition() + (this.solver.isMoveRebound(step) ? " rebound" : "") + "\n", null);
        //System.out.println(step.toString());
        this.refreshButtons();
        this.refreshBoard(step);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (AC_BOARD_QUADRANTS.equals(e.getActionCommand())) {
            this.makeBoardQuadrants();
            this.refreshJComboPlaceRobot();
        } else if (AC_RANDOM_ROBOTS.equals(e.getActionCommand())) {
            this.placeRobot = -1;
            refreshJComboPlaceRobot();
            this.updateBoardRandomRobots();
        } else if (AC_RANDOM_GOAL.equals(e.getActionCommand())) {
            this.placeGoal = false;
            this.updateBoardRandomGoal();
        } else if (AC_SHOW_NEXT_MOVE.equals(e.getActionCommand())) {
            this.showNextMove();
        } else if (AC_SHOW_PREV_MOVE.equals(e.getActionCommand())) {
            this.showPrevMove();
        } else if (AC_SHOW_ALL_MOVES.equals(e.getActionCommand())) {
            while (this.jbutNextMove.isEnabled()) {
                this.showNextMove();
            }
        } else if (AC_SHOW_NO_MOVES.equals(e.getActionCommand())) {
            while (this.jbutPrevMove.isEnabled()) {
                this.showPrevMove();
            }
        } else if (AC_SHOW_HINT.equals(e.getActionCommand())) {
            this.showHint();
        } else if (AC_PLACE_ROBOT.equals(e.getActionCommand())) {
            this.placeRobot = this.jcomboPlaceRobot.getSelectedIndex() - 1;     //item #0 is "Place robot"
        } else if (AC_PLACE_GOAL.equals(e.getActionCommand())) {
            this.placeGoal = !this.placeGoal;
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
            final Dimension mySize = this.getSize();
            final int hWallWidth = mySize.height / H_WALL_DIVISOR;
            final int vWallWidth = mySize.width / V_WALL_DIVISOR;
            
            //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // fill background (walls and center)
            g2d.setColor(COL_BACKGROUND);
            g2d.fillRect(0, 0, mySize.width, mySize.height);
            
            // fill center
            g2d.setPaint(new GradientPaint(0, mySize.height-1, COL_CELL1, mySize.width-1, 0, COL_CELL2));
            g2d.fillRect(vWallWidth, hWallWidth, mySize.width - vWallWidth * 2, mySize.height - hWallWidth * 2);
            
            // fill the 4 walls
            final byte[] walls = board.getWalls(this.boardPosition);
            g2d.setColor(COL_WALL);
            if (walls[Board.NORTH] != 0) {
                g2d.fillRect(0, 0, mySize.width, hWallWidth);
            }
            if (walls[Board.EAST] != 0) {
                g2d.fillRect(mySize.width - vWallWidth, 0, mySize.width, mySize.height);
            }
            if (walls[Board.SOUTH] != 0) { 
                g2d.fillRect(0, mySize.height - hWallWidth, mySize.width, mySize.height);
            }
            if (walls[Board.WEST] != 0) {
                g2d.fillRect(0, 0, vWallWidth, mySize.height);
            }
            
            // paint the robot paths
            if (isModePlay()) {
                final Stroke oldStroke = g2d.getStroke();
                final int pathWidth = Math.min(hWallWidth, vWallWidth);
                g2d.setStroke(new BasicStroke(pathWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
                for (Move step : moves) {
                    final Integer path = step.pathMap.get(Integer.valueOf(this.boardPosition));
                    if (null != path) {
                        g2d.setColor(COL_ROBOT[step.robotNumber]);
                        final int halfHeight = (mySize.height >> 1) + ((step.robotNumber - 2) * pathWidth);
                        final int halfWidth = (mySize.width >> 1) + ((step.robotNumber - 2) * pathWidth);
                        if ((path.intValue() & Move.PATH_NORTH) != 0) {
                            g2d.drawLine(halfWidth, 0, halfWidth, halfHeight);
                        }
                        if ((path.intValue() & Move.PATH_SOUTH) != 0) {
                            g2d.drawLine(halfWidth, halfHeight, halfWidth, mySize.height-1);
                        }
                        if ((path.intValue() & Move.PATH_EAST) != 0) {
                            g2d.drawLine(halfWidth, halfHeight, mySize.width-1, halfHeight);
                        }
                        if ((path.intValue() & Move.PATH_WEST) != 0) {
                            g2d.drawLine(0, halfHeight, halfWidth, halfHeight);
                        }
                    }
                }
                g2d.setStroke(oldStroke);
            }
            
            // paint the goal X
            if (!isModePlay() || placeGoal || (board.getGoalPosition() == this.boardPosition)) {
                final int robot;
                if (isModePlay() && !placeGoal) {
                    robot = board.getGoalRobot();
                } else {
                    int tmp = board.getGoalAt(this.boardPosition);
                    robot = (placeGoal && (tmp >= board.getRobotPositions().length) ? Integer.MAX_VALUE : tmp);
                }
                if (robot < COL_ROBOT.length) {
                    final Paint thePaint;
                    if (robot < 0) {
                        final float fStep = 1.0f / (COL_ROBOT.length-1);
                        final float[] fractions = new float[COL_ROBOT.length];
                        for (int i = 1; i < fractions.length; ++i) {
                            fractions[i] = fractions[i - 1] + fStep;
                        }
                        thePaint = new LinearGradientPaint(vWallWidth*2, hWallWidth*2, vWallWidth*2, mySize.height-1-hWallWidth*2, fractions, COL_ROBOT, MultipleGradientPaint.CycleMethod.REPEAT);
                    } else {
                        thePaint = new GradientPaint(0, 0, COL_ROBOT[robot], 0, mySize.height-1, Color.DARK_GRAY);
                    }
                    g2d.setPaint(thePaint);
                    final Stroke oldStroke = g2d.getStroke();
                    final int strokeWidth = Math.min(hWallWidth, vWallWidth);
                    g2d.setStroke(new BasicStroke(strokeWidth * 3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                    g2d.drawLine(vWallWidth, hWallWidth, mySize.width - 1 - vWallWidth, mySize.height - 1 - hWallWidth);
                    g2d.drawLine(mySize.width - 1 - vWallWidth, hWallWidth, vWallWidth, mySize.height - 1 - hWallWidth);
                    g2d.setStroke(oldStroke);
                    final String goalColorName = ((robot < 0) ? "*" : Board.ROBOT_COLOR_NAMES_SHORT[robot]);
                    g2d.setColor(Color.WHITE);
                    g2d.drawChars(goalColorName.toCharArray(), 0, 1, mySize.width / 2 - 3, mySize.height / 2 + 3);
                }
            }
            
            // paint the robots
            if (isModePlay()) {
                for (int i = 0; i < currentPosition.length; ++i) {
                    if (currentPosition[i] == this.boardPosition) {
                        final Paint fillPaint = new GradientPaint(0, 0, COL_ROBOT[i], 0, mySize.height-1, Color.DARK_GRAY);
                        final Color outlineColor = Color.BLACK;
                        Polygon shapeFoot = new Polygon();
                        shapeFoot.addPoint(mySize.width / 2 - 1, mySize.height * 3 / 4 - 1);
                        shapeFoot.addPoint(vWallWidth, mySize.height - 1 - hWallWidth);
                        shapeFoot.addPoint(mySize.width - 1 - vWallWidth, mySize.height - 1 - hWallWidth);
                        final Ellipse2D.Double shapeBody = new Ellipse2D.Double(
                                vWallWidth * 3, hWallWidth,
                                mySize.width - 1 - vWallWidth * 6,
                                mySize.height - 1 - hWallWidth * 2
                                );
                        g2d.setPaint(fillPaint);    g2d.fill(shapeFoot);
                        g2d.setColor(outlineColor); g2d.draw(shapeFoot);
                        g2d.setPaint(fillPaint);    g2d.fill(shapeBody);
                        g2d.setColor(outlineColor); g2d.draw(shapeBody);
                        g2d.setColor(Color.WHITE);
                        g2d.drawChars(Board.ROBOT_COLOR_NAMES_SHORT[i].toCharArray(), 0, 1, mySize.width / 2 - 3, mySize.height / 2 + 3);
                    }
                }
            }
        }
        
        //implements MouseListener
        @Override
        public void mouseClicked(MouseEvent e) {
            if (placeRobot >= 0) {
                board.setRobot(placeRobot, this.boardPosition);
                placeRobot = -1;
                refreshJComboPlaceRobot();
                updateBoardGetRobots();
            }
            if (placeGoal) {
                if (board.setGoal(this.boardPosition)) {
                    placeGoal = false;
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
