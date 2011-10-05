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

package driftingdroids.model;

import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

import java.util.ArrayList;
import java.util.List;



public class SolverIDDFS extends AbstractSolver {
    
    private static final int MAXIMUM_SEARCH_DEPTH = 100;    //used for static data structures
    
    private final List<int[]> solution; //list of robot positions in reverse order, result of "execute()".
    private final int allSearchPositions[][][];     // [depth] [robots*directions] [robots]
    
    public SolverIDDFS(Board board) {
        super(board);
        this.solution = new ArrayList<int[]>();
        this.allSearchPositions = new int[MAXIMUM_SEARCH_DEPTH][this.boardNumRobots * 4][this.boardNumRobots];
    }
    
    @Override
    public int execute() throws InterruptedException {
        final long startT = System.currentTimeMillis();

        final KnownStates knownStates = this.createKnownStates();
        final int[] startState = this.board.getRobotPositions().clone();
        swapGoalLast(startState);
        System.out.printf("startState=" + this.stateString(startState) + "\n");
        final int goalRobot = ((true == this.isBoardGoalWildcard) ? -1 : this.boardNumRobots - 1);
        
        //do the search - get the result in list "solution"
        this.iddfs(startState, this.board.getGoalPosition(), goalRobot, knownStates);
        this.solutionStoredStates = knownStates.size();
        
        //convert the list "solution" into an array of Moves
        this.solutionMoves = new Move[this.solution.size() - 1];
        swapGoalLast(this.solution.get(this.solution.size() - 1));
        for (int resultIndex = 0, move = this.solution.size() - 1;  move > 0;  --move, resultIndex++) {
            swapGoalLast(this.solution.get(move - 1));
            this.solutionMoves[resultIndex] = new Move(this.board, this.solution.get(move), this.solution.get(move - 1), resultIndex);
        }
        this.solutionMoveIndex = 0;
        
        this.solutionMilliSeconds = System.currentTimeMillis() - startT;
        return this.getSolutionSize();
    }
    
    /* iterative deepening depth-first search (IDDFS).
     */
    private void iddfs(final int[] startState, final int goalPosition, final int goalRobot, final KnownStates knownStates) throws InterruptedException {
        this.solution.clear();
        
        long prevNT = System.currentTimeMillis();
        
        for (int depthLimit = 0;  this.solution.isEmpty();  ++depthLimit) {
            
            final long thisNT = System.currentTimeMillis();
            if (thisNT - prevNT > 1000) {
                System.out.println("... DLS at depthLimit="+depthLimit);
                prevNT = thisNT;
            }
            knownStates.clearIncreaseCapacity();
            this.dls(startState, goalPosition, goalRobot, depthLimit, 0, knownStates);
        }
    }
    
    /* depth-limited search (DLS); a modified depth-first search (DFS).
     * @return true if the goal has been reached.
     * as a side effect the robot positions of the path leading to the goal
     * have been stored in this.solution.
     */
    private boolean dls(final int[] state, final int goalPosition, final int goalRobot, final int depthLimit, final int depth, final KnownStates knownStates) throws InterruptedException {
        if (Thread.interrupted()) {
            //System.out.println("!!! solver was interrupted !!!");
            throw new InterruptedException();
        }
        boolean result = false;
        //check if the current position is the goal
        if (goalRobot < 0) {  //wildcard goal
            for (int position : state) {
                if (position == goalPosition) {
                    result = true;
                    break;
                }
            }
        } else if (state[goalRobot] == goalPosition) {
            result = true;
        }
        //expand all "child" positions from the current one and follow them (recursion)
        if (!result && (depthLimit > depth)) {
            final int[][] children = this.allSearchPositions[depth];
            int numChildren;
            if ((depthLimit == depth + 1) && (goalRobot >= 0)) {
                numChildren = this.expandLast(state, depth + 1, children, goalPosition, goalRobot);
            } else {
                numChildren = this.expand(state, depth + 1, children, knownStates);
            }
            for (int i = 0;  i < numChildren;  ++i) {
                if (this.dls(children[i], goalPosition, goalRobot, depthLimit, depth + 1, knownStates)) {
                    result = true;
                    break;
                }
            }
        }
        //if the current position is the goal or it's on the path to the goal
        //then store it as part of the solution.
        if (result) {
            this.solution.add(state);
        }
        return result;
    }
    
    

    /* move each robot in each direction from the current position.
     * fill the array "result" with the new positions.
     */
    private int expand(final int[] state, final int depth, final int[][] result, final KnownStates knownStates) {
        int resultIndex = 0;
        for (int pos : state) { this.expandRobotPositions[pos] = true; }
        for (int robo = 0; robo < state.length; ++robo) {
            final int oldRoboPos = state[robo];
            for (int dir = 0; dir < 4; ++dir) {
                int newRoboPos = oldRoboPos;
                //move the robot until it reaches a wall or another robot.
                //NOTE: we rely on the fact that all boards are surrounded
                //by outer walls. without the outer walls we would need
                //some additional boundary checking here.
                final int dirIncr = this.board.directionIncrement[dir];
                while (0 == this.boardWalls[newRoboPos][dir]) {
                    newRoboPos += dirIncr;
                    if (this.expandRobotPositions[newRoboPos]) {
                        newRoboPos -= dirIncr;
                        break;
                    }
                }
                //here we only accept the new position of the moved robot
                //if it's not already known at a lesser search depth.
                if (oldRoboPos != newRoboPos) {
                    state[robo] = newRoboPos;  //temporarily!
                    if (false == knownStates.isKnown(state, depth)) {
                        System.arraycopy(state, 0, result[resultIndex], 0, state.length);
                        ++resultIndex;
                    }
                    state[robo] = oldRoboPos;  //temporarily! reverted!
                }
            }
        }
        for (int pos : state) { this.expandRobotPositions[pos] = false; }
        return resultIndex;
    }
    
    /* move only the "goal robot" in each direction from the current position.
     * fill the array "result" with the new positions.
     */
    private int expandLast(final int[] state, final int depth, final int[][] result, final int goalPosition, final int goalRobot) {
        int resultIndex = 0;
        for (int pos : state) { this.expandRobotPositions[pos] = true; }
        final int oldRoboPos = state[goalRobot];
        for (int dir = 0;  dir < 4;  ++dir) {
            int newRoboPos = oldRoboPos;
            //move the robot until it reaches a wall or another robot.
            //NOTE: we rely on the fact that all boards are surrounded
            //by outer walls. without the outer walls we would need
            //some additional boundary checking here.
            final int dirIncr = this.board.directionIncrement[dir];
            while (0 == this.boardWalls[newRoboPos][dir]) {
                newRoboPos += dirIncr;
                if (this.expandRobotPositions[newRoboPos]) {
                    newRoboPos -= dirIncr;
                    break;
                }
            }
            //here we only accept the new position of the goal robot
            //if it's the goal position.
            if (goalPosition == newRoboPos) {
                for (int i = 0;  i < state.length;  ++i) {
                    result[resultIndex][i] = state[i];
                }
                result[resultIndex][goalRobot] = newRoboPos;
                ++resultIndex;
                break;
            }
        }
        for (int pos : state) { this.expandRobotPositions[pos] = false; }
        return resultIndex;
    }
    
    
    
    
    private KnownStates createKnownStates() {
        return ((true == this.isBoardStateInt32) ? new KnownStatesInt() : new KnownStatesLong());    
    }
    
    private interface KnownStates {
        
        /* remove all known states.
         */
        public void clearIncreaseCapacity();
        
        /* @return the number of states that are currently known.
         */
        public int size();
        
        /* remember all "states" during a solution search.
         * a "state" is: key=robot_positions and value=search_depth.
         * @return true if this state was already known at this or a lesser depth.
         *         false if this state was unknown before or known at a greater depth.
         */
        public boolean isKnown(final int[] positions, final int depth);
    }
    
    
    private final class KnownStatesInt implements KnownStates {
        private final KeyMakerInt keyMaker;
        private Int2ByteOpenHashMap theMap;
        
        public KnownStatesInt() {
            this.theMap = new Int2ByteOpenHashMap();
            this.keyMaker = createKeyMakerInt();
        }
        @Override
        public final void clearIncreaseCapacity() {
            final int oldSize = this.theMap.size();
            this.theMap = null;
            this.theMap = new Int2ByteOpenHashMap(oldSize * 2);
        }
        @Override
        public final int size() {
            return this.theMap.size();
        }
        @Override
        public final boolean isKnown(final int[] positions, final int depth) {
            boolean result = true;
            final int key = this.keyMaker.run(positions);
            final int oldDepth = this.theMap.get(key);
            if ((oldDepth == 0) || (oldDepth > depth)) {
                this.theMap.put(key, (byte)depth);
                result = false;
            }
            return result;
        }
    }
    
    private final class KnownStatesLong implements KnownStates {
        private final KeyMakerLong keyMaker;
        private Long2ByteOpenHashMap theMap;
        
        public KnownStatesLong() {
            this.theMap = new Long2ByteOpenHashMap();
            this.keyMaker = createKeyMakerLong();
        }
        @Override
        public final void clearIncreaseCapacity() {
            final int oldSize = this.theMap.size();
            this.theMap = null;
            this.theMap = new Long2ByteOpenHashMap(oldSize * 2);
        }
        @Override
        public final int size() {
            return this.theMap.size();
        }
        @Override
        public final boolean isKnown(final int[] positions, final int depth) {
            boolean result = true;
            final long key = this.keyMaker.run(positions);
            final int oldDepth = this.theMap.get(key);
            if ((oldDepth == 0) || (oldDepth > depth)) {
                this.theMap.put(key, (byte)depth);
                result = false;
            }
            return result;
        }
    }
}

