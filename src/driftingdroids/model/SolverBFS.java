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
import java.util.List;

import org.enerj.core.SparseBitSet;



public class SolverBFS extends AbstractSolver {
    
    
    
    public SolverBFS(final Board board) {
        super(board);
    }
    
    
    
    @Override
    public int execute() throws InterruptedException {
        final long startExecute = System.currentTimeMillis();
        this.solutionMoves = null;  //THE RESULT
        this.solutionMoveIndex = 0;
        
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
                Move[] moves = new Move[statesPath.size() - 1];
                swapGoalLast(statesPath.get(0));
                for (int i = 0;  i < statesPath.size() - 1;  ++i) {
                    swapGoalLast(statesPath.get(i+1));
                    moves[i] = new Move(this.board, statesPath.get(i), statesPath.get(i+1), i);
                }
                final int thisRobots = this.getSolutionRobotsMoved(moves).size();
                System.out.printf("finalState=%s  robotsMoved=%d", this.stateString(finalState), Integer.valueOf(thisRobots));
                if (true == this.isSolutionRebound(moves, null)) {
                    System.out.print("  <- rebound");
                }
                if ((SOLUTION_MODE.ANY == this.optSolutionMode) && (null == this.solutionMoves)) {
                    System.out.print("  <- any");
                } else if ((SOLUTION_MODE.MINIMUM == this.optSolutionMode) && (thisRobots < minRobots)) {
                    minRobots = thisRobots;
                    System.out.print("  <- min");
                } else if ((SOLUTION_MODE.MAXIMUM == this.optSolutionMode) && (thisRobots > maxRobots)) {
                    maxRobots = thisRobots;
                    System.out.print("  <- max");
                } else {
                    moves = null;   //don't accept this solution
                }
                if (null != moves) {
                    this.solutionMoves = moves;
                    System.out.print("  !!!");
                }
                System.out.println();
            }
        }
        final long durationPath = System.currentTimeMillis() - startGetPath;
        System.out.println("time (Depth-First-Search   for statePaths ) : " + (durationPath / 1000d) + " seconds");
        
        this.solutionMilliSeconds = System.currentTimeMillis() - startExecute;
        return this.getSolutionSize();
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
    
    
    
    private class KnownStates {
        private final AllStateKeys allStateKeys;
        private final AllStates allStates;
        private final AllDirections allDirections;
        private int currentDepth = -1;
        
        public KnownStates() {
            this.allStateKeys = ((true == isBoardStateInt32) ? new KnownStateKeysInt() : new KnownStateKeysLong());
            this.allStates = new AllStatesByte();   //TODO add AllStatesShort to support board sizes > 16*16
            this.allDirections = new AllDirectionsShort();
        }
        
        //store the unique keys of all known states
        private abstract class AllStateKeys {
            protected final SparseBitSet stateKeys;
            public abstract boolean add(final int[] state);
            public AllStateKeys() {
                long maxNumStates = boardSizeBitMask;
                for (int i = 1;  i < boardNumRobots;  ++i) {
                    maxNumStates = (maxNumStates << boardSizeNumBits) | boardSizeBitMask;
                }
                final int nodeSize = (int)Math.ceil(Math.pow(maxNumStates / 64.0d, 1.0d/3.0d));
                this.stateKeys = new SparseBitSet(nodeSize);
            }
            public int size() {
                return 0;
            }
            public int size2() {
                return 0;
            }
        }
        //store the unique keys of all known states in 32-bit ints
        //supports up to 4 robots with a board size of 256 (16*16)
        private final class KnownStateKeysInt extends AllStateKeys {
            private final KeyMakerInt keyMaker = createKeyMakerInt();
            public KnownStateKeysInt() {
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
        private final class KnownStateKeysIntBitArray extends AllStateKeys {
            private final int[] BIT_POS = {
                    0x00000001, 0x00000002, 0x00000004, 0x00000008, 0x00000010, 0x00000020, 0x00000040, 0x00000080,
                    0x00000100, 0x00000200, 0x00000400, 0x00000800, 0x00001000, 0x00002000, 0x00004000, 0x00008000,
                    0x00010000, 0x00020000, 0x00040000, 0x00080000, 0x00100000, 0x00200000, 0x00400000, 0x00800000,
                    0x01000000, 0x02000000, 0x04000000, 0x08000000, 0x10000000, 0x20000000, 0x40000000, 0x80000000
            };
            private final KeyMakerInt keyMaker = createKeyMakerInt();
            private final int[] allBits = new int[(4096 / 32) * 1024 * 1024];   // 512 megabytes = 4 gigabits (2**32)
            public KnownStateKeysIntBitArray() {
                super();
            }
            @Override
            public final boolean add(final int[] state) {
                final int key = this.keyMaker.run(state);
                final int index = key >>> 5;
                final int oldBits = this.allBits[index];
                final int newBits = oldBits | (this.BIT_POS[key & 31]);
                final boolean keyHasBeenAdded = (oldBits != newBits);
                if (true == keyHasBeenAdded) {
                    this.allBits[index] = newBits;
                }
                return keyHasBeenAdded;
            }
        }
        //store the unique keys of all known states in 64-bit longs
        //supports more than 4 robots and/or board sizes larger than 256
        private final class KnownStateKeysLong extends AllStateKeys {
            private final KeyMakerLong keyMaker = createKeyMakerLong();
            public KnownStateKeysLong() {
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
            final boolean stateHasBeenAdded = this.allStateKeys.add(state);
            if (true == stateHasBeenAdded) {
                this.allStates.add(state);
            }
            return stateHasBeenAdded;
        }
        public final boolean add(final int[] state, final int[] dirs) {
            assert state.length == boardNumRobots : state.length;
            assert dirs.length == boardNumRobots : dirs.length;
            final boolean stateHasBeenAdded = this.allStateKeys.add(state);
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
            return "size=" + this.allStates.size() + " depth=" + this.currentDepth
                + " keys1=" + this.allStateKeys.size() + " keys2=" + this.allStateKeys.size2();
        }
    }
    
}

