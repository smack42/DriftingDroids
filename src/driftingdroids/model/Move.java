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

import java.util.HashMap;
import java.util.Map;



public class Move {
    public final Board board;
    public int stepNumber;
    public final int robotNumber;
    public final int oldPosition;
    public final int newPosition;
    public final int direction;
    public final Map<Integer,Integer> pathMap;  //key=position, value=PATH
    public final long oldPositions;  // positions of all robots before this move
    public final long newPositions;  // positions of all robots after this move
    
    public static final int PATH_NORTH = 1 << Board.NORTH;
    public static final int PATH_EAST  = 1 << Board.EAST;
    public static final int PATH_SOUTH = 1 << Board.SOUTH;
    public static final int PATH_WEST  = 1 << Board.WEST;
    
    public Move(Board board, int[] oldPositions, int[] newPositions, int stepNumber) {
        this.board = board;
        this.stepNumber = stepNumber;
        int robotNumber=0, oldPosition=0, newPosition=0;
        for (int robo = 0; robo < oldPositions.length; ++robo) {
            if (oldPositions[robo] != newPositions[robo]) {
                robotNumber = robo;
                oldPosition = oldPositions[robo];
                newPosition = newPositions[robo];
                break;
            }
        }
        this.robotNumber = robotNumber;
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
        
        this.pathMap = new HashMap<Integer, Integer>();
        final int diffPos = newPosition - oldPosition;
        this.direction = board.getDirection(diffPos);
        final int pathStart = 1 << this.direction;
        final int pathEnd = 1 << board.getDirection(-diffPos);
        final int posIncr = board.directionIncrement[this.direction];
        int i = oldPosition;
        this.pathMap.put(Integer.valueOf(i), Integer.valueOf(pathStart));
        for (i += posIncr ; i != newPosition; i += posIncr) {
            this.pathMap.put(Integer.valueOf(i), Integer.valueOf(pathStart + pathEnd));
        }
        this.pathMap.put(Integer.valueOf(i), Integer.valueOf(pathEnd));

        long oldPos = 0;
        for (final int pos : oldPositions) {
            oldPos = (oldPos << board.sizeNumBits) | pos;
        }
        this.oldPositions = oldPos;
        long newPos = 0;
        for (final int pos : newPositions) {
            newPos = (newPos << board.sizeNumBits) | pos;
        }
        this.newPositions = newPos;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if ((null == obj) || !(obj instanceof Move)) {
            return false;
        }
        final Move other = (Move) obj;
        return ((this.stepNumber == other.stepNumber) &&
                (this.robotNumber == other.robotNumber) &&
                (this.oldPosition == other.oldPosition) &&
                (this.newPosition == other.newPosition));
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = this.stepNumber;
        result = 1000003 * result + this.robotNumber;
        result = 1000003 * result + this.oldPosition;
        result = 1000003 * result + this.newPosition;
        return result;
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ((this.stepNumber + 1) + ": " + this.strRobotDirection() + " " + this.strOldNewPosition());
    }
    
    public String strRobotDirection() {
        final String dir;
        switch (this.pathMap.get(Integer.valueOf(this.oldPosition)).intValue()) {
        case PATH_NORTH : dir = "N"; break; //up    / NORTH
        case PATH_EAST  : dir = "E"; break; //right / EAST
        case PATH_SOUTH : dir = "S"; break; //down  / SOUTH
        case PATH_WEST  : dir = "W"; break; //left  / WEST
        default         : dir = "?"; break;
        }
        return (Board.ROBOT_COLOR_NAMES_SHORT[this.robotNumber] + dir); 
    }
    
    public String strDirectionL10N() {
        final String dir;
        switch (this.pathMap.get(Integer.valueOf(this.oldPosition)).intValue()) {
        case PATH_NORTH : dir = Board.L10N.getString("move.direction.N.text"); break;   //up    / NORTH
        case PATH_EAST  : dir = Board.L10N.getString("move.direction.E.text"); break;   //right / EAST
        case PATH_SOUTH : dir = Board.L10N.getString("move.direction.S.text"); break;   //down  / SOUTH
        case PATH_WEST  : dir = Board.L10N.getString("move.direction.W.text"); break;   //left  / WEST
        default         : dir = "?"; break;
        }
        return dir;
    }
    
    public String strDirectionL10Nlong() {
        final String dir;
        switch (this.pathMap.get(Integer.valueOf(this.oldPosition)).intValue()) {
        case PATH_NORTH : dir = Board.L10N.getString("move.direction.North.text");  break;   //up
        case PATH_EAST  : dir = Board.L10N.getString("move.direction.East.text");   break;   //right
        case PATH_SOUTH : dir = Board.L10N.getString("move.direction.South.text");  break;   //down
        case PATH_WEST  : dir = Board.L10N.getString("move.direction.West.text");   break;   //left
        default         : dir = "???"; break;
        }
        return dir;
    }
    
    public String strOldNewPosition() {
        return ("(" + (this.oldPosition % this.board.width) + "," + (this.oldPosition / this.board.width) +
                ") -> (" + (this.newPosition % this.board.width) + "," + (this.newPosition / this.board.width) + ")");
    }
}
