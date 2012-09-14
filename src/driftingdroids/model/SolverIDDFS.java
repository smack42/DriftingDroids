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
    
    private final int[][] states = new int[MAX_DEPTH][];
    private final boolean[][] expandRobotPositions = new boolean[MAX_DEPTH][];
    private KnownStates knownStates;
    private int goalPosition;
    private int minRobotLast;
    private int depthLimit;
    

    public SolverIDDFS(final Board board) {
        super(board);
        for (int i = 0;  i < this.expandRobotPositions.length;  ++i) {
            this.expandRobotPositions[i] = new boolean[board.size];
            Arrays.fill(this.expandRobotPositions[i], false);
        }
    }
    
    
    //TODO add support for optAllowRebounds
    //TODO comply with special cases in the rules (solutions with zero or one moves are not allowed, instead a longer solution must be found because the goal robot has to ricochet at least once)
    
    @Override
    public List<Solution> execute() throws InterruptedException {
        final long startExecute = System.nanoTime();
        this.lastResultSolutions = new ArrayList<Solution>();
        
        System.out.println("***** " + this.getClass().getSimpleName() + " *****");
        System.out.println("options: " + this.getOptionsAsString());
        
        this.states[0] = this.board.getRobotPositions().clone();
        swapGoalLast(this.states[0]);   //goal robot is always the last one.
        System.out.println("startState=" + this.stateString(this.states[0]));
        
        this.goalPosition = this.board.getGoal().position;
        this.minRobotLast = (this.isBoardGoalWildcard ? 0 : this.states[0].length - 1);
        this.iddfs();
        
        this.solutionStoredStates = this.knownStates.size();
        this.knownStates = null;    //allow garbage collection
        
        this.sortSolutions();
        this.solutionMilliSeconds = (System.nanoTime() - startExecute) / 1000000L;
        return this.lastResultSolutions;
    }
    
    
    
    private void iddfs() throws InterruptedException {
        this.knownStates = null;
        
        //TODO if goal is already reached in startState, then do not add startState to knownStates
        
        long totalStates = 0;
        for (this.depthLimit = 1;  MAX_DEPTH > this.depthLimit;  ++this.depthLimit) {
            this.knownStates = new KnownStates(this.knownStates);
            if (this.depthLimit > 1) {
                dfsRecursion(1);
            } else {
                dfsLast(1);
            }
            totalStates += this.knownStates.size();
            System.out.println("iddfs: " + this.knownStates.infoString() + " totalStates=" + totalStates);
            if (false == this.lastResultSolutions.isEmpty()) {
                break;  //found solution(s)
            }
        }
    }
    
    
    
    private void dfsRecursion(final int depth) throws InterruptedException {
        //if (Thread.interrupted()) { throw new InterruptedException(); }
        final boolean[] expandRobotPositions = this.expandRobotPositions[depth];
        final int[] oldState = this.states[depth - 1];
        final int depth1 = depth + 1;
        for (int pos : oldState) { expandRobotPositions[pos] = true; }
        //move all robots
        int robo = -1;
        for (int oldRoboPos : oldState) {
            ++robo;
            int dir = -1;
            for (int dirIncr : this.board.directionIncrement) {
                ++dir;
                int newRoboPos = oldRoboPos;
                final byte[] walls = this.boardWalls[dir];
                while (0 == walls[newRoboPos]) {                    //move the robot until it reaches a wall or another robot.
                    newRoboPos += dirIncr;                          //NOTE: we rely on the fact that all boards are surrounded
                    if (expandRobotPositions[newRoboPos]) {         //by outer walls. without the outer walls we would need
                        newRoboPos -= dirIncr;                      //some additional boundary checking here.
                        break;
                    }
                }
                if (oldRoboPos != newRoboPos) { //the robot has actually moved
                    oldState[robo] = newRoboPos;
                    if (true == this.knownStates.add(oldState, depth)) {    //the new state is not already known
                        this.states[depth] = oldState.clone();
                        oldState[robo] = oldRoboPos;
                        if (this.depthLimit > depth1) {
                            dfsRecursion(depth1);
                        } else {
                            dfsLast(depth1);
                        }
                    } else {
                        oldState[robo] = oldRoboPos;
                    }
                }
            }
        }
        for (int pos : oldState) { expandRobotPositions[pos] = false; }
    }
    
    
    
    private void dfsLast(final int depth) throws InterruptedException {
        if (Thread.interrupted()) { throw new InterruptedException(); }
        final boolean[] expandRobotPositions = this.expandRobotPositions[depth];
        final int[] oldState = this.states[depth - 1];
        for (int pos : oldState) { expandRobotPositions[pos] = true; }
        //move goal robot(s) only
        for (int robo = this.minRobotLast;  robo < oldState.length;  ++robo) {
            final int oldRoboPos = oldState[robo];
            int dir = -1;
            for (int dirIncr : this.board.directionIncrement) {
                ++dir;
                int newRoboPos = oldRoboPos;
                final byte[] walls = this.boardWalls[dir];
                while (0 == walls[newRoboPos]) {                    //move the robot until it reaches a wall or another robot.
                    newRoboPos += dirIncr;                          //NOTE: we rely on the fact that all boards are surrounded
                    if (expandRobotPositions[newRoboPos]) {         //by outer walls. without the outer walls we would need
                        newRoboPos -= dirIncr;                      //some additional boundary checking here.
                        break;
                    }
                }
                //the robot has actually moved and has arrived at the goal
                if ((oldRoboPos != newRoboPos) && (this.goalPosition == newRoboPos)) {
                    oldState[robo] = newRoboPos;
                    if (true == this.knownStates.add(oldState, depth)) {    //the new state is not already known
                        this.states[depth] = oldState.clone();
                        oldState[robo] = oldRoboPos;
                        //build solution
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
                    } else {
                        oldState[robo] = oldRoboPos;
                    }
                }
            }
        }
        for (int pos : oldState) { expandRobotPositions[pos] = false; }
    }
    
    
    
    private class KnownStates {
        private final AllKeys allKeys;
        
        public KnownStates(final KnownStates previousKnownStates) {
            this.allKeys = ((true == isBoardStateInt32) ? new AllKeysInt(previousKnownStates) : new AllKeysLong(previousKnownStates));
        }
        
        //store the unique keys of all known states
        private abstract class AllKeys {
            protected final TrieMapByte theMap;
            public int size = 0;
            
            protected AllKeys(final KnownStates previousKnownStates) {
                this.size = 0;
                if (null == previousKnownStates) {
                    this.theMap = new TrieMapByte(Math.max(12, boardNumRobots * boardSizeNumBits));
                } else {
                    this.theMap = previousKnownStates.allKeys.theMap;
                    this.theMap.allValuesOr128();
                }
            }
            
            public abstract boolean add(final int[] state, final int depth);
            
            public long getBytesAllocated() {
                return this.theMap.getBytesAllocated();
            }
        }
        //store the unique keys of all known states in 32-bit ints
        //supports up to 4 robots with a board size of 256 (16*16)
        private final class AllKeysInt extends AllKeys {
            private final KeyMakerInt keyMaker = KeyMakerInt.createInstance(boardNumRobots, boardSizeNumBits, isBoardGoalWildcard);
            public AllKeysInt(final KnownStates previousKnownStates) {
                super(previousKnownStates);
            }
            @Override
            public final boolean add(final int[] state, final int depth) {
                final int key = this.keyMaker.run(state);
                final byte prevDepth = this.theMap.get(key);
                if (0x80 == (0x80 & prevDepth)) {
                    if (depth <= (0x7f & prevDepth)) {
                        this.theMap.put(key, (byte)depth);
                        ++this.size;
                        return true;
                    }
                } else if (depth < (0x7f & prevDepth)) {
                    this.theMap.put(key, (byte)depth);
                    ++this.size;
                    return true;
                }
                return false;
            }
        }
        //store the unique keys of all known states in 64-bit longs
        //supports more than 4 robots and/or board sizes larger than 256
        private final class AllKeysLong extends AllKeys {
            private final KeyMakerLong keyMaker = KeyMakerLong.createInstance(boardNumRobots, boardSizeNumBits, isBoardGoalWildcard);
            public AllKeysLong(final KnownStates previousKnownStates) {
                super(previousKnownStates);
            }
            @Override
            public final boolean add(final int[] state, final int depth) {
                final long key = this.keyMaker.run(state);
                final byte prevDepth = this.theMap.get(key);
                if (0x80 == (0x80 & prevDepth)) {
                    if (depth <= (0x7f & prevDepth)) {
                        this.theMap.put(key, (byte)depth);
                        ++this.size;
                        return true;
                    }
                } else if (depth < (0x7f & prevDepth)) {
                    this.theMap.put(key, (byte)depth);
                    ++this.size;
                    return true;
                }
                return false;
            }
        }

        public boolean add(int[] state, int depth) {
            return this.allKeys.add(state, depth);
        }
        public final int size() {
            return this.allKeys.size;
        }
        public final String infoString() {
            final int keysMB = (int)((this.allKeys.getBytesAllocated() + (1 << 20) - 1) >> 20);
            return "depthLimit=" + depthLimit + " states=" + this.size() + " megaBytes=" + keysMB;
        }
    }

}
