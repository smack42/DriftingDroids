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
    private final boolean isSolution01, isSolution01NoSpeedup;
    private final int[] minimumMovesToGoal;
    private final int[] directionIncrement;
    
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
        this.isSolution01NoSpeedup = (true == this.isSolution01) && ((true == this.isBoardGoalWildcard) || (4 > this.board.getNumRobots()));
        this.minimumMovesToGoal = new int[board.size];
        this.directionIncrement = this.board.directionIncrement;
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
            Arrays.fill(this.directions[0], DIRECTION_NOT_MOVED_YET);
            this.precomputeMinimumMovesToGoal();
            this.knownStates = new KnownStates();
            
            System.out.println("startState=" + this.stateString(this.states[0]));
            System.out.println("solution01=" + this.isSolution01 + "  isSolution01NoSpeedup=" + this.isSolution01NoSpeedup);
            System.out.println("goalWildcard=" + this.isBoardGoalWildcard);
            System.out.println(this.knownStates.getInfo());
            
            this.iddfs();
            
            this.solutionStoredStates = this.knownStates.size();
            this.solutionMemoryMegabytes = this.knownStates.getMegaBytesAllocated();
            this.knownStates = null;    //allow garbage collection
        }
        this.sortSolutions();
        
        this.solutionMilliSeconds = (System.nanoTime() - startExecute) / 1000000L;
        return this.lastResultSolutions;
    }
    
    
    
    private void precomputeMinimumMovesToGoal() {
        final boolean[] posToDo = new boolean[this.minimumMovesToGoal.length];
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
                    for (int dirIncr : this.directionIncrement) {
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
        final boolean doDfsFast = (false == this.isBoardGoalWildcard) && (false == this.isSolution01) && (true == this.optAllowRebounds);
        System.out.println("doDfsFast=" + doDfsFast);
        for (this.depthLimit = 2;  MAX_DEPTH > this.depthLimit;  ++this.depthLimit) {
            final long nanoDfs = System.nanoTime();
            if (doDfsFast) {
                this.dfsRecursionFast(1, -1, -1, this.states[0]);
            } else {
                this.dfsRecursion(1, -1, -1, this.states[0], this.directions[0]);
            }
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
    
    
    
    // standard version: supports wildcard goal, solution01 special case and option noRebounds
    private void dfsRecursion(final int depth, final int prevRobo, final int prevDirBit0, final int[] oldState, final int[] oldDirs) throws InterruptedException {
        final int height = this.depthLimit - depth + 1;
        final int minMovesToGoal;
        if (true == this.isBoardGoalWildcard) {
            int min = Integer.MAX_VALUE;
            for (final int pos : oldState) {
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
        final int[] obstacles = this.obstacles[depth];
        final int[] newState = this.states[depth];
        final int depth1 = depth + 1;
        for (final int pos : oldState) { obstacles[pos] |= OBSTACLE_ROBOT; }  //set robot positions
        System.arraycopy(oldState, 0, newState, 0, oldState.length);
        final boolean doRecursion = (this.depthLimit > depth1);
        //move all robots
        int robo = 0;
        for (final int oldRoboPos : oldState) {
            final boolean isGoalRobot = (this.goalRobot == robo) || (this.goalRobot < 0);
            if ((minMovesToGoal == height) && (false == isGoalRobot)) {
                ++robo;
                continue;   //useless to move this robot: can't reach goal
            }
            final int oldDir = oldDirs[robo];
            final int obstacleInit = obstacles[oldRoboPos];
            int dir = 0;
            for (final int dirIncr : this.directionIncrement) {
                if (((true == this.optAllowRebounds) || ((oldDir != dir) && (oldDir != (dir ^ 2)))) // (dir + 2) & 3
                        && ((prevRobo != robo) || (prevDirBit0 != (dir & 1)))) {
                    int newRoboPos = oldRoboPos;
                    int obstacle = obstacleInit;
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
                            && ((false == this.isSolution01) || !((this.goalPosition == newRoboPos) && (true == isGoalRobot)))) {
                        newState[robo] = newRoboPos;
                        //special case (isSolution01): we must be able to visit states more than once, so we don't add them to knownStates
                        //the new state is not already known (i.e. stored in knownStates)
                        if (this.isSolution01NoSpeedup || (this.isSolution01 && isGoalRobot) || (this.knownStates.add(newState, height))) {
                            final int[] newDirs = this.directions[depth];
                            System.arraycopy(oldDirs, 0, newDirs, 0, oldDirs.length);
                            newDirs[robo] = dir;
                            if (true == doRecursion) {
                                this.dfsRecursion(depth1, robo, (dir & 1), newState, newDirs);
                            } else {
                                this.dfsLast(depth1, robo, (dir & 1), newState, newDirs);
                            }
                        }
                    }
                }
                ++dir;
            }
            newState[robo++] = oldRoboPos;
        }
        for (final int pos : oldState) { obstacles[pos] ^= OBSTACLE_ROBOT; }  //unset robot positions
    }
    
    
    
    // fast version: (false == this.isBoardGoalWildcard) && (false == this.isSolution01) && (true == this.optAllowRebounds)
    private void dfsRecursionFast(final int depth, final int prevRobo, final int prevDirBit0, final int[] oldState) throws InterruptedException {
        final int minMovesToGoal = this.minimumMovesToGoal[oldState[this.goalRobot]];
        final int height = this.depthLimit - depth + 1;
        if (minMovesToGoal > height) {
            return; //useless to move any robot: can't reach goal
        }
        final int[] obstacles = this.obstacles[depth];
        final int[] newState = this.states[depth];
        final int depth1 = depth + 1;
        for (final int pos : oldState) { obstacles[pos] |= OBSTACLE_ROBOT; }  //set robot positions
        final boolean doRecursion = (this.depthLimit > depth1);
        System.arraycopy(oldState, 0, newState, 0, oldState.length);
        //move all robots
        int robo = 0;
        for (final int oldRoboPos : oldState) {
            if ((minMovesToGoal == height) && (this.goalRobot != robo)) {
                ++robo; //useless to move this robot: can't reach goal
            } else {
                final int obstacleInit = obstacles[oldRoboPos];
                int dir = 0;
                for (final int dirIncr : this.directionIncrement) {
                    if ((prevRobo != robo) || (prevDirBit0 != (dir & 1))) {
                        int newRoboPos = oldRoboPos;
                        int obstacle = obstacleInit;
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
                        if (oldRoboPos != newRoboPos) {
                            newState[robo] = newRoboPos;
                            //the new state is not already known (i.e. stored in knownStates)
                            if (true == this.knownStates.add(newState, height)) {
                                if (true == doRecursion) {
                                    this.dfsRecursionFast(depth1, robo, (dir & 1), newState);
                                } else {
                                    this.dfsLastFast(depth1, robo, (dir & 1), newState);
                                }
                            }
                        }
                    }
                    ++dir;
                }
                newState[robo++] = oldRoboPos;
            }
        }
        for (final int pos : oldState) { obstacles[pos] ^= OBSTACLE_ROBOT; }  //unset robot positions
    }
    
    
    
    // standard version: supports wildcard goal, solution01 special case and option noRebounds
    private void dfsLast(final int depth, final int prevRobo, final int prevDirBit0, final int[] oldState, final int[] oldDirs) throws InterruptedException {
        if (Thread.interrupted()) { throw new InterruptedException(); }
        final int[] obstacles = this.obstacles[depth];
        for (final int pos : oldState) { obstacles[pos] |= OBSTACLE_ROBOT; }  //set robot positions
        //move goal robot(s) only
        for (int robo = this.minRobotLast;  robo < oldState.length;  ++robo) {
            final int oldRoboPos = oldState[robo];
            final int oldDir = oldDirs[robo];
            final int obstacleInit = obstacles[oldRoboPos];
            int dir = 0;
            for (final int dirIncr : this.directionIncrement) {
                if (((true == this.optAllowRebounds) || ((oldDir != dir) && (oldDir != (dir ^ 2)))) // (dir + 2) & 3
                    && ((prevRobo != robo) || (prevDirBit0 != (dir & 1)))) {
                    int newRoboPos = oldRoboPos;
                    int obstacle = obstacleInit;
                    final int wallMask = (1 << dir);
                    while (0 == (obstacle & wallMask)) {        //move the robot until it reaches a wall or another robot.
                        newRoboPos += dirIncr;                  //NOTE: we rely on the fact that all boards are surrounded
                        obstacle = obstacles[newRoboPos];       //by outer walls. without the outer walls we would need
                        if (0 != (obstacle & OBSTACLE_ROBOT)) { //some additional boundary checking here.
                            newRoboPos -= dirIncr;
                            break;
                        }
                    }
                    //the robot has arrived at the goal
                    if ((this.goalPosition == newRoboPos) && hasPerpendicularMove(depth, robo, dir)) {
                        System.arraycopy(oldState, 0, this.states[depth], 0, oldState.length);
                        this.states[depth][robo] = newRoboPos;
                        this.buildSolution(depth);
                    }
                }
                ++dir;
            }
        }
        for (final int pos : oldState) { obstacles[pos] ^= OBSTACLE_ROBOT; }  //unset robot positions
    }
    
    
    
    // fast version: (false == this.isBoardGoalWildcard) && (false == this.isSolution01) && (true == this.optAllowRebounds)
    private void dfsLastFast(final int depth, final int prevRobo, final int prevDirBit0, final int[] oldState) throws InterruptedException {
        if (Thread.interrupted()) { throw new InterruptedException(); }
        final int[] obstacles = this.obstacles[depth];
        final int oldRoboPos = oldState[this.goalRobot];
        for (final int pos : oldState) { obstacles[pos] |= OBSTACLE_ROBOT; }  //set robot positions
        int dir = 0;
        final int obstacleInit = obstacles[oldRoboPos];
        //move goal robot only
        for (final int dirIncr : this.directionIncrement) {
            if ((prevRobo != this.goalRobot) || (prevDirBit0 != (dir & 1))) {
                int newRoboPos = oldRoboPos;
                int obstacle = obstacleInit;
                final int wallMask = (1 << dir);
                while (0 == (obstacle & wallMask)) {        //move the robot until it reaches a wall or another robot.
                    newRoboPos += dirIncr;                  //NOTE: we rely on the fact that all boards are surrounded
                    obstacle = obstacles[newRoboPos];       //by outer walls. without the outer walls we would need
                    if (0 != (obstacle & OBSTACLE_ROBOT)) { //some additional boundary checking here.
                        newRoboPos -= dirIncr;
                        break;
                    }
                }
                //the robot has arrived at the goal
                if (this.goalPosition == newRoboPos) {
                    System.arraycopy(oldState, 0, this.states[depth], 0, oldState.length);
                    this.states[depth][this.goalRobot] = newRoboPos;
                    this.buildSolution(depth);
                }
            }
            ++dir;
        }
        for (final int pos : oldState) { obstacles[pos] ^= OBSTACLE_ROBOT; }  //unset robot positions
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
        return (((lastDir + 1) & 3) == prevDir) || (((lastDir + 3) & 3) == prevDir);
    }
    
    
    
    private void buildSolution(final int depth) {
        Solution newSolution = new Solution(this.board);
        int[] state0 = this.states[0].clone();
        swapGoalLast(state0);
        for (int i = 0;  i < depth;  ++i) {
            final int[] state1 = this.states[i + 1].clone();
            swapGoalLast(state1);
            newSolution.add(new Move(this.board, state0, state1, i));
            state0 = state1;
        }
        newSolution = newSolution.finish();
        System.out.println(newSolution.toMovelistString() + " " + newSolution.toString() + " finalState=" + this.stateString(states[depth]));
        if (false == this.lastResultSolutions.contains(newSolution)) {
            this.lastResultSolutions.add(newSolution);
        }
    }
    
    
    
    private class KnownStates {
        private final AllKeys allKeys;
        
        public KnownStates() {
            this.allKeys = (board.sizeNumBits * (board.getNumRobots() - (isSolution01 ? 1 : 0)) <= 32) ? new AllKeysInt() : new AllKeysLong();
        }
        
        //store the unique keys of all known states
        private abstract class AllKeys {
            protected final KeyDepthMap theMap;
            
            protected AllKeys() {
                this.theMap = KeyDepthMapFactory.newInstance(board);
            }
            
            public abstract boolean add(final int[] state, final int depth);
            
            public long getBytesAllocated() {
                return this.theMap.allocatedBytes();
            }
            
            public abstract String getInfo();
        }
        //store the unique keys of all known states in 32-bit ints
        //supports up to 4 robots with a board size of 256 (16*16)
        private final class AllKeysInt extends AllKeys {
            private final KeyMakerInt keyMaker = KeyMakerInt.createInstance(board.getNumRobots(), board.sizeNumBits, isBoardGoalWildcard, isSolution01);
            public AllKeysInt() {
                super();
            }
            @Override
            public final boolean add(final int[] state, final int depth) {
                final int key = this.keyMaker.run(state);
                return this.theMap.putIfGreater(key, depth);
            }
            @Override
            public String getInfo() {
                return this.getClass().getSimpleName() + "," + this.theMap.getClass().getSimpleName() + "," + (null == this.keyMaker ? "n/a" : this.keyMaker.getClass().getSimpleName());
            }
        }
        //store the unique keys of all known states in 64-bit longs
        //supports more than 4 robots and/or board sizes larger than 256
        private final class AllKeysLong extends AllKeys {
            private final KeyMakerLong keyMaker = KeyMakerLong.createInstance(board.getNumRobots(), board.sizeNumBits, isBoardGoalWildcard, isSolution01);
            public AllKeysLong() {
                super();
            }
            @Override
            public final boolean add(final int[] state, final int depth) {
                final long key = this.keyMaker.run(state);
                return this.theMap.putIfGreater(key, depth);
            }
            @Override
            public String getInfo() {
                return this.getClass().getSimpleName() + "," + this.theMap.getClass().getSimpleName() + "," + (null == this.keyMaker ? "n/a" : this.keyMaker.getClass().getSimpleName());
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
        public String getInfo() {
            return "KnownStates(" + this.allKeys.getInfo() + ")";
        }
    }

}
