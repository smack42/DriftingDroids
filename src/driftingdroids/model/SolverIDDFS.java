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

package driftingdroids.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class SolverIDDFS extends Solver {
    
    private static final int MAX_DEPTH = 126;
    
    private final int[][] states;
    private final int[][] directions;
    private static final int DIRECTION_NOT_MOVED_YET = 7;
    private final int[][] obstacles = new int[MAX_DEPTH][];
    private static final int OBSTACLE_ROBOT = (1 << 4);
    private KnownStates knownStates;
    private final int goalPosition;
    private final int minRobotLast;
    private final int goalRobot;
    private final boolean isSolution01;
    private final int[] minimumMovesToGoal;
    
    private int depthLimit;
    

    protected SolverIDDFS(final Board board) {
        super(board);
        this.initObstacles();
        this.states = new int[MAX_DEPTH][this.board.getRobotPositions().length];
        this.directions = new int[MAX_DEPTH][this.board.getRobotPositions().length];
        this.goalPosition = (null == this.board.getGoal() ? 0 : this.board.getGoal().position);
        this.minRobotLast = (this.isBoardGoalWildcard ? 0 : this.states[0].length - 1); //swapGoalLast
        this.goalRobot = (this.isBoardGoalWildcard ? (null == this.board.getGoal() ? 0 : this.board.getGoal().robotNumber) : this.minRobotLast); //swapGoalLast
        this.isSolution01 = this.board.isSolution01();
        this.minimumMovesToGoal = new int[board.size];
    }
    
    
    
    private void initObstacles() {
        this.obstacles[0] = new int[board.size];
        for (int pos = 0;  pos < this.obstacles[0].length;  ++pos) {
            int obstacle = 0;
            for (int dir = 0;  dir < 4;  ++dir) {
                if (true == this.boardWalls[dir][pos]) { obstacle |= (1 << dir); }
            }
            this.obstacles[0][pos] = obstacle;
        }
        for (int depth = 1;  depth < this.obstacles.length;  ++depth) {
            this.obstacles[depth] = this.obstacles[0].clone();
        }
    }
    
    
    
    @Override
    public List<Solution> execute() throws InterruptedException {
        final long startExecute = System.nanoTime();
        this.lastResultSolutions = new ArrayList<Solution>();
        
        System.out.println("***** " + this.getClass().getSimpleName() + " *****");
        System.out.println("options: " + this.getOptionsAsString());
        
        if (null == this.board.getGoal()) {
            System.out.println("no goal is set - nothing to solve!");
        } else {
            this.states[0] = this.board.getRobotPositions().clone();
            swapGoalLast(this.states[0]);   //goal robot is always the last one.
            System.out.println("startState=" + this.stateString(this.states[0]));
            
            Arrays.fill(this.directions[0], DIRECTION_NOT_MOVED_YET);
            
            this.iddfs();
            
            this.solutionStoredStates = this.knownStates.size();
            this.knownStates = null;    //allow garbage collection
        }
        this.sortSolutions();
        
        this.solutionMilliSeconds = (System.nanoTime() - startExecute) / 1000000L;
        return this.lastResultSolutions;
    }
    
    
    
    private void precomputeMinimumMovesToGoal() {
        final boolean posToDo[] = new boolean[this.minimumMovesToGoal.length];
        Arrays.fill(this.minimumMovesToGoal, Integer.MAX_VALUE);
        this.minimumMovesToGoal[this.goalPosition] = 0;
        posToDo[this.goalPosition] = true;
        for (boolean done = false;  false == done;  ) {
            done = true;
            for (int pos = 0;  pos < posToDo.length;  ++pos) {
                if (true == posToDo[pos]) {
                    posToDo[pos] = false;
                    final int depth = this.minimumMovesToGoal[pos] + 1;
                    int dir = -1;
                    for (int dirIncr : this.board.directionIncrement) {
                        int newPos = pos;
                        final boolean[] walls = this.boardWalls[++dir];
                        while (false == walls[newPos]) {    //move the robot until it reaches a wall.
                            newPos += dirIncr;              //NOTE: we rely on the fact that all boards are surrounded by outer walls.
                            if (depth < this.minimumMovesToGoal[newPos]) {
                                this.minimumMovesToGoal[newPos] = depth;
                                posToDo[newPos] = true;
                                done = false;
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    
    private void iddfs() throws InterruptedException {
        final long nanoStart = System.nanoTime();
        this.precomputeMinimumMovesToGoal();
        this.knownStates = null;
        this.knownStates = new KnownStates();
        for (this.depthLimit = 2;  MAX_DEPTH > this.depthLimit;  ++this.depthLimit) {
            final long nanoDfs = System.nanoTime();
            this.dfsRecursion(1, -1, -1);
            final long nanoEnd = System.nanoTime();
            System.out.println("iddfs:  finished depthLimit=" + this.depthLimit +
                    " megaBytes=" + this.knownStates.getMegaBytesAllocated() +
                    " time=" + (nanoEnd - nanoDfs) / 1000000L + "ms" + 
                    " totalTime=" + (nanoEnd - nanoStart) / 1000000L + "ms");
            if (false == this.lastResultSolutions.isEmpty()) {
                break;  //found solution(s)
            }
        }
    }
    
    
    
    private void dfsRecursion(final int depth, final int lastRobo, final int lastDirReverse) throws InterruptedException {
        //if (Thread.interrupted()) { throw new InterruptedException(); }
        final int[] oldState = this.states[depth - 1];
        final int height = this.depthLimit - depth + 1;
        final int minMovesToGoal;
        if (true == this.isBoardGoalWildcard) {
            int min = Integer.MAX_VALUE;
            for (int pos : oldState) {
                final int tmp = this.minimumMovesToGoal[pos];
                if (min > tmp) { min = tmp; }
            }
            minMovesToGoal = min;
        } else {
            minMovesToGoal = this.minimumMovesToGoal[oldState[this.goalRobot]];
        }
        if (minMovesToGoal > height) {
            return; //useless to move any robot: can't reach goal
        }
        final int[] newState = this.states[depth];
        final int[] oldDirs = this.directions[depth - 1];
        final int depth1 = depth + 1;
        final int[] obstacles = this.obstacles[depth];
        for (int pos : oldState) { obstacles[pos] |= OBSTACLE_ROBOT; }  //set robot positions
        System.arraycopy(oldState, 0, newState, 0, oldState.length);
        //move all robots
        int robo = -1;
        for (int oldRoboPos : oldState) {
            ++robo;
            final boolean isGoalRobot = (this.goalRobot == robo) || (this.goalRobot < 0);
            if ((minMovesToGoal == height) && (false == isGoalRobot)) {
                continue;   //useless to move this robot: can't reach goal
            }
            final int oldDir = oldDirs[robo];
            int dir = -1;
            for (int dirIncr : this.board.directionIncrement) {
                ++dir;
                if (((true == this.optAllowRebounds) || ((oldDir != dir) && (oldDir != ((dir + 2) & 3))))
                        && ((lastRobo != robo) || (lastDirReverse != dir))) {
                    int newRoboPos = oldRoboPos;
                    int obstacle = obstacles[newRoboPos];
                    final int wallMask = (1 << dir);
                    while (0 == (obstacle & wallMask)) {        //move the robot until it reaches a wall or another robot.
                        newRoboPos += dirIncr;                  //NOTE: we rely on the fact that all boards are surrounded
                        obstacle = obstacles[newRoboPos];       //by outer walls. without the outer walls we would need
                        if (0 != (obstacle & OBSTACLE_ROBOT)) { //some additional boundary checking here.
                            newRoboPos -= dirIncr;
                            break;
                        }
                    }
                    //the robot has actually moved
                    //special case (isSolution01): the goal robot has _NOT_ arrived at the goal
                    if ((oldRoboPos != newRoboPos)
                            && ((false == this.isSolution01)
                                    || !((this.goalPosition == newRoboPos) && (true == isGoalRobot)))
                            ) {
                        newState[robo] = newRoboPos;
                        //special case (isSolution01): we must be able to visit states more than once, so we don't add them to knownStates
                        //the new state is not already known (i.e. stored in knownStates)
                        if ((true == this.isSolution01) || true == this.knownStates.add(newState, height)) {
                            final int[] newDirs = this.directions[depth];
                            System.arraycopy(oldDirs, 0, newDirs, 0, oldDirs.length);
                            newDirs[robo] = dir;
                            if (this.depthLimit > depth1) {
                                this.dfsRecursion(depth1, robo, ((dir + 2) & 3));
                            } else {
                                this.dfsLast(depth1, robo, ((dir + 2) & 3));
                            }
                        }
                    }
                }
            }
            newState[robo] = oldRoboPos;
        }
        for (int pos : oldState) { obstacles[pos] ^= OBSTACLE_ROBOT; }  //unset robot positions
    }
    
    
    
    private void dfsLast(final int depth, final int lastRobo, final int lastDirReverse) throws InterruptedException {
        if (Thread.interrupted()) { throw new InterruptedException(); }
        final int[] oldState = this.states[depth - 1];
        final int[] oldDirs = this.directions[depth - 1];
        final int[] obstacles = this.obstacles[depth];
        for (int pos : oldState) { obstacles[pos] |= OBSTACLE_ROBOT; }  //set robot positions
        //move goal robot(s) only
        for (int robo = this.minRobotLast;  robo < oldState.length;  ++robo) {
            final int oldRoboPos = oldState[robo];
            final int oldDir = oldDirs[robo];
            int dir = -1;
            for (int dirIncr : this.board.directionIncrement) {
                ++dir;
                if (((true == this.optAllowRebounds) || ((oldDir != dir) && (oldDir != ((dir + 2) & 3))))
                    && ((lastRobo != robo) || (lastDirReverse != dir))) {
                    int newRoboPos = oldRoboPos;
                    int obstacle = obstacles[newRoboPos];
                    final int wallMask = (1 << dir);
                    while (0 == (obstacle & wallMask)) {        //move the robot until it reaches a wall or another robot.
                        newRoboPos += dirIncr;                  //NOTE: we rely on the fact that all boards are surrounded
                        obstacle = obstacles[newRoboPos];       //by outer walls. without the outer walls we would need
                        if (0 != (obstacle & OBSTACLE_ROBOT)) { //some additional boundary checking here.
                            newRoboPos -= dirIncr;
                            break;
                        }
                    }
                    //the robot has actually moved and has arrived at the goal
                    if ((this.goalPosition == newRoboPos) && (oldRoboPos != newRoboPos)
                            && hasPerpendicularMove(depth, robo, dir)) {
                        oldState[robo] = newRoboPos;
                        final int height = this.depthLimit - depth + 1;
                        if (true == this.knownStates.add(oldState, height)) {   //the new state is not already known
                            System.arraycopy(oldState, 0, this.states[depth], 0, oldState.length);
                            oldState[robo] = oldRoboPos;
                            this.buildSolution(depth);
                        } else {
                            oldState[robo] = oldRoboPos;
                        }
                    }
                }
            }
        }
        for (int pos : oldState) { obstacles[pos] ^= OBSTACLE_ROBOT; }  //unset robot positions
    }
    
    
    
    private boolean hasPerpendicularMove(final int depth, final int robot, final int lastDir) {
        int prevDir = this.directions[0][robot];
        for (int i = 1;  depth > i;  ++i) {
            final int thisDir = this.directions[i][robot];
            if ((((thisDir + 1) & 3) == prevDir) || (((thisDir + 3) & 3) == prevDir)) {
                return true;
            }
            prevDir = thisDir;
        }
        if ((((lastDir + 1) & 3) == prevDir) || (((lastDir + 3) & 3) == prevDir)) {
            return true;
        }
        return false;
    }
    
    
    
    private void buildSolution(final int depth) {
        final Solution tmpSolution = new Solution(this.board);
        int[] state0 = this.states[0].clone();
        swapGoalLast(state0);
        for (int i = 0;  i < depth;  ++i) {
            final int[] state1 = this.states[i + 1].clone();
            swapGoalLast(state1);
            tmpSolution.add(new Move(this.board, state0, state1, i));
            state0 = state1;
        }
        this.lastResultSolutions.add(tmpSolution);
        System.out.println(tmpSolution.toMovelistString() + " " + tmpSolution.toString() + " finalState=" + this.stateString(states[depth]));
    }
    
    
    
    private class KnownStates {
        private final AllKeys allKeys;
        
        public KnownStates() {
            this.allKeys = ((true == isBoardStateInt32) ? new AllKeysInt() : new AllKeysLong());
        }
        
        //store the unique keys of all known states
        private abstract class AllKeys {
            protected final TrieMapByte theMap;
            
            protected AllKeys() {
                this.theMap = new TrieMapByte(Math.max(12, board.getNumRobots() * board.sizeNumBits));
            }
            
            public abstract boolean add(final int[] state, final int depth);
            
            public long getBytesAllocated() {
                return this.theMap.getBytesAllocated();
            }
        }
        //store the unique keys of all known states in 32-bit ints
        //supports up to 4 robots with a board size of 256 (16*16)
        private final class AllKeysInt extends AllKeys {
            private final KeyMakerInt keyMaker = KeyMakerInt.createInstance(board.getNumRobots(), board.sizeNumBits, isBoardGoalWildcard);
            public AllKeysInt() {
                super();
            }
            @Override
            public final boolean add(final int[] state, final int depth) {
                final int key = this.keyMaker.run(state);
                return this.theMap.putIfGreater(key, depth);
            }
        }
        //store the unique keys of all known states in 64-bit longs
        //supports more than 4 robots and/or board sizes larger than 256
        private final class AllKeysLong extends AllKeys {
            private final KeyMakerLong keyMaker = KeyMakerLong.createInstance(board.getNumRobots(), board.sizeNumBits, isBoardGoalWildcard);
            public AllKeysLong() {
                super();
            }
            @Override
            public final boolean add(final int[] state, final int depth) {
                final long key = this.keyMaker.run(state);
                return this.theMap.putIfGreater(key, depth);
            }
        }

        public boolean add(int[] state, int depth) {
            return this.allKeys.add(state, depth);
        }
        public final int size() {
            return this.allKeys.theMap.size();
        }
        public final int getMegaBytesAllocated() {
            return (int)((this.allKeys.getBytesAllocated() + (1 << 20) - 1) >> 20);
        }
    }

}
