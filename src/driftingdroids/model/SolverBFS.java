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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;

import org.enerj.core.SparseBitSet;



public class SolverBFS {
    
    public enum SOLUTION_MODE {
        ANY("any"), MINIMUM("minimum"), MAXIMUM("maximum");
        private final String name;
        private SOLUTION_MODE(String name) { this.name = name; }
        @Override public String toString() { return this.name; }
    }
    
    private final Board board;
    private final byte[][] boardWalls;
    private final int boardSizeNumBits;
    private final int boardSizeBitMask;
    private final int boardNumRobots;
    private final boolean isBoardStateInt32;
    private final boolean isBoardGoalWildcard;
    private final boolean[] expandRobotPositions;
    
    private SOLUTION_MODE optSolutionMode = SOLUTION_MODE.ANY;
    private boolean optAllowRebounds = true;
    
    private Solution lastResultSolution = null;
    private long solutionMilliSeconds = 0;
    private int solutionStoredStates = 0;
    
    
    
    public SolverBFS(final Board board) {
        this.board = board;
        this.boardWalls = this.board.getWalls();
        this.boardSizeNumBits = 32 - Integer.numberOfLeadingZeros(this.board.size - 1); //ceil(log2(x))
        int bitMask = 0;
        for (int i = 0;  i < this.boardSizeNumBits;  ++i) { bitMask += bitMask + 1; }
        this.boardSizeBitMask = bitMask;
        this.boardNumRobots = this.board.getRobotPositions().length;
        this.isBoardStateInt32 = (this.boardSizeNumBits * this.boardNumRobots <= 32);
        this.isBoardGoalWildcard = (this.board.getGoalRobot() < 0);
        this.expandRobotPositions = new boolean[this.board.size];
        Arrays.fill(this.expandRobotPositions, false);
    }
    
    
    
    public Solution get() {
        return this.lastResultSolution;
    }
    
    
    
    public Solution execute() throws InterruptedException {
        final long startExecute = System.currentTimeMillis();
        this.lastResultSolution = new Solution(this.board);
        
        final KnownStates knownStates = new KnownStates();
        final List<int[]> finalStates = new ArrayList<int[]>();
        final boolean returnFirstSolution = (SOLUTION_MODE.ANY == this.optSolutionMode);
        System.out.println();
        System.out.println("options: " + this.getOptionsAsString());
        
        final int[] startState = this.board.getRobotPositions().clone();
        swapGoalLast(startState);
        System.out.printf("startState=" + this.stateString(startState) + "\n");
        
        //find the "finalStates" and save all intermediate states "knownStates"
        final long startGetStates = System.currentTimeMillis();
        final int goalRobot = ((true == this.isBoardGoalWildcard) ? -1 : this.boardNumRobots - 1);
        if (true == this.optAllowRebounds) {
            this.getFinalStates(startState, this.board.getGoalPosition(), goalRobot, returnFirstSolution, knownStates, finalStates);
        } else {
            this.getFinalStatesNoRebound(startState, this.board.getGoalPosition(), goalRobot, returnFirstSolution, knownStates, finalStates);
        }
        this.solutionStoredStates = knownStates.size();
        System.out.println("knownStates: " + knownStates.infoString());
        final long durationStates = System.currentTimeMillis() - startGetStates;
        System.out.println("time (Breadth-First-Search for finalStates) : " + (durationStates / 1000d) + " seconds");
        System.out.println("number of finalStates: " + finalStates.size());
        
        
        //find the paths from "startState" to the "finalStates".
        //build the arrays of moves for all paths.
        //dpending on the options, store the appropriate array of moves
        //in "this.solutionMoves" (THE RESULT).
        final long startGetPath = System.currentTimeMillis();
        int minRobots = Integer.MAX_VALUE, maxRobots = 0;
        for (int[] finalState : finalStates) {
            final List<int[]> statesPath = this.getStatesPath(finalState, knownStates);
            if (1 < statesPath.size()) {
                Solution tmpSolution = new Solution(this.board);
                swapGoalLast(statesPath.get(0));
                for (int i = 0;  i < statesPath.size() - 1;  ++i) {
                    swapGoalLast(statesPath.get(i+1));
                    tmpSolution.add(new Move(this.board, statesPath.get(i), statesPath.get(i+1), i));
                }
                final int thisRobots = tmpSolution.getRobotsMoved().size();
                System.out.printf("finalState=%s  robotsMoved=%d", this.stateString(finalState), Integer.valueOf(thisRobots));
                if (true == tmpSolution.isRebound()) {
                    System.out.print("  <- rebound");
                }
                if ((SOLUTION_MODE.ANY == this.optSolutionMode) && (0 == this.lastResultSolution.size())) {
                    System.out.print("  <- any");
                } else if ((SOLUTION_MODE.MINIMUM == this.optSolutionMode) && (thisRobots < minRobots)) {
                    minRobots = thisRobots;
                    System.out.print("  <- min");
                } else if ((SOLUTION_MODE.MAXIMUM == this.optSolutionMode) && (thisRobots > maxRobots)) {
                    maxRobots = thisRobots;
                    System.out.print("  <- max");
                } else {
                    tmpSolution = null;   //don't accept this solution
                }
                if (null != tmpSolution) {
                    this.lastResultSolution = tmpSolution;
                    System.out.print("  !!!");
                }
                System.out.println();
            }
        }
        final long durationPath = System.currentTimeMillis() - startGetPath;
        System.out.println("time (Depth-First-Search   for statePaths ) : " + (durationPath / 1000d) + " seconds");
        
        this.solutionMilliSeconds = System.currentTimeMillis() - startExecute;
        return this.lastResultSolution;
    }
    
    
    
    private void getFinalStates(
            final int[] startState,             //IN: initial state (positions of all robots)
            final int goalPosition,             //IN: position of goal
            final int goalRobot,                //IN: number of goal robot (-1 for wildcard)
            final boolean returnFirstSolution,  //IN: find only the first shortest solution (false = find all shortest solutions)
            final KnownStates knownStates,      //OUT: all known states
            final List<int[]> finalStates       //OUT: final states (goal robot has reached goal position)
            ) throws InterruptedException {
        int depth = knownStates.incrementDepth();
        assert 0 == depth : depth;
        knownStates.add(startState);
        final int[] tmpState = new int[startState.length];
        //is the starting position already on goal?
        if (-1 == goalRobot) {
            for (int pos : startState) { if (goalPosition == pos) { finalStates.add(startState.clone()); } }
        } else if (goalPosition == startState[goalRobot]) { finalStates.add(startState.clone()); }
        //breadth-first search
        while(true) {
            if (0 < finalStates.size()) { return; } //goal has been reached!
            depth = knownStates.incrementDepth();
            final KnownStates.Iterator iter = knownStates.iterator(depth - 1);
            System.out.println("... BFS working at depth="+depth+"   statesToExpand=" + iter.size());
            if (0 == iter.size()) { return; }       //goal NOT reachable!
            while (true == iter.next(tmpState)) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                //expand the current state: evaluate and store the successor states.
                for (int pos : tmpState) { this.expandRobotPositions[pos] = true; }
                for (int robo = 0;  robo < tmpState.length;  ++robo) {
                    final int oldRoboPos = tmpState[robo];
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
                        if (oldRoboPos != newRoboPos) {
                            //add the new state to the set of known states
                            //only if it's not already known.
                            tmpState[robo] = newRoboPos;   //temporarily!
                            final boolean stateHasBeenAdded = knownStates.add(tmpState);
                            if (true == stateHasBeenAdded) {
                                //check if the goal robot has reached the goal position.
                                if ((goalPosition == newRoboPos) && ((goalRobot == robo) || (-1 == goalRobot))) {
                                    finalStates.add(tmpState.clone());
                                }
                            }
                        }
                    }
                    tmpState[robo] = oldRoboPos;   //temporarily! reverted!
                }
                for (int pos : tmpState) { this.expandRobotPositions[pos] = false; }
                if (returnFirstSolution && (0 < finalStates.size())) { return; }    //goal has been reached!
            }
        }
    }
    
    
    
    private void getFinalStatesNoRebound(
            final int[] startState,             //IN: initial state (positions of all robots)
            final int goalPosition,             //IN: position of goal
            final int goalRobot,                //IN: number of goal robot (-1 for wildcard)
            final boolean returnFirstSolution,  //IN: find only the first shortest solution (false = find all shortest solutions)
            final KnownStates knownStates,      //OUT: all known states
            final List<int[]> finalStates       //OUT: final states (goal robot has reached goal position)
            ) throws InterruptedException {
        int depth = knownStates.incrementDepth();
        assert 0 == depth : depth;
        final int[] tmpDirs = new int[startState.length];
        for (int i = 0;  i < tmpDirs.length;  ++i) { tmpDirs[i] = 7; }  // 7 == not_yet_moved
        knownStates.add(startState, tmpDirs);
        final int[] tmpState = new int[startState.length];
        //is the starting position already on goal?
        if (-1 == goalRobot) {
            for (int pos : startState) { if (goalPosition == pos) { finalStates.add(startState.clone()); } }
        } else if (goalPosition == startState[goalRobot]) { finalStates.add(startState.clone()); }
        //breadth-first search
        while(true) {
            if (0 < finalStates.size()) { return; } //goal has been reached!
            depth = knownStates.incrementDepth();
            final KnownStates.Iterator iter = knownStates.iterator(depth - 1);
            System.out.println("... BFS working at depth="+depth+"   statesToExpand=" + iter.size());
            if (0 == iter.size()) { return; }       //goal NOT reachable!
            while (true == iter.next(tmpState, tmpDirs)) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                //expand the current state: evaluate and store the successor states.
                for (int pos : tmpState) { this.expandRobotPositions[pos] = true; }
                for (int robo = 0;  robo < tmpState.length;  ++robo) {
                    final int oldRoboPos = tmpState[robo];
                    final int oldRoboDir = tmpDirs[robo];
                    for (int dir = 0;  dir < 4;  ++dir) {
                        //don't allow rebound moves
                        if ((oldRoboDir != dir) && (oldRoboDir != ((dir + 2) & 3))) {
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
                            if (oldRoboPos != newRoboPos) {
                                //add the new state to the set of known states
                                //only if it's not already known.
                                tmpState[robo] = newRoboPos;    //temporarily!
                                tmpDirs[robo] = dir;            //temporarily!
                                final boolean stateHasBeenAdded = knownStates.add(tmpState, tmpDirs);
                                if (true == stateHasBeenAdded) {
                                    //check if the goal robot has reached the goal position.
                                    if ((goalPosition == newRoboPos) && ((goalRobot == robo) || (-1 == goalRobot))) {
                                        finalStates.add(tmpState.clone());
                                    }
                                }
                            }
                        }
                    }
                    tmpState[robo] = oldRoboPos;    //temporarily! reverted!
                    tmpDirs[robo] = oldRoboDir;     //temporarily! reverted!
                }
                for (int pos : tmpState) { this.expandRobotPositions[pos] = false; }
                if (returnFirstSolution && (0 < finalStates.size())) { return; }    //goal has been reached!
            }
        }
    }
    
    
    
    private List<int[]> getStatesPath(final int[] finalState, final KnownStates knownStates) throws InterruptedException {
        final List<int[]> result = new ArrayList<int[]>();
        final int depth = knownStates.depth();
        if (depth > 0) {
            final int[][] tmpStates = new int[depth][this.boardNumRobots];
            final boolean haveResult;
            if (true == this.optAllowRebounds) {
                haveResult = this.doPathDFS(finalState.clone(), knownStates, depth-1, result, tmpStates);
            } else {
                final int[] tmpDirs = new int[this.boardNumRobots];
                haveResult = this.doPathDFSNoRebound(finalState.clone(), knownStates, depth-1, result, tmpStates, tmpDirs);
            }
            if (true == haveResult) {
                result.add(finalState.clone());
            }
        }
        return result;
    }
    
    
    
    private boolean doPathDFS(final int[] thisState, final KnownStates knownStates, final int depth, final List<int[]> result, final int[][] tmpStates) throws InterruptedException {
        final KnownStates.Iterator iter = knownStates.iterator(depth);
        if (0 == depth) {
            iter.next(tmpStates[depth]);
            result.add(tmpStates[depth].clone());
            return true;
        } else {
            while (true == iter.next(tmpStates[depth])) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                //detect the number of moved robots between prevState and thisState.
                //store the position difference in diffPos if only one robot has moved.
                int diffPos = 0, prevPos = 0;
                for (int i = 0;  i < thisState.length;  ++i) {
                    if (tmpStates[depth][i] != thisState[i]) {
                        if (0 == diffPos) {
                            prevPos = tmpStates[depth][i];
                            diffPos = thisState[i] - prevPos;
                        } else {
                            diffPos = 0; break; //found more than one difference
                        }
                    }
                }
                //check if this position difference is a possible move along one row or column.
                if ((0 != diffPos) && ((Math.abs(diffPos) < this.board.width) || (0 == diffPos % this.board.width))) {
                    final int thisPos = prevPos + diffPos;
                    for (int pos : tmpStates[depth]) { this.expandRobotPositions[pos] = true; }
                    final int dir = this.board.getDirection(diffPos);
                    //check if the move would go though obstacles (walls or robots).
                    final int dirIncr = this.board.directionIncrement[dir];
                    while (0 == this.boardWalls[prevPos][dir]) {
                        prevPos += dirIncr;
                        if (this.expandRobotPositions[prevPos]) {
                            prevPos -= dirIncr;
                            break;
                        }
                    }
                    for (int pos : tmpStates[depth]) { this.expandRobotPositions[pos] = false; }
                    //follow the move to the previous level in the array of states. (recursion)
                    if (prevPos == thisPos) {
                        if (this.doPathDFS(tmpStates[depth], knownStates, depth-1, result, tmpStates)) {
                            result.add(tmpStates[depth].clone());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    
    
    private boolean doPathDFSNoRebound(final int[] thisState, final KnownStates knownStates, final int depth, final List<int[]> result, final int[][] tmpStates, final int[] tmpDirections) throws InterruptedException {
        final KnownStates.Iterator iter = knownStates.iterator(depth);
        if (0 == depth) {
            iter.next(tmpStates[depth]);
            result.add(tmpStates[depth].clone());
            return true;
        } else {
            while (true == iter.next(tmpStates[depth], tmpDirections)) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                //detect the number of moved robots between prevState and thisState.
                //store the position difference in diffPos if only one robot has moved.
                int diffPos = 0, prevPos = 0, tmpDir = 0;
                for (int i = 0;  i < thisState.length;  ++i) {
                    if (tmpStates[depth][i] != thisState[i]) {
                        if (0 == diffPos) {
                            prevPos = tmpStates[depth][i];
                            diffPos = thisState[i] - prevPos;
                            tmpDir  = tmpDirections[i];
                        } else {
                            diffPos = 0; break; //found more than one difference
                        }
                    }
                }
                //check if this position difference is a possible move along one row or column.
                if ((0 != diffPos) && ((Math.abs(diffPos) < this.board.width) || (0 == diffPos % this.board.width))) {
                    final int dir = this.board.getDirection(diffPos);
                    //don't allow rebound moves
                    if ((tmpDir != dir) && (tmpDir != ((dir + 2) & 3))) {
                        final int thisPos = prevPos + diffPos;
                        for (int pos : tmpStates[depth]) { this.expandRobotPositions[pos] = true; }
                        //check if the move would go though obstacles (walls or robots).
                        final int dirIncr = this.board.directionIncrement[dir];
                        while (0 == this.boardWalls[prevPos][dir]) {
                            prevPos += dirIncr;
                            if (this.expandRobotPositions[prevPos]) {
                                prevPos -= dirIncr;
                                break;
                            }
                        }
                        for (int pos : tmpStates[depth]) { this.expandRobotPositions[pos] = false; }
                        //follow the move to the previous level in the array of states. (recursion)
                        if (prevPos == thisPos) {
                            if (this.doPathDFSNoRebound(tmpStates[depth], knownStates, depth-1, result, tmpStates, tmpDirections)) {
                                result.add(tmpStates[depth].clone());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    
    
    private String stateString(final int[] state) {
        final Formatter formatter = new Formatter();
        this.swapGoalLast(state);
        for (int i = 0;  i < state.length;  i++) {
            formatter.format("%02x", Integer.valueOf(state[i]));
        }
        this.swapGoalLast(state);
        return "0x" + formatter.out().toString();
    }
    
    private void swapGoalLast(final int[] state) {
        //swap goal robot and last robot (if goal is not wildcard)
        if (false == this.isBoardGoalWildcard) {
            final int tmp = state[state.length - 1];
            state[state.length - 1] = state[this.board.getGoalRobot()];
            state[this.board.getGoalRobot()] = tmp;
        }
    }
    
    
    
    public void setOptionSolutionMode(SOLUTION_MODE mode) {
        this.optSolutionMode = mode;
    }
    
    public void setOptionAllowRebounds(boolean allowRebounds) {
        this.optAllowRebounds = allowRebounds;
    }
    
    public String getOptionsAsString() {
        return this.optSolutionMode.toString() + " number of robots moved; "
                + (this.optAllowRebounds ? "with" : "no") + " rebound moves";
    }
    
    public long getSolutionMilliSeconds() {
        return this.solutionMilliSeconds;
    }
    
    public int getSolutionStoredStates() {
        return this.solutionStoredStates;
    }
    
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("storedStates=").append(this.solutionStoredStates);
        s.append(", time=").append(this.solutionMilliSeconds / 1000d).append(" seconds");
        return s.toString();
    }
    
    
    
    private class KnownStates {
        private final AllKeys allKeys;
        private final AllStates allStates;
        private final AllDirections allDirections;
        private int currentDepth = -1;
        
        public KnownStates() {
            this.allKeys = ((true == isBoardStateInt32) ? new AllKeysInt() : new AllKeysLong());
            this.allStates = new AllStatesByte();   //TODO add AllStatesShort to support board sizes > 16*16
            this.allDirections = new AllDirectionsShort();
        }
        
        //store the unique keys of all known states
        private abstract class AllKeys {
            protected final SparseBitSet stateKeys;
            public abstract boolean add(final int[] state);
            public AllKeys() {
                long maxNumStates = boardSizeBitMask;
                for (int i = 1;  i < boardNumRobots;  ++i) {
                    maxNumStates = (maxNumStates << boardSizeNumBits) | boardSizeBitMask;
                }
                final int nodeSize = (int)Math.ceil(Math.pow(maxNumStates / 64.0d, 1.0d/3.0d));
                this.stateKeys = new SparseBitSet(nodeSize);
            }
        }
        //store the unique keys of all known states in 32-bit ints
        //supports up to 4 robots with a board size of 256 (16*16)
        private final class AllKeysInt extends AllKeys {
            private final KeyMakerInt keyMaker = createKeyMakerInt();
            public AllKeysInt() {
                super();
            }
            @Override
            public final boolean add(final int[] state) {
                final long key = 0x00000000ffffffffL & this.keyMaker.run(state);
                return this.stateKeys.add(key);
            }
        }
        //store the unique keys of all known states in 32-bit ints
        //fastest version = uses full 4 gigabits array.
//        private final class AllKeysIntBitArray extends AllKeys {
//            private final int[] BIT_POS = {
//                    0x00000001, 0x00000002, 0x00000004, 0x00000008, 0x00000010, 0x00000020, 0x00000040, 0x00000080,
//                    0x00000100, 0x00000200, 0x00000400, 0x00000800, 0x00001000, 0x00002000, 0x00004000, 0x00008000,
//                    0x00010000, 0x00020000, 0x00040000, 0x00080000, 0x00100000, 0x00200000, 0x00400000, 0x00800000,
//                    0x01000000, 0x02000000, 0x04000000, 0x08000000, 0x10000000, 0x20000000, 0x40000000, 0x80000000
//            };
//            private final KeyMakerInt keyMaker = createKeyMakerInt();
//            private final int[] allBits = new int[(4096 / 32) * 1024 * 1024];   // 512 megabytes = 4 gigabits (2**32)
//            public AllKeysIntBitArray() {
//                super();
//            }
//            @Override
//            public final boolean add(final int[] state) {
//                final int key = this.keyMaker.run(state);
//                final int index = key >>> 5;
//                final int oldBits = this.allBits[index];
//                final int newBits = oldBits | (this.BIT_POS[key & 31]);
//                final boolean keyHasBeenAdded = (oldBits != newBits);
//                if (true == keyHasBeenAdded) {
//                    this.allBits[index] = newBits;
//                }
//                return keyHasBeenAdded;
//            }
//        }
        //store the unique keys of all known states in 64-bit longs
        //supports more than 4 robots and/or board sizes larger than 256
        private final class AllKeysLong extends AllKeys {
            private final KeyMakerLong keyMaker = createKeyMakerLong();
            public AllKeysLong() {
                super();
            }
            @Override
            public final boolean add(final int[] state) {
                final long key = this.keyMaker.run(state);
                return this.stateKeys.add(key);
            }
        }
        
        //store all known states in a way that allows them to be retrieved later
        private abstract class AllStates {
            protected final int ARRAY_SIZE = 60 * 100 * 100; //size of each array in the list of arrays. lcm(1,2,3,4,5) = 60
            protected int numStates = 0;                     //number of states that are stored
            protected int addArray = -1;                     //add: index of the current array in list "allStates"
            protected int addOffset = this.ARRAY_SIZE;       //add: current index inside the current array
            protected final List<Integer> depthBegin = new ArrayList<Integer>();    //iterateStart
            public int size() {
                return this.numStates;
            }
            public final void incrementDepth() {
                this.depthBegin.add(Integer.valueOf(this.numStates));
            }
            public abstract void add(final int[] state);
            public abstract Iterator iterator(final int depth);
            public abstract class Iterator {
                protected final int iterStart, iterEnd;
                protected int iterCurrent, iterArray, iterOffset;
                protected Iterator(final int depth) {
                    this.iterStart = depthBegin.get(depth).intValue();
                    this.iterEnd = ((depth + 1 < depthBegin.size()) ? depthBegin.get(depth + 1).intValue() : numStates);
                    this.iterCurrent = this.iterStart;
                    this.iterArray = (this.iterStart * boardNumRobots) / ARRAY_SIZE;
                    this.iterOffset = (this.iterStart * boardNumRobots) % ARRAY_SIZE;
                }
                public int size() {
                    return this.iterEnd - this.iterStart;
                }
                public abstract boolean next(final int[] resultState);
            }
        }
        //store all known states in a list of byte arrays
        //supports board sizes up to 256 (16*16)
        private final class AllStatesByte extends AllStates {
            private final List<byte[]> allStatesListOfByteArrays = new ArrayList<byte[]>();
            @Override
            public final void add(final int[] state) {
                assert 8 >= boardSizeNumBits : boardSizeNumBits;
                //if necessary, allocate an additional array and append it to the list
                if (this.addOffset >= this.ARRAY_SIZE) {
                    this.addArray++;
                    this.allStatesListOfByteArrays.add(new byte[this.ARRAY_SIZE]);
                    this.addOffset = 0;
                }
                //append state to the current array in list
                final byte[] allStatesArray = this.allStatesListOfByteArrays.get(this.addArray);
                for (int pos : state) {
                    allStatesArray[this.addOffset++] = (byte)pos;
                }
                this.numStates++;
            }
            private final class AllStatesByteIterator extends AllStates.Iterator {
                public AllStatesByteIterator(final int depth) {
                    super(depth);
                }
                @Override
                public boolean next(final int[] resultState) {
                  final boolean hasNext = (this.iterEnd > this.iterCurrent);
                  if (true == hasNext) {
                      //if necessary, switch to next array in the list
                      if (this.iterOffset >= ARRAY_SIZE) {
                          this.iterArray++;
                          this.iterOffset = 0;
                      }
                      //retrieve the next state
                      final byte[] allStatesArray = allStatesListOfByteArrays.get(this.iterArray);
                      for (int i = 0;  i < resultState.length;  i++) {
                          resultState[i] = (boardSizeBitMask & allStatesArray[this.iterOffset++]);
                      }
                      this.iterCurrent++;
                  }
                  return hasNext;
                }
            }
            @Override
            public AllStates.Iterator iterator(final int depth) {
                return new AllStatesByteIterator(depth);
            }
        }
        
        //store all directions belonging to the known states
        //(implementation is copy/paste from AllStates with some adaptions)
        private abstract class AllDirections {
            protected final int ARRAY_SIZE = 10 * 100 * 100;
            protected int numDirs = 0;
            protected int addArray = -1, addOffset = this.ARRAY_SIZE;
            protected final List<Integer> depthBegin = new ArrayList<Integer>();
            public final void incrementDepth() {
                this.depthBegin.add(Integer.valueOf(this.numDirs));
            }
            public abstract void add(final int[] dirs);
            public abstract Iterator iterator(final int depth);
            public abstract class Iterator {
                protected final int iterStart, iterEnd;
                protected int iterCurrent, iterArray, iterOffset;
                protected Iterator(final int depth) {
                    this.iterStart = depthBegin.get(depth).intValue();
                    this.iterEnd = ((depth + 1 < depthBegin.size()) ? depthBegin.get(depth + 1).intValue() : numDirs);
                    this.iterCurrent = this.iterStart;
                    this.iterArray = this.iterStart / ARRAY_SIZE;
                    this.iterOffset = this.iterStart % ARRAY_SIZE;
                }
                public int size() {
                    return this.iterEnd - this.iterStart;
                }
                public abstract boolean next(final int[] resultState);
            }
        }
        //store all directions belonging to the known states in a list of short arrays
        //supports up to 5 robots (with 3 bits per direction)
        private final class AllDirectionsShort extends AllDirections {
            private final List<short[]> allDirsListOfShortArrays = new ArrayList<short[]>();
            @Override
            public final void add(final int[] dirs) {
                assert dirs.length <= 5 : dirs.length;
                //if necessary, allocate an additional array and append it to the list
                if (this.addOffset >= this.ARRAY_SIZE) {
                    this.addArray++;
                    this.allDirsListOfShortArrays.add(new short[this.ARRAY_SIZE]);
                    this.addOffset = 0;
                }
                //append "dirs" to the current array in list
                int packed = dirs[0];
                for (int i = 1;  i < dirs.length;  ++i) {
                    packed = (packed << 3) | dirs[i];
                }
                this.allDirsListOfShortArrays.get(this.addArray)[this.addOffset++] = (short)packed;
                this.numDirs++;
            }
            private final class AllDirectionsShortIterator extends AllDirections.Iterator {
                public AllDirectionsShortIterator(final int depth) {
                    super(depth);
                }
                @Override
                public boolean next(final int[] resultDirs) {
                    assert resultDirs.length <= 5 : resultDirs.length;
                    final boolean hasNext = (this.iterEnd > this.iterCurrent);
                    if (true == hasNext) {
                        if (this.iterOffset >= ARRAY_SIZE) {
                            this.iterArray++;
                            this.iterOffset = 0;
                        }
                        int packed = allDirsListOfShortArrays.get(this.iterArray)[this.iterOffset++];
                        for (int i = resultDirs.length - 1;  i > 0;  --i) {
                            resultDirs[i] = (7 & packed);
                            packed >>>= 3;
                        }
                        resultDirs[0] = (7 & packed);
                        this.iterCurrent++;
                    }
                    return hasNext;
                }
            }
            @Override
            public Iterator iterator(final int depth) {
                return new AllDirectionsShortIterator(depth);
            }
        }
        
        public final class Iterator {
            private final AllStates.Iterator allStatesIter;
            private final AllDirections.Iterator allDirsIter;
            public Iterator(final int depth) {
                this.allStatesIter = allStates.iterator(depth);
                this.allDirsIter = allDirections.iterator(depth);
            }
            public int size() {
                return this.allStatesIter.size();
            }
            public boolean next(final int[] resultState) {
                assert resultState.length == boardNumRobots : resultState.length;
                return this.allStatesIter.next(resultState);
            }
            public boolean next(final int[] resultState, final int[] resultDirs) {
                assert this.allStatesIter.size() == this.allDirsIter.size();
                assert resultState.length == boardNumRobots : resultState.length;
                assert resultDirs.length == boardNumRobots : resultDirs.length;
                this.allDirsIter.next(resultDirs);
                return this.allStatesIter.next(resultState);
            }
        }
        
        public final int incrementDepth() {
            this.currentDepth++;
            this.allStates.incrementDepth();
            this.allDirections.incrementDepth();
            return this.currentDepth;
        }
        
        public final boolean add(final int[] state) {
            assert state.length == boardNumRobots : state.length;
            final boolean stateHasBeenAdded = this.allKeys.add(state);
            if (true == stateHasBeenAdded) {
                this.allStates.add(state);
            }
            return stateHasBeenAdded;
        }
        public final boolean add(final int[] state, final int[] dirs) {
            assert state.length == boardNumRobots : state.length;
            assert dirs.length == boardNumRobots : dirs.length;
            final boolean stateHasBeenAdded = this.allKeys.add(state);
            if (true == stateHasBeenAdded) {
                this.allStates.add(state);
                this.allDirections.add(dirs);
            }
            return stateHasBeenAdded;
        }
        
        public Iterator iterator(final int depth) {
            return new Iterator(depth);
        }
        public final int size() {
            return this.allStates.size();
        }
        public final int depth() {
            return this.currentDepth;
        }
        public final String infoString() {
            return "size=" + this.allStates.size() + " depth=" + this.currentDepth;
        }
    }
    
    
    
    private final KeyMakerInt createKeyMakerInt() {
        final KeyMakerInt keyMaker;
        switch (boardNumRobots) {
        case 1:  keyMaker = new KeyMakerInt1(); break;
        case 2:  keyMaker = new KeyMakerInt2(); break;
        case 3:  keyMaker = (isBoardGoalWildcard ? new KeyMakerInt3nosort() : new KeyMakerInt3sort()); break;
        case 4:  keyMaker = (isBoardGoalWildcard ? new KeyMakerInt4nosort() : new KeyMakerInt4sort()); break;
        default: keyMaker = new KeyMakerIntAll();
        }
        return keyMaker;
    }
    private abstract class KeyMakerInt {
        protected final int s1 = boardSizeNumBits,  s2 = s1 * 2;
        public abstract int run(final int[] state);
    }
    private final class KeyMakerIntAll extends KeyMakerInt {
        private final int[] tmpState = new int[boardNumRobots];
        @Override
        public final int run(final int[] state) {
            //copy and sort state
            System.arraycopy(state, 0, this.tmpState, 0, state.length);
            if (false == isBoardGoalWildcard) {
                Arrays.sort(this.tmpState, 0, this.tmpState.length - 1);    //don't sort the last element
//                //calculate deltas - it doesn't reduce memory usage in SparseBitSet ?!
//                for (int i = 1, prev = this.tmpState[0];  i < this.tmpState.length - 1;  ++i) {
//                    final int tmp = this.tmpState[i];
//                    this.tmpState[i] = tmp - prev;
//                    prev = tmp;
//                }
            }
            //pack state into a single int value
            int result = this.tmpState[0];
            for (int i = 1;  i < boardNumRobots;  ++i) {
                result = (result << s1) | this.tmpState[i];
            }
            return result;
        }
    }
    private final class KeyMakerInt1 extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 1 == state.length : state.length;
            return state[0];
        }
    }
    private final class KeyMakerInt2 extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 2 == state.length : state.length;
            return (state[0] << s1) | state[1];
        }
    }
    private final class KeyMakerInt3sort extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 3 == state.length : state.length;
            int result = state[0];
            if (result < state[1]) {
                result = (result << s1) | state[1];
            } else {
                result = (state[1] << s1) | result;
            }
            return (result << s1) | state[2];
        }
    }
    private final class KeyMakerInt3nosort extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 3 == state.length : state.length;
            int result = state[0] << s1;
            return (result << s1) | (state[1] << s1) | state[2];
        }
    }
    private final class KeyMakerInt4sort extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 4 == state.length : state.length;
            final int a = state[0],  b = state[1],  c = state[2];
            final int result;
            if (a < b) {
                if (a < c) {
                    if (b < c) { result = (a << s2) | (b << s1) | c;
                    } else {     result = (a << s2) | (c << s1) | b; }
                } else {         result = (c << s2) | (a << s1) | b; }
            } else {
                if (b < c) {
                    if (a < c) { result = (b << s2) | (a << s1) | c;
                    } else {     result = (b << s2) | (c << s1) | a; }
                } else {         result = (c << s2) | (b << s1) | a; }
            }
            return (result << s1) | state[3];
        }
    }
    private final class KeyMakerInt4nosort extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 4 == state.length : state.length;
            final int result = (state[0] << s2) | (state[1] << s1) | state[2];
            return (result << s1) | state[3];
        }
    }
    
    
    
    private final KeyMakerLong createKeyMakerLong() {
        final KeyMakerLong keyMaker;
        switch (boardNumRobots) {
        case 5:  keyMaker = (isBoardGoalWildcard ? new KeyMakerLong5nosort() : new KeyMakerLong5sort()); break;
        default: keyMaker = new KeyMakerLongAll();
        }
        return keyMaker;
    }
    private abstract class KeyMakerLong {
        protected final int s1 = boardSizeNumBits,  s2 = s1 * 2,  s3 = s1 * 3;
        public abstract long run(final int[] state);
    }
    private class KeyMakerLongAll extends KeyMakerLong {
        private final int[] tmpState = new int[boardNumRobots];
        @Override
        public final long run(final int[] state) {
            //copy and sort state
            System.arraycopy(state, 0, this.tmpState, 0, state.length);
            if (false == isBoardGoalWildcard) {
                Arrays.sort(this.tmpState, 0, this.tmpState.length - 1);    //don't sort the last element
//                //calculate deltas - it doesn't reduce memory usage in SparseBitSet ?!
//                for (int i = 1, prev = this.tmpState[0];  i < this.tmpState.length - 1;  ++i) {
//                    final int tmp = this.tmpState[i];
//                    this.tmpState[i] = tmp - prev;
//                    prev = tmp;
//                }
            }
            //pack state into a single long value
            long result = this.tmpState[0];
            for (int i = 1;  i < boardNumRobots;  ++i) {
                result = (result << s1) | this.tmpState[i];
            }
            return result;
        }
    }
    private class KeyMakerLong5sort extends KeyMakerLong {
        @Override
        public final long run(final int[] state) {
            assert 5 == state.length : state.length;
            final long a = state[0],  b = state[1],  c = state[2],  d = state[3];
            final long result;
            if (a <= b) {
                if (c <= d) {
                    if (a <= c) {
                        if (b <= d) {
                            if (b <= c) { result = (a << s3) | (b << s2) | (c << s1) | d;
                            } else {      result = (a << s3) | (c << s2) | (b << s1) | d; }
                        } else {          result = (a << s3) | (c << s2) | (d << s1) | b; }
                    } else {
                        if (b <= d) {     result = (c << s3) | (a << s2) | (b << s1) | d;
                        } else {
                            if (a <= d) { result = (c << s3) | (a << s2) | (d << s1) | b;
                            } else {      result = (c << s3) | (d << s2) | (a << s1) | b; }
                        }
                    }
                } else {
                    if (a <= d) {
                        if (b <= c) {
                            if (b <= d) { result = (a << s3) | (b << s2) | (d << s1) | c;
                            } else {      result = (a << s3) | (d << s2) | (b << s1) | c; }
                        } else {          result = (a << s3) | (d << s2) | (c << s1) | b; }
                    } else {
                        if (b <= c) {     result = (d << s3) | (a << s2) | (b << s1) | c;
                        } else {
                            if (a <= c) { result = (d << s3) | (a << s2) | (c << s1) | b;
                            } else {      result = (d << s3) | (c << s2) | (a << s1) | b; }
                        }
                    }
                }
            } else {
                if (c <= d) {
                    if (b <= c) {
                        if (a <= d) {
                            if (a <= c) { result = (b << s3) | (a << s2) | (c << s1) | d;
                            } else {      result = (b << s3) | (c << s2) | (a << s1) | d; }
                        } else {          result = (b << s3) | (c << s2) | (d << s1) | a; }
                    } else {
                        if (a <= d) {     result = (c << s3) | (b << s2) | (a << s1) | d;
                        } else {
                            if (b <= d) { result = (c << s3) | (b << s2) | (d << s1) | a;
                            } else {      result = (c << s3) | (d << s2) | (b << s1) | a; }
                        }
                    }
                } else {
                    if (b <= d) {
                        if (a <= c) {
                            if (a <= d) { result = (b << s3) | (a << s2) | (d << s1) | c;
                            } else {      result = (b << s3) | (d << s2) | (a << s1) | c; }
                        } else {          result = (b << s3) | (d << s2) | (c << s1) | a; }
                    } else {
                        if (a <= c) {     result = (d << s3) | (b << s2) | (a << s1) | c;
                        } else {
                            if (b <= c) { result = (d << s3) | (b << s2) | (c << s1) | a;
                            } else {      result = (d << s3) | (c << s2) | (b << s1) | a; }
                        }
                    }
                }
            }
            return (result << s1) | state[4];
        }
    }
    private class KeyMakerLong5nosort extends KeyMakerLong {
        @Override
        public final long run(final int[] state) {
            assert 5 == state.length : state.length;
            final long result = (state[0] << s3) | (state[1] << s2) | (state[2] << s1) | state[3];
            return (result << s1) | state[4];
        }
    }
    
    
}

