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




public class SolverBFS extends Solver {
    private final boolean[] expandRobotPositions;

    protected SolverBFS(final Board board) {
        super(board);
        this.expandRobotPositions = new boolean[this.board.size];
        Arrays.fill(this.expandRobotPositions, false);
    }
    
    
    @Override
    public List<Solution> execute() throws InterruptedException {
        final long startExecute = System.nanoTime();
        this.lastResultSolutions = new ArrayList<Solution>();
        
        System.out.println("***** " + this.getClass().getSimpleName() + " *****");
        System.out.println("options: " + this.getOptionsAsString());
        
        final KnownStates knownStates = new KnownStates();
        final List<int[]> finalStates = new ArrayList<int[]>();
        final int[] startState = this.board.getRobotPositions().clone();
        swapGoalLast(startState);   //goal robot is always the last one.
        System.out.println("startState=" + this.stateString(startState));
        
        //find the "finalStates" and save all intermediate states in "knownStates"
        final long startGetStates = System.nanoTime();
        if (true == this.optAllowRebounds) {
            this.getFinalStates(startState, this.board.getGoal().position, this.isBoardGoalWildcard, knownStates, finalStates);
        } else {
            this.getFinalStatesNoRebound(startState, this.board.getGoal().position, this.isBoardGoalWildcard, knownStates, finalStates);
        }
        this.solutionStoredStates = knownStates.size();
        System.out.println("knownStates: " + knownStates.infoString());
        final long durationStates = (System.nanoTime() - startGetStates) / 1000000L;
        System.out.println("time (Breadth-First-Search for finalStates) : " + (durationStates / 1000d) + " seconds");
        System.out.println("number of finalStates: " + finalStates.size());
        System.out.println(knownStates.megaBytesAllocated());
        
        
        //find the paths from "startState" to the "finalStates".
        //build the Solutions and store them in list "this.lastResultSolutions" (THE RESULT).
        //depending on the options, this list is then sorted in natural order (MINIMUM)
        //or reverse natural order (MAXIMUM), so that the preferred solution is always
        //placed at list index 0.
        final long startGetPath = System.nanoTime();
        for (int[] finalState : finalStates) {
            final List<int[]> statesPath = this.getStatesPath(finalState, knownStates);
            if (1 < statesPath.size()) {
                Solution tmpSolution = new Solution(this.board);
                swapGoalLast(statesPath.get(0));
                for (int i = 0;  i < statesPath.size() - 1;  ++i) {
                    swapGoalLast(statesPath.get(i+1));
                    tmpSolution.add(new Move(this.board, statesPath.get(i), statesPath.get(i+1), i));
                }
                System.out.printf("finalState=%s  solution=%s", this.stateString(finalState), tmpSolution.toString());
                if (true == tmpSolution.isRebound()) {
                    System.out.print("  <- rebound");
                }
                this.lastResultSolutions.add(tmpSolution);
                System.out.println();
            }
        }
        
        this.sortSolutions();
        
        final long durationPath = (System.nanoTime() - startGetPath) / 1000000L;
        System.out.println("time (Depth-First-Search   for statePaths ) : " + (durationPath / 1000d) + " seconds");
        
        this.solutionMilliSeconds = (System.nanoTime() - startExecute) / 1000000L;
        return this.lastResultSolutions;
    }
    
    
    
    private void getFinalStates(
            final int[] startState,             //IN: initial state (positions of all robots)
            final int goalPosition,             //IN: position of goal
            final boolean isWildcardGoal,       //IN: is it the wildcard goal (any robot)
            final KnownStates knownStates,      //OUT: all known states
            final List<int[]> finalStates       //OUT: final states (goal robot has reached goal position)
            ) throws InterruptedException {
        int depth = knownStates.incrementDepth();
        assert 0 == depth : depth;
        knownStates.addKey(startState);
        knownStates.addState(startState);
        final int[] tmpState = new int[startState.length];
        final int robo1 = tmpState.length - 1;  //goal robot is always the last one.
        //is the starting position already on goal?
        if (true == isWildcardGoal) {
            for (int pos : startState) { if (goalPosition == pos) { finalStates.add(startState.clone()); } }
        } else if (goalPosition == startState[robo1]) { finalStates.add(startState.clone()); }
        //breadth-first search
        int prevSize = 0;
        boolean foundGoal = false;
        while(true) {
            if (0 < finalStates.size()) { return; } //goal has been reached!
            depth = knownStates.incrementDepth();
            KnownStates.Iterator iter = knownStates.iterator(depth - 1);
            final double thisPrevSizes = (0 == iter.size() ? 0.0 : (double)prevSize / iter.size());
            System.out.println("... BFS working at depth="+depth+"   statesToExpand=" + iter.size() + "   prev/thisStates=" + Math.round(thisPrevSizes*1000d)/1000d);
            if (0 == iter.size()) { return; }       //goal NOT reachable!
            prevSize += iter.size();
            //first pass: move goal robot, only.
            while (true == iter.next(tmpState)) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                for (int pos : tmpState) { this.expandRobotPositions[pos] = true; }
                final int oldRoboPos = tmpState[robo1];
                int dir = -1;
                for (int dirIncr : this.board.directionIncrement) {
                    ++dir;
                    int newRoboPos = oldRoboPos;
                    final boolean[] walls = this.boardWalls[dir];
                    while (false == walls[newRoboPos]) {                //move the robot until it reaches a wall or another robot.
                        newRoboPos += dirIncr;                          //NOTE: we rely on the fact that all boards are surrounded
                        if (this.expandRobotPositions[newRoboPos]) {    //by outer walls. without the outer walls we would need
                            newRoboPos -= dirIncr;                      //some additional boundary checking here.
                            break;
                        }
                    }
                    if (oldRoboPos != newRoboPos) {
                        tmpState[robo1] = newRoboPos;
                        //if we have already found a finalState then this is the last BFS pass.
                        //and we only need to store the additional finalStates but not all the "misses".
                        if ((false == foundGoal) || (goalPosition == newRoboPos)) {
                            if (true == knownStates.addKey(tmpState)) {
                                knownStates.addState(tmpState);
                                if (goalPosition == newRoboPos) {
                                    finalStates.add(tmpState.clone());  //goal robot has reached the goal position.
                                    foundGoal = true;
                                }
                            }
                        }
                    }
                }
                tmpState[robo1] = oldRoboPos;
                for (int pos : tmpState) { this.expandRobotPositions[pos] = false; }
            }
            if ((0 < finalStates.size()) && (false == isWildcardGoal)) { return; }  //goal has been reached!
            //second pass: move the other (non-goal) robots.
            iter = knownStates.iterator(depth - 1);
            while (true == iter.next(tmpState)) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                for (int pos : tmpState) { this.expandRobotPositions[pos] = true; }
                for (int robo2 = 0;  robo2 < robo1;  ++robo2) {
                    final int oldRoboPos = tmpState[robo2];
                    int dir = -1;
                    for (int dirIncr : this.board.directionIncrement) {
                        ++dir;
                        int newRoboPos = oldRoboPos;
                        final boolean[] walls = this.boardWalls[dir];
                        while (false == walls[newRoboPos]) {                //move the robot until it reaches a wall or another robot.
                            newRoboPos += dirIncr;                          //NOTE: we rely on the fact that all boards are surrounded
                            if (this.expandRobotPositions[newRoboPos]) {    //by outer walls. without the outer walls we would need
                                newRoboPos -= dirIncr;                      //some additional boundary checking here.
                                break;
                            }
                        }
                        if (oldRoboPos != newRoboPos) {
                            tmpState[robo2] = newRoboPos;
                            //if we have already found a finalState then this is the last BFS pass.
                            //and we only need to store the additional finalStates but not all the "misses".
                            if ((false == foundGoal) || ((true == isWildcardGoal) && (goalPosition == newRoboPos))) {
                                if (true == knownStates.addKey(tmpState)) {
                                    knownStates.addState(tmpState);
                                    //in this second pass, we can reach a wildcard goal, only.
                                    if ((true == isWildcardGoal) && (goalPosition == newRoboPos)) {
                                        finalStates.add(tmpState.clone());  //goal robot has reached the goal position.
                                        foundGoal = true;
                                    }
                                }
                            }
                        }
                    }
                    tmpState[robo2] = oldRoboPos;
                }
                for (int pos : tmpState) { this.expandRobotPositions[pos] = false; }
            }
        }
    }
    
    
    
    private void getFinalStatesNoRebound(
            final int[] startState,             //IN: initial state (positions of all robots)
            final int goalPosition,             //IN: position of goal
            final boolean isWildcardGoal,       //IN: is it the wildcard goal (any robot)
            final KnownStates knownStates,      //OUT: all known states
            final List<int[]> finalStates       //OUT: final states (goal robot has reached goal position)
            ) throws InterruptedException {
        int depth = knownStates.incrementDepth();
        assert 0 == depth : depth;
        final int[] tmpDirs = new int[startState.length];
        for (int i = 0;  i < tmpDirs.length;  ++i) { tmpDirs[i] = 7; }  // 7 == not_yet_moved
        knownStates.addKey(startState);
        knownStates.addState(startState);
        knownStates.addDirection(tmpDirs);
        final int[] tmpState = new int[startState.length];
        final int robo1 = tmpState.length - 1;  //goal robot is always the last one.
        //is the starting position already on goal?
        if (true == isWildcardGoal) {
            for (int pos : startState) { if (goalPosition == pos) { finalStates.add(startState.clone()); } }
        } else if (goalPosition == startState[robo1]) { finalStates.add(startState.clone()); }
        //breadth-first search
        boolean foundGoal = false;
        while(true) {
            if (0 < finalStates.size()) { return; } //goal has been reached!
            depth = knownStates.incrementDepth();
            final KnownStates.Iterator iter = knownStates.iterator(depth - 1);
            System.out.println("... BFS working at depth="+depth+"   statesToExpand=" + iter.size());
            if (0 == iter.size()) { return; }       //goal NOT reachable!
            while (true == iter.next(tmpState, tmpDirs)) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                for (int pos : tmpState) { this.expandRobotPositions[pos] = true; }
                for (int robo = 0;  robo < tmpState.length;  ++robo) {
                    final int oldRoboPos = tmpState[robo],  oldRoboDir = tmpDirs[robo];
                    int dir = -1;
                    for (int dirIncr : this.board.directionIncrement) {
                        ++dir;
                        //don't allow rebound moves
                        if ((oldRoboDir != dir) && (oldRoboDir != ((dir + 2) & 3))) {
                            int newRoboPos = oldRoboPos;
                            final boolean[] walls = this.boardWalls[dir];
                            while (false == walls[newRoboPos]) {                //move the robot until it reaches a wall or another robot.
                                newRoboPos += dirIncr;                          //NOTE: we rely on the fact that all boards are surrounded
                                if (this.expandRobotPositions[newRoboPos]) {    //by outer walls. without the outer walls we would need
                                    newRoboPos -= dirIncr;                      //some additional boundary checking here.
                                    break;
                                }
                            }
                            if (oldRoboPos != newRoboPos) {
                                tmpState[robo] = newRoboPos;
                                tmpDirs[robo] = dir;
                                //if we have already found a finalState then this is the last BFS pass.
                                //and we only need to store the additional finalStates but not all the "misses".
                                if ((false == foundGoal) || ((goalPosition == newRoboPos) && ((robo1 == robo) || (true == isWildcardGoal)))) {
                                    if (true == knownStates.addKey(tmpState)) {
                                        knownStates.addState(tmpState);
                                        knownStates.addDirection(tmpDirs);
                                        if ((goalPosition == newRoboPos) && ((robo1 == robo) || (true == isWildcardGoal))) {
                                            finalStates.add(tmpState.clone());  //goal robot has reached the goal position.
                                            foundGoal = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    tmpState[robo] = oldRoboPos;
                    tmpDirs[robo] = oldRoboDir;
                }
                for (int pos : tmpState) { this.expandRobotPositions[pos] = false; }
            }
        }
    }
    
    
    
    private List<int[]> getStatesPath(final int[] finalState, final KnownStates knownStates) throws InterruptedException {
        final List<int[]> result = new ArrayList<int[]>();
        final int depth = knownStates.depth();
        if (depth > 0) {
            final int[][] tmpStates = new int[depth][this.board.getNumRobots()];
            final boolean haveResult;
            if (true == this.optAllowRebounds) {
                haveResult = this.doPathDFS(finalState.clone(), knownStates, depth-1, result, tmpStates);
            } else {
                final int[] tmpDirs = new int[this.board.getNumRobots()];
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
        final int[] tmpStatesAtDepth = tmpStates[depth];
        if (0 == depth) {
            iter.next(tmpStatesAtDepth);
            result.add(tmpStatesAtDepth.clone());
            return true;
        } else {
            while (true == iter.next(tmpStatesAtDepth)) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                //detect the number of moved robots between prevState and thisState.
                //store the position difference in diffPos if only one robot has moved.
                int diffPos = 0, prevPos = 0, i = -1;
                for (int thisPos : thisState) {
                    ++i;
                    if (tmpStatesAtDepth[i] != thisPos) {
                        if (0 == diffPos) {
                            prevPos = tmpStatesAtDepth[i];
                            diffPos = thisPos - prevPos;
                        } else {
                            diffPos = 0; break; //found more than one difference
                        }
                    }
                }
                //check if this position difference is a possible move along one row or column.
                if ((0 != diffPos) && ((Math.abs(diffPos) < this.board.width) || (0 == diffPos % this.board.width))) {
                    final int thisPos = prevPos + diffPos;
                    for (int pos : tmpStatesAtDepth) { this.expandRobotPositions[pos] = true; }
                    final int dir = this.board.getDirection(diffPos);
                    //check if the move would go though obstacles (walls or robots).
                    final int dirIncr = this.board.directionIncrement[dir];
                    final boolean[] walls = this.boardWalls[dir];
                    while (false == walls[prevPos]) {
                        prevPos += dirIncr;
                        if (this.expandRobotPositions[prevPos]) {
                            prevPos -= dirIncr;
                            break;
                        }
                    }
                    for (int pos : tmpStatesAtDepth) { this.expandRobotPositions[pos] = false; }
                    //follow the move to the previous level in the array of states. (recursion)
                    if (prevPos == thisPos) {
                        if (this.doPathDFS(tmpStatesAtDepth, knownStates, depth-1, result, tmpStates)) {
                            result.add(tmpStatesAtDepth.clone());
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
        final int[] tmpStatesAtDepth = tmpStates[depth];
        if (0 == depth) {
            iter.next(tmpStatesAtDepth);
            result.add(tmpStatesAtDepth.clone());
            return true;
        } else {
            while (true == iter.next(tmpStatesAtDepth, tmpDirections)) {
                if (Thread.interrupted()) { throw new InterruptedException(); }
                //detect the number of moved robots between prevState and thisState.
                //store the position difference in diffPos if only one robot has moved.
                int diffPos = 0, prevPos = 0, tmpDir = 0, i = -1;
                for (int thisPos : thisState) {
                    ++i;
                    if (tmpStatesAtDepth[i] != thisPos) {
                        if (0 == diffPos) {
                            prevPos = tmpStatesAtDepth[i];
                            diffPos = thisPos - prevPos;
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
                        for (int pos : tmpStatesAtDepth) { this.expandRobotPositions[pos] = true; }
                        //check if the move would go though obstacles (walls or robots).
                        final int dirIncr = this.board.directionIncrement[dir];
                        final boolean[] walls = this.boardWalls[dir];
                        while (false == walls[prevPos]) {
                            prevPos += dirIncr;
                            if (this.expandRobotPositions[prevPos]) {
                                prevPos -= dirIncr;
                                break;
                            }
                        }
                        for (int pos : tmpStatesAtDepth) { this.expandRobotPositions[pos] = false; }
                        //follow the move to the previous level in the array of states. (recursion)
                        if (prevPos == thisPos) {
                            if (this.doPathDFSNoRebound(tmpStatesAtDepth, knownStates, depth-1, result, tmpStates, tmpDirections)) {
                                result.add(tmpStatesAtDepth.clone());
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
            public abstract boolean add(final int[] state);
            public abstract long getBytesAllocated();
        }
        //store the unique keys of all known states in 32-bit ints
        //supports up to 4 robots with a board size of 256 (16*16)
        private final class AllKeysInt extends AllKeys {
            private final KeyTrieSet theSet = new KeyTrieSet(board);
            private final KeyMakerInt keyMaker = KeyMakerInt.createInstance(board.getNumRobots(), board.sizeNumBits, isBoardGoalWildcard);
            @Override
            public final boolean add(final int[] state) {
                final int key = this.keyMaker.run(state);
                return this.theSet.add(key);
            }
            @Override
            public final long getBytesAllocated() {
                return this.theSet.getBytesAllocated();
            }
        }
        //store the unique keys of all known states in 64-bit longs
        //supports more than 4 robots and/or board sizes larger than 256
        private final class AllKeysLong extends AllKeys {
            private final KeyTrieSet theSet = new KeyTrieSet(board);
            private final KeyMakerLong keyMaker = KeyMakerLong.createInstance(board.getNumRobots(), board.sizeNumBits, isBoardGoalWildcard);
            @Override
            public final boolean add(final int[] state) {
                final long key = this.keyMaker.run(state);
                return this.theSet.add(key);
            }
            @Override
            public final long getBytesAllocated() {
                return this.theSet.getBytesAllocated();
            }
        }
        
        //store all known states in a way that allows them to be retrieved later
        private abstract class AllStates {
            protected final int ARRAY_SIZE = 60 * 100 * 100; //size of each array in the list of arrays. lcm(1,2,3,4,5) = 60
            protected int numStates = 0;                     //number of states that are stored
            protected int addArrayNum = 0;                   //add: number of arrays in list "allStates"
            protected int addOffset = this.ARRAY_SIZE;       //add: current index inside the current array
            protected final List<Integer> depthBegin = new ArrayList<Integer>();    //iterateStart
            public int size() {
                return this.numStates;
            }
            public void incrementDepth() {
                this.depthBegin.add(Integer.valueOf(this.numStates));
            }
            public abstract void add(final int[] state);
            public abstract Iterator iterator(final int depth);
            public abstract class Iterator {
                protected final int iterStart, iterEnd;
                protected int iterCurrent, iterArrayNum, iterOffset;
                protected Iterator(final int depth) {
                    this.iterStart = depthBegin.get(depth).intValue();
                    this.iterEnd = ((depth + 1 < depthBegin.size()) ? depthBegin.get(depth + 1).intValue() : numStates);
                    this.iterCurrent = this.iterStart;
                    this.iterArrayNum = (this.iterStart * board.getNumRobots()) / ARRAY_SIZE;
                    this.iterOffset = (this.iterStart * board.getNumRobots()) % ARRAY_SIZE;
                }
                public int size() {
                    return this.iterEnd - this.iterStart;
                }
                public abstract boolean next(final int[] resultState);
            }
            public abstract long getBytesAllocated();
        }
        //store all known states in a list of byte arrays
        //supports board sizes up to 256 (16*16)
        private final class AllStatesByte extends AllStates {
            private byte[][] allStatesArrays = new byte[32][];
            private byte[] addArray = null;
            @Override
            public final void add(final int[] state) {
                assert 8 >= board.sizeNumBits : board.sizeNumBits;
                //if necessary, allocate an additional array and append it to the list
                if (this.addOffset >= this.ARRAY_SIZE) {
                    if (this.allStatesArrays.length <= this.addArrayNum) {
                        this.allStatesArrays = Arrays.copyOf(this.allStatesArrays, this.allStatesArrays.length << 1);
                    }
                    this.addArray = new byte[this.ARRAY_SIZE];
                    this.allStatesArrays[this.addArrayNum++] = this.addArray;
                    this.addOffset = 0;
                }
                //append state to the current array
                for (int pos : state) {
                    this.addArray[this.addOffset++] = (byte)pos;
                }
                this.numStates++;
            }
            private final class AllStatesByteIterator extends AllStates.Iterator {
                private byte[] iterArray;
                public AllStatesByteIterator(final int depth) {
                    super(depth);
                    this.iterArray = allStatesArrays[this.iterArrayNum++];
                }
                @Override
                public boolean next(final int[] resultState) {
                  final boolean hasNext = (this.iterEnd > this.iterCurrent);
                  if (true == hasNext) {
                      //if necessary, switch to next array in the list
                      if (this.iterOffset >= ARRAY_SIZE) {
                          this.iterArray = allStatesArrays[this.iterArrayNum++];
                          this.iterOffset = 0;
                      }
                      //retrieve the next state
                      for (int i = 0;  i < resultState.length;  i++) {
                          resultState[i] = (boardSizeBitMask & iterArray[this.iterOffset++]);
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
            @Override
            public final long getBytesAllocated() {
                long result = 0;
                for (int i = 0;  i < this.addArrayNum;  ++i) {
                    result += this.allStatesArrays[i].length;
                }
                return result;
            }
        }
        
        //store all directions belonging to the known states
        //(implementation is copy/paste from AllStates with some adaptions)
        private abstract class AllDirections {
            protected final int ARRAY_SIZE = 10 * 100 * 100;
            protected int numDirs = 0;
            protected int addOffset = this.ARRAY_SIZE;
            protected final List<Integer> depthBegin = new ArrayList<Integer>();
            public final void incrementDepth() {
                this.depthBegin.add(Integer.valueOf(this.numDirs));
            }
            public abstract void add(final int[] dirs);
            public abstract Iterator iterator(final int depth);
            public abstract class Iterator {
                protected final int iterStart, iterEnd;
                protected int iterCurrent, iterArrayNum, iterOffset;
                protected Iterator(final int depth) {
                    this.iterStart = depthBegin.get(depth).intValue();
                    this.iterEnd = ((depth + 1 < depthBegin.size()) ? depthBegin.get(depth + 1).intValue() : numDirs);
                    this.iterCurrent = this.iterStart;
                    this.iterArrayNum = this.iterStart / ARRAY_SIZE;
                    this.iterOffset = this.iterStart % ARRAY_SIZE;
                }
                public int size() {
                    return this.iterEnd - this.iterStart;
                }
                public abstract boolean next(final int[] resultState);
            }
            public abstract long getBytesAllocated();
        }
        //store all directions belonging to the known states in a list of short arrays
        //supports up to 5 robots (with 3 bits per direction)
        private final class AllDirectionsShort extends AllDirections {
            private final List<short[]> allDirsListOfShortArrays = new ArrayList<short[]>();
            private short[] addArray = null;
            @Override
            public final void add(final int[] dirs) {
                assert dirs.length <= 5 : dirs.length;
                //if necessary, allocate an additional array and append it to the list
                if (this.addOffset >= this.ARRAY_SIZE) {
                    this.addArray = new short[this.ARRAY_SIZE];
                    this.allDirsListOfShortArrays.add(this.addArray);
                    this.addOffset = 0;
                }
                //append "dirs" to the current array in list
                int packed = 0;
                for (int dir : dirs) {
                    packed = (packed << 3) | dir;
                }
                this.addArray[this.addOffset++] = (short)packed;
                this.numDirs++;
            }
            private final class AllDirectionsShortIterator extends AllDirections.Iterator {
                private short[] iterArray;
                public AllDirectionsShortIterator(final int depth) {
                    super(depth);
                    this.iterArray = ((allDirsListOfShortArrays.size() == 0) ? null : allDirsListOfShortArrays.get(this.iterArrayNum++));
                }
                @Override
                public boolean next(final int[] resultDirs) {
                    assert resultDirs.length <= 5 : resultDirs.length;
                    final boolean hasNext = (this.iterEnd > this.iterCurrent);
                    if (true == hasNext) {
                        if (this.iterOffset >= ARRAY_SIZE) {
                            this.iterArray = allDirsListOfShortArrays.get(this.iterArrayNum++);
                            this.iterOffset = 0;
                        }
                        int packed = this.iterArray[this.iterOffset++];
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
            @Override
            public final long getBytesAllocated() {
                long result = 0;
                for (short[] dirArray : this.allDirsListOfShortArrays) {
                    result += dirArray.length << 1;
                }
                return result;
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
                assert resultState.length == board.getNumRobots() : resultState.length;
                return this.allStatesIter.next(resultState);
            }
            public boolean next(final int[] resultState, final int[] resultDirs) {
                assert this.allStatesIter.size() == this.allDirsIter.size();
                assert resultState.length == board.getNumRobots() : resultState.length;
                assert resultDirs.length == board.getNumRobots() : resultDirs.length;
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
        
        public final boolean addKey(final int[] state) {
            assert state.length == board.getNumRobots() : state.length;
            return this.allKeys.add(state);
        }
        
        public final void addState(final int[] state) {
            assert state.length == board.getNumRobots() : state.length;
            this.allStates.add(state);
        }
        
        public final void addDirection(final int[] dirs) {
            assert dirs.length == board.getNumRobots() : dirs.length;
            this.allDirections.add(dirs);
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
        public final String megaBytesAllocated() {
            final int keysMB = (int)((this.allKeys.getBytesAllocated() + (1 << 20) - 1) >> 20);
            final int statesMB = (int)((this.allStates.getBytesAllocated() + (1 << 20) - 1) >> 20);
            final int dirsMB = (int)((this.allDirections.getBytesAllocated() + (1 << 20) - 1) >> 20);
            return "megabytes allocated: keys=" + keysMB + " states=" + statesMB + " directions=" + dirsMB +
                    " total=" + (keysMB + statesMB + dirsMB);
        }
    }



}

