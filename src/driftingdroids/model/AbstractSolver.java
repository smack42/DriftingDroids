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

import java.util.Arrays;
import java.util.Formatter;
import java.util.Set;
import java.util.TreeSet;



public abstract class AbstractSolver {
    
    public enum SOLUTION_MODE {
        ANY("any"), MINIMUM("minimum"), MAXIMUM("maximum");
        private final String name;
        private SOLUTION_MODE(String name) { this.name = name; }
        @Override public String toString() { return this.name; }
    }
    
    protected final Board board;
    protected final byte[][] boardWalls;
    protected final int boardSizeNumBits;
    protected final int boardSizeBitMask;
    protected final int boardNumRobots;
    protected final boolean isBoardStateInt32;
    protected final boolean isBoardGoalWildcard;
    protected final boolean[] expandRobotPositions;
    
    protected AbstractSolver (final Board board) {
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
    
    
    protected SOLUTION_MODE optSolutionMode = SOLUTION_MODE.ANY;
    protected boolean optAllowRebounds = true;
    
    protected long solutionMilliSeconds = 0;
    protected int solutionStoredStates = 0;
    
    protected Move[] solutionMoves = null;
    protected int solutionMoveIndex = 0;
    
    
    
    /**
     * find the solution.
     * store the moves in array "solutionMoves". they can be retrieved using
     * methods getCurrent/Next/PrevMove.
     * @return the size of the solution (number of moves required to reach the goal).
     * @throws InterruptedException 
     */
    abstract public int execute() throws InterruptedException;
    
    
    
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
    
    
    
    public Move getCurrentMove() {
        if ((null != this.solutionMoves) && (this.solutionMoveIndex >= 0) && (this.solutionMoveIndex < this.solutionMoves.length)) {
            return this.solutionMoves[this.solutionMoveIndex];
        } else {
            return null;
        }
    }
    public Move getNextMove() {
        final Move result =  getCurrentMove();
        if (null != result) {
            this.solutionMoveIndex++;
        }
        return result;
    }
    public Move getPrevMove() {
        if (this.solutionMoveIndex > 0) {
            this.solutionMoveIndex--;
        }
        return getCurrentMove();
    }
    
    
    
    public boolean isMoveRebound(Move move) {
        return this.isSolutionRebound(this.solutionMoves, move);
    }
    protected boolean isSolutionRebound(final Move[] moves, final Move queryMove) {
        boolean result = false;
        int[] directions = this.board.getRobotPositions().clone();
        Arrays.fill(directions, -1);
        for (Move move : moves) {
            if ((-1 == directions[move.robotNumber]) || (move.direction != (3 & (directions[move.robotNumber] + 2)))) {
                directions[move.robotNumber] = move.direction;
            } else {
                if ((queryMove == null) || (queryMove == move)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
    
    
    
    public int getSolutionSize() {
        return (this.solutionMoves == null ? 0 : this.solutionMoves.length);
    }
    public long getSolutionMilliSeconds() {
        return this.solutionMilliSeconds;
    }
    public int getSolutionStoredStates() {
        return this.solutionStoredStates;
    }
    
    
    
    public Set<Integer> getSolutionRobotsMoved() {
        return this.getSolutionRobotsMoved(this.solutionMoves);
    }
    protected Set<Integer> getSolutionRobotsMoved(final Move[] moves) {
        final TreeSet<Integer> result = new TreeSet<Integer>(); //sorted set
        for (Move mov : moves) {
            result.add(Integer.valueOf(mov.robotNumber));
        }
        return result;
    }
    
    
    
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("solution: size=").append(this.getSolutionSize()).append(" *****");
        if (this.getSolutionSize() > 0) {
            for (Move mov : this.solutionMoves) {
                s.append(' ');
                s.append(mov.strRobotDirection());
            }
        }
        s.append(" *****  (storedStates=").append(this.solutionStoredStates);
        s.append(", time=").append(this.solutionMilliSeconds / 1000d).append(" seconds)");
        return s.toString();
    }
    
    
    protected final KeyMakerInt createKeyMakerInt() {
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
    
    protected abstract class KeyMakerInt {
        protected final int s1 = boardSizeNumBits,  s2 = s1 * 2;
        public abstract int run(final int[] state);
    }
    protected final class KeyMakerIntAll extends KeyMakerInt {
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
    protected final class KeyMakerInt1 extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 1 == state.length : state.length;
            return state[0];
        }
    }
    protected final class KeyMakerInt2 extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 2 == state.length : state.length;
            return (state[0] << s1) | state[1];
        }
    }
    protected final class KeyMakerInt3sort extends KeyMakerInt {
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
    protected final class KeyMakerInt3nosort extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 3 == state.length : state.length;
            int result = state[0] << s1;
            return (result << s1) | (state[1] << s1) | state[2];
        }
    }
    protected final class KeyMakerInt4sort extends KeyMakerInt {
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
    protected final class KeyMakerInt4nosort extends KeyMakerInt {
        @Override
        public final int run(final int[] state) {
            assert 4 == state.length : state.length;
            final int result = (state[0] << s2) | (state[1] << s1) | state[2];
            return (result << s1) | state[3];
        }
    }
    
    
    
    protected final KeyMakerLong createKeyMakerLong() {
        final KeyMakerLong keyMaker;
        switch (boardNumRobots) {
        case 5:  keyMaker = (isBoardGoalWildcard ? new KeyMakerLong5nosort() : new KeyMakerLong5sort()); break;
        default: keyMaker = new KeyMakerLongAll();
        }
        return keyMaker;
    }
    
    protected abstract class KeyMakerLong {
        protected final int s1 = boardSizeNumBits,  s2 = s1 * 2,  s3 = s1 * 3;
        public abstract long run(final int[] state);
    }
    protected class KeyMakerLongAll extends KeyMakerLong {
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
    protected class KeyMakerLong5sort extends KeyMakerLong {
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
    protected class KeyMakerLong5nosort extends KeyMakerLong {
        @Override
        public final long run(final int[] state) {
            assert 5 == state.length : state.length;
            final long result = (state[0] << s3) | (state[1] << s2) | (state[2] << s1) | state[3];
            return (result << s1) | state[4];
        }
    }
    
    
    
    protected void swapGoalLast(final int[] state) {
        //swap goal robot and last robot (if goal is not wildcard)
        if (false == this.isBoardGoalWildcard) {
            final int tmp = state[state.length - 1];
            state[state.length - 1] = state[this.board.getGoalRobot()];
            state[this.board.getGoalRobot()] = tmp;
        }
    }
    
    protected String stateString(final int[] state) {
        final Formatter formatter = new Formatter();
        this.swapGoalLast(state);
        for (int i = 0;  i < state.length;  i++) {
            formatter.format("%02x", Integer.valueOf(state[i]));
        }
        this.swapGoalLast(state);
        return "0x" + formatter.out().toString();
    }
    
    
}

