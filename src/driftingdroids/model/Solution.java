/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011, 2012, 2013  Michael Henke

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
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;



public class Solution implements Comparable<Solution> {
    private final Board board;
    private final List<Move> movesList;
    private int moveIndex;
    
    public Solution(Board board) {
        this.board = board;
        this.movesList = new ArrayList<Move>();
        this.moveIndex = 0;
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
        @SuppressWarnings("resource")
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
            final Set<Integer> thisRobotsMoved = this.getRobotsMoved();
            final Set<Integer> otherRobotsMoved = other.getRobotsMoved();
            if (thisRobotsMoved.size() < otherRobotsMoved.size()) {
                return -1;
            } else if (thisRobotsMoved.size() > otherRobotsMoved.size()) {
                return 1;
            } else {    //equal number of robots moved
                // 3. compare the moved robots
                int thisRobotsMovedSum = 0,  otherRobotsMovedSum = 0;
                for (Integer robot : thisRobotsMoved) {
                    thisRobotsMovedSum = (thisRobotsMovedSum << 3) + robot.intValue();
                }
                for (Integer robot : otherRobotsMoved) {
                    otherRobotsMovedSum = (otherRobotsMovedSum << 3) + robot.intValue();
                }
                if (thisRobotsMovedSum < otherRobotsMovedSum) {
                    return -1;
                } else if (thisRobotsMovedSum > otherRobotsMovedSum) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

}

