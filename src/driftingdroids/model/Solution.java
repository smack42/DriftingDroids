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

package driftingdroids.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;



public class Solution implements Comparable<Solution> {
    private final Board board;
    private final List<Move> movesList;
    private int moveIndex;
    private int numColors;
    private int numColorChanges;
    private int movedRobots;
    private long finalPositions;
    
    public Solution(Board board) {
        this.board = board;
        this.movesList = new ArrayList<Move>();
        this.moveIndex = 0;
        this.numColors = 0;
        this.numColorChanges = 0;
        this.movedRobots = 0;
        this.finalPositions = 0;
    }
    
    
    public void add(final Move move) {
        this.movesList.add(move);
    }
    
    public int size() {
        return this.movesList.size();
    }
    
    public Set<Integer> getRobotsMoved() {
        final TreeSet<Integer> result = new TreeSet<Integer>(); //sorted set
        for (Move move : this.movesList) {
            result.add(Integer.valueOf(move.robotNumber));
        }
        return result;
    }
    
    public boolean isRebound() {
        return this.isRebound(null);
    }
    
    public boolean isRebound(final Move queryMove) {
        boolean result = false;
        int[] directions = this.board.getRobotPositions().clone();
        Arrays.fill(directions, -1);
        for (Move move : this.movesList) {
            if ((-1 == directions[move.robotNumber]) || (move.direction != (3 & (directions[move.robotNumber] + 2)))) {
                directions[move.robotNumber] = move.direction;
            } else {
                if ((queryMove == null) || (queryMove.equals(move))) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
    
    public void resetMoves() {
        this.moveIndex = 0;
    }
    
    public Move getCurrentMove() {
        if ((this.moveIndex >= 0) && (this.moveIndex < this.movesList.size())) {
            return this.movesList.get(this.moveIndex);
        } else {
            return null;
        }
    }
    
    public Move getNextMove() {
        final Move result =  getCurrentMove();
        if (null != result) {
            this.moveIndex++;
        }
        return result;
    }
    
    public Move getPrevMove() {
        if (this.moveIndex > 0) {
            this.moveIndex--;
        }
        return getCurrentMove();
    }
    
    public Move getLastMove() {
        if (this.movesList.size() > 0) {
            return this.movesList.get(this.movesList.size() - 1);
        } else {
            return null;
        }
    }
    
    public String toMovelistString() {
        StringBuilder s = new StringBuilder();
        s.append("solution: size=").append(this.movesList.size()).append(" *****");
        if (this.movesList.size() > 0) {
            for (Move mov : this.movesList) {
                s.append(' ');
                s.append(mov.strRobotDirection());
            }
        }
        s.append(" *****");
        return s.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        // 1. number of moves
        final Formatter f = new Formatter(s);
        f.format("%02d", Integer.valueOf(this.size()));
        // 2. number of robots moved
        final Set<Integer> thisRobotsMoved = this.getRobotsMoved();
        s.append('/').append(thisRobotsMoved.size()).append('/');
        // 3. list of robots moved
        for (int i = 0;  i < this.board.getRobotPositions().length;  ++i) {
            if (thisRobotsMoved.contains(Integer.valueOf(i))) {
                s.append(Board.ROBOT_COLOR_NAMES_SHORT[i]);
            } else {
                s.append('#');
            }
        }
        return s.toString();
    }
    
    @Override
    public int compareTo(Solution other) {
        // 1. compare number of moves
        if (this.size() < other.size()) {
            return -1;
        } else if (this.size() > other.size()) {
            return 1;
        } else {    //equal number of moves

            // 2. compare number of robots moved
            if (this.numColors < other.numColors) {
                return -1;
            } else if (this.numColors > other.numColors) {
                return 1;
            } else {    //equal number of robots moved

                // 3. compare the actual robots moved
                if (this.movedRobots < other.movedRobots) {
                    return -1;
                } else if (this.movedRobots > other.movedRobots) {
                    return 1;
                } else {    //equal robots moved

                    // 4. compare final robot positions
                    if (this.finalPositions < other.finalPositions) {
                        return -1;
                    } else if (this.finalPositions > other.finalPositions) {
                        return 1;
                    } else {    // equal final positions
                        return 0;
                    }
                }
            }
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Solution) {
            return this.movesList.equals(((Solution)obj).movesList);
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        int result = 0;
        for (final Move move : this.movesList) {
            result = 1000003 * result + move.hashCode();
        }
        return result;
    }

    // set attributes used for sorting of solutions and swap some moves to minimize color changes
    public Solution finish() {
        final List<List<Move>> colorSolution = this.determineColorChanges();
        // set the attributes used for sorting of solutions
        this.numColorChanges = colorSolution.size();
        final Set<Integer> robotsMoved = this.getRobotsMoved();
        this.numColors = robotsMoved.size();
        this.movedRobots = 0;
        for (final Integer robo : robotsMoved) {
            this.movedRobots |= 1 << (30 - robo.intValue());
        }
        this.finalPositions = this.movesList.get(this.movesList.size() - 1).newPositions;
        // for solution01 the order of moves is important and should not be changed here
        if (false == this.board.isSolution01()) {
            this.minimizeColorChanges(colorSolution);
        }
        return this;
    }

    // transform Solution to list of lists of moves, grouped by colors
    private List<List<Move>> determineColorChanges() {
        final List<List<Move>> colorSolution = new ArrayList<List<Move>>();
        LinkedList<Move> moveList = new LinkedList<Move>();
        for (final Move move : this.movesList) {
            if ((false == moveList.isEmpty()) && (moveList.getLast().robotNumber != move.robotNumber)) { // color change
                colorSolution.add(moveList);
                moveList = new LinkedList<Move>();
            }
            moveList.add(move);
        }
        colorSolution.add(moveList);
        return colorSolution;
    }

    // prettify the solution: transpose some moves and thus create longer runs of moves of the same robot color
    private void minimizeColorChanges(List<List<Move>> thisSolution) {
        final long startNano = System.nanoTime();
        if (this.numColors == this.numColorChanges) {
            System.out.println("minimizeColorChanges: no search, already at global minimum " + this.numColorChanges);
            return; // nothing to be minimized here
        }
        final Set<List<List<Move>>> knownSet = new HashSet<List<List<Move>>>();
        final Deque<List<List<Move>>> todoList = new LinkedList<List<List<Move>>>();
        knownSet.add(thisSolution);
        todoList.addLast(thisSolution);
search_loop:
        while (false == todoList.isEmpty()) {
            thisSolution = todoList.removeFirst();
            // iterate the lists of moves, try to swap adjacent lists
try_swap_loop:
            for (int i = 0;  i < thisSolution.size() - 2;  ++i) {
                List<Move> thisMoves = thisSolution.get(i);
                List<Move> nextMoves = thisSolution.get(i + 1);
                // check if the lists of moves can be swapped
                for (final Move move1 : thisMoves) {
                    for (final Move move2 : nextMoves) {
                        if (move1.pathMap.containsKey(Integer.valueOf(move2.newPosition)) ||
                            move2.pathMap.containsKey(Integer.valueOf(move1.oldPosition))) {
                            System.out.println("minimizeColorChanges: blocked path  " + move1.toString() + "  " + move2.toString());
                            continue try_swap_loop; // no swap - blocked path
                        }
                        if ((move1.newPosition == move2.oldPosition - board.directionIncrement[move1.direction]) ||
                            (move2.newPosition == move1.newPosition - board.directionIncrement[move2.direction])) {
                            System.out.println("minimizeColorChanges: blocker position  " + move1.toString() + "  " + move2.toString());
                            continue try_swap_loop; // no swap - blocker position
                        }
                    }
                }
                // swap
                final List<List<Move>> nextSolution = new ArrayList<List<Move>>(thisSolution);
                nextSolution.set(i, nextMoves);
                nextSolution.set(i + 1, thisMoves);
                // merge same-colored adjacent lists of moves
                thisMoves = nextSolution.get(0);
                for (int j = 1;  j < nextSolution.size();  ++j) {
                    nextMoves = nextSolution.get(j);
                    if (thisMoves.get(0).robotNumber == nextMoves.get(0).robotNumber) {
                        thisMoves = new LinkedList<Move>(thisMoves);
                        thisMoves.addAll(nextMoves);
                        nextSolution.set(j - 1, thisMoves);
                        nextSolution.remove(j--);
                        System.out.println("minimizeColorChanges: merged " + Board.ROBOT_COLOR_NAMES_LONG[thisMoves.get(0).robotNumber]);
                    } else {
                        thisMoves = nextMoves;
                    }
                }
                // if this is a new minimum of color changes then update the solution
                if (this.numColorChanges > nextSolution.size()) {
                    System.out.println("minimizeColorChanges: reduced from " + this.numColorChanges + " to " + nextSolution.size());
                    this.numColorChanges = nextSolution.size();
                    this.movesList.clear();
                    int stepNumber = 0;
                    for (final List<Move> moves : nextSolution) {
                        for (final Move move : moves) {
                            move.stepNumber = stepNumber++; // re-number moves
                            this.movesList.add(move);
                        }
                    }
                    knownSet.clear();
                    todoList.clear();
                    if (this.numColors == this.numColorChanges) { // global minimum reached
                        System.out.println("minimizeColorChanges: global minimum reached " + this.numColorChanges);
                        break search_loop; // end of search
                    }
                }
                if (true == knownSet.add(nextSolution)) {
                    todoList.addLast(nextSolution);
                }
            }
        }
        final long millis = (System.nanoTime() - startNano) / 1000000L;
        System.out.println("minimizeColorChanges: finished after " + millis + " ms.");
    }
}

