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

import java.util.Arrays;




public abstract class KeyMakerLong {
    
    /**
     * Creates the <tt>long</tt> key from the values of the given <tt>state</tt>.
     *
     * @param state array of int values (positions of the robots on the board)
     * @return the key
     */
    public abstract long run(final int[] state);
    
    
    /**
     * Creates an instance of <tt>KeyMakerLong</tt> that is tailored to the given parameters.
     *
     * @param boardNumRobots number of robots on the board (length of parameter <tt>state</tt> of method <tt>run</tt>)
     * @param boardSizeNumBits number of bits required to store the size of the board (the 16x16 board need 8 bits)
     * @param isBoardGoalWildcard true if the current goal is a wildcard goal (can be reached by any robot)
     * @return the instance of KeyMakerLong created
     */
    public static KeyMakerLong createInstance(int boardNumRobots, int boardSizeNumBits, boolean isBoardGoalWildcard) {
        final KeyMakerLong keyMaker;
        switch (boardNumRobots) {
        case 4:  keyMaker = (isBoardGoalWildcard ? new KeyMakerLong44(boardSizeNumBits, 4) : new KeyMakerLong43(boardSizeNumBits)); break;
        case 5:  keyMaker = (isBoardGoalWildcard ? new KeyMakerLongAll(boardNumRobots, boardSizeNumBits, isBoardGoalWildcard) : new KeyMakerLong54(boardSizeNumBits)); break;
        default: keyMaker = new KeyMakerLongAll(boardNumRobots, boardSizeNumBits, isBoardGoalWildcard);
        }
        return keyMaker;
    }
    
    
    /**
     * Creates an instance of <tt>KeyMakerLong</tt> that is tailored to the given parameters.
     *
     * @param boardNumRobots number of robots on the board (length of parameter <tt>state</tt> of method <tt>run</tt>)
     * @param boardSizeNumBits number of bits required to store the size of the board (the 16x16 board need 8 bits)
     * @param isBoardGoalWildcard true if the current goal is a wildcard goal (can be reached by any robot)
     * @param isSolution01 true if the active robot can reach the goal in one move
     * @return the instance of KeyMakerLong created
     */
    public static KeyMakerLong createInstance(int boardNumRobots, int boardSizeNumBits, boolean isBoardGoalWildcard, boolean isSolution01) {
        final KeyMakerLong keyMaker;
        if (isSolution01) {
            switch (boardNumRobots) {
            case 4:  keyMaker = new KeyMakerLong33(boardSizeNumBits, 4); break;
            case 5:  keyMaker = new KeyMakerLong44(boardSizeNumBits, 5); break;
            default: keyMaker = null;  // other numbers of robots are not supported!
            }
        } else {
            keyMaker = createInstance(boardNumRobots, boardSizeNumBits, isBoardGoalWildcard);
        }
        return keyMaker;
    }
    
    
    private static final class KeyMakerLongAll extends KeyMakerLong {
        private final int[] tmpState;
        private final int idxSort, idxLen1, idxLen2;
        private final int s1;
        private KeyMakerLongAll(int boardNumRobots, int boardSizeNumBits, boolean isBoardGoalWildcard) {
            this.tmpState = new int[boardNumRobots];
            this.idxSort = this.tmpState.length - (isBoardGoalWildcard ? 0 : 1);
            this.idxLen1 = this.tmpState.length - 1;
            this.idxLen2 = this.tmpState.length - 2;
            this.s1 = boardSizeNumBits;
        }
        @Override
        public final long run(final int[] state) {
            assert this.tmpState.length == state.length : state.length;
            //copy and sort state
            System.arraycopy(state, 0, this.tmpState, 0, state.length);
            Arrays.sort(this.tmpState, 0, this.idxSort);
            //pack state into a single long value
            long result = this.tmpState[this.idxLen1];
            for (int i = this.idxLen2;  i >= 0;  --i) {
                result = (result << this.s1) | this.tmpState[i];
            }
            return result;
        }
    }
    
    
    private static final class KeyMakerLong33 extends KeyMakerLong {     //sort 3 of 3 elements
        private final int s1, s2, len;
        private KeyMakerLong33(int boardSizeNumBits, int len) {
            this.len = len;
            this.s1 = boardSizeNumBits;
            this.s2 = boardSizeNumBits * 2;
        }
        @Override
        public final long run(final int[] state) {
            assert this.len == state.length : state.length;
            final int a = state[0],  b = state[1],  c = state[2];
            final long result;
            if (a < b) {
                if (a < c) {
                    if (b < c) { result = (a | (b << this.s1)) | ((long)c << this.s2);
                    } else {     result = (a | (c << this.s1)) | ((long)b << this.s2); }
                } else {         result = (c | (a << this.s1)) | ((long)b << this.s2); }
            } else {
                if (b < c) {
                    if (a < c) { result = (b | (a << this.s1)) | ((long)c << this.s2);
                    } else {     result = (b | (c << this.s1)) | ((long)a << this.s2); }
                } else {         result = (c | (b << this.s1)) | ((long)a << this.s2); }
            }
            return result;
        }
    }
    
    
    private static final class KeyMakerLong43 extends KeyMakerLong {     //sort 3 of 4 elements
        private final int s1, s2, s3;
        private KeyMakerLong43(int boardSizeNumBits) {
            this.s1 = boardSizeNumBits;
            this.s2 = boardSizeNumBits * 2;
            this.s3 = boardSizeNumBits * 3;
        }
        @Override
        public final long run(final int[] state) {
            assert 4 == state.length : state.length;
            final int a = state[0],  b = state[1],  c = state[2];
            final long result;
            if (a < b) {
                if (a < c) {
                    if (b < c) { result = (a | (b << this.s1)) | ((long)c << this.s2);
                    } else {     result = (a | (c << this.s1)) | ((long)b << this.s2); }
                } else {         result = (c | (a << this.s1)) | ((long)b << this.s2); }
            } else {
                if (b < c) {
                    if (a < c) { result = (b | (a << this.s1)) | ((long)c << this.s2);
                    } else {     result = (b | (c << this.s1)) | ((long)a << this.s2); }
                } else {         result = (c | (b << this.s1)) | ((long)a << this.s2); }
            }
            return result | ((long)state[3] << this.s3);
        }
    }
    
    
    private static final class KeyMakerLong44 extends KeyMakerLong {     //sort 4 of 4 elements
        private final int s1, s2, s3, len;
        private KeyMakerLong44(int boardSizeNumBits, int len) {
            this.len = len;
            this.s1 = boardSizeNumBits;
            this.s2 = boardSizeNumBits * 2;
            this.s3 = boardSizeNumBits * 3;
        }
        @Override
        public final long run(final int[] state) {
            assert this.len == state.length : state.length;
            final int a = state[0],  b = state[1],  c = state[2],  d = state[3];
            final long result;
            if (a <= b) {
                if (c <= d) {
                    if (a <= c) {
                        if (b <= d) {
                            if (b <= c) { result = (a | (b << this.s1)) | ((long)c << this.s2) | ((long)d << this.s3);
                            } else {      result = (a | (c << this.s1)) | ((long)b << this.s2) | ((long)d << this.s3); }
                        } else {          result = (a | (c << this.s1)) | ((long)d << this.s2) | ((long)b << this.s3); }
                    } else {
                        if (b <= d) {     result = (c | (a << this.s1)) | ((long)b << this.s2) | ((long)d << this.s3);
                        } else {
                            if (a <= d) { result = (c | (a << this.s1)) | ((long)d << this.s2) | ((long)b << this.s3);
                            } else {      result = (c | (d << this.s1)) | ((long)a << this.s2) | ((long)b << this.s3); }
                        }
                    }
                } else {
                    if (a <= d) {
                        if (b <= c) {
                            if (b <= d) { result = (a | (b << this.s1)) | ((long)d << this.s2) | ((long)c << this.s3);
                            } else {      result = (a | (d << this.s1)) | ((long)b << this.s2) | ((long)c << this.s3); }
                        } else {          result = (a | (d << this.s1)) | ((long)c << this.s2) | ((long)b << this.s3); }
                    } else {
                        if (b <= c) {     result = (d | (a << this.s1)) | ((long)b << this.s2) | ((long)c << this.s3);
                        } else {
                            if (a <= c) { result = (d | (a << this.s1)) | ((long)c << this.s2) | ((long)b << this.s3);
                            } else {      result = (d | (c << this.s1)) | ((long)a << this.s2) | ((long)b << this.s3); }
                        }
                    }
                }
            } else {
                if (c <= d) {
                    if (b <= c) {
                        if (a <= d) {
                            if (a <= c) { result = (b | (a << this.s1)) | ((long)c << this.s2) | ((long)d << this.s3);
                            } else {      result = (b | (c << this.s1)) | ((long)a << this.s2) | ((long)d << this.s3); }
                        } else {          result = (b | (c << this.s1)) | ((long)d << this.s2) | ((long)a << this.s3); }
                    } else {
                        if (a <= d) {     result = (c | (b << this.s1)) | ((long)a << this.s2) | ((long)d << this.s3);
                        } else {
                            if (b <= d) { result = (c | (b << this.s1)) | ((long)d << this.s2) | ((long)a << this.s3);
                            } else {      result = (c | (d << this.s1)) | ((long)b << this.s2) | ((long)a << this.s3); }
                        }
                    }
                } else {
                    if (b <= d) {
                        if (a <= c) {
                            if (a <= d) { result = (b | (a << this.s1)) | ((long)d << this.s2) | ((long)c << this.s3);
                            } else {      result = (b | (d << this.s1)) | ((long)a << this.s2) | ((long)c << this.s3); }
                        } else {          result = (b | (d << this.s1)) | ((long)c << this.s2) | ((long)a << this.s3); }
                    } else {
                        if (a <= c) {     result = (d | (b << this.s1)) | ((long)a << this.s2) | ((long)c << this.s3);
                        } else {
                            if (b <= c) { result = (d | (b << this.s1)) | ((long)c << this.s2) | ((long)a << this.s3);
                            } else {      result = (d | (c << this.s1)) | ((long)b << this.s2) | ((long)a << this.s3); }
                        }
                    }
                }
            }
            return result;
        }
    }
    
    
    private static final class KeyMakerLong54 extends KeyMakerLong {     //sort 4 of 5 elements
        private final int s1, s2, s3, s4;
        private KeyMakerLong54(int boardSizeNumBits) {
            this.s1 = boardSizeNumBits;
            this.s2 = boardSizeNumBits * 2;
            this.s3 = boardSizeNumBits * 3;
            this.s4 = boardSizeNumBits * 4;
        }
        @Override
        public final long run(final int[] state) {
            assert 5 == state.length : state.length;
            final int a = state[0],  b = state[1],  c = state[2],  d = state[3];
            final long result;
            if (a <= b) {
                if (c <= d) {
                    if (a <= c) {
                        if (b <= d) {
                            if (b <= c) { result = (a | (b << this.s1)) | ((long)c << this.s2) | ((long)d << this.s3);
                            } else {      result = (a | (c << this.s1)) | ((long)b << this.s2) | ((long)d << this.s3); }
                        } else {          result = (a | (c << this.s1)) | ((long)d << this.s2) | ((long)b << this.s3); }
                    } else {
                        if (b <= d) {     result = (c | (a << this.s1)) | ((long)b << this.s2) | ((long)d << this.s3);
                        } else {
                            if (a <= d) { result = (c | (a << this.s1)) | ((long)d << this.s2) | ((long)b << this.s3);
                            } else {      result = (c | (d << this.s1)) | ((long)a << this.s2) | ((long)b << this.s3); }
                        }
                    }
                } else {
                    if (a <= d) {
                        if (b <= c) {
                            if (b <= d) { result = (a | (b << this.s1)) | ((long)d << this.s2) | ((long)c << this.s3);
                            } else {      result = (a | (d << this.s1)) | ((long)b << this.s2) | ((long)c << this.s3); }
                        } else {          result = (a | (d << this.s1)) | ((long)c << this.s2) | ((long)b << this.s3); }
                    } else {
                        if (b <= c) {     result = (d | (a << this.s1)) | ((long)b << this.s2) | ((long)c << this.s3);
                        } else {
                            if (a <= c) { result = (d | (a << this.s1)) | ((long)c << this.s2) | ((long)b << this.s3);
                            } else {      result = (d | (c << this.s1)) | ((long)a << this.s2) | ((long)b << this.s3); }
                        }
                    }
                }
            } else {
                if (c <= d) {
                    if (b <= c) {
                        if (a <= d) {
                            if (a <= c) { result = (b | (a << this.s1)) | ((long)c << this.s2) | ((long)d << this.s3);
                            } else {      result = (b | (c << this.s1)) | ((long)a << this.s2) | ((long)d << this.s3); }
                        } else {          result = (b | (c << this.s1)) | ((long)d << this.s2) | ((long)a << this.s3); }
                    } else {
                        if (a <= d) {     result = (c | (b << this.s1)) | ((long)a << this.s2) | ((long)d << this.s3);
                        } else {
                            if (b <= d) { result = (c | (b << this.s1)) | ((long)d << this.s2) | ((long)a << this.s3);
                            } else {      result = (c | (d << this.s1)) | ((long)b << this.s2) | ((long)a << this.s3); }
                        }
                    }
                } else {
                    if (b <= d) {
                        if (a <= c) {
                            if (a <= d) { result = (b | (a << this.s1)) | ((long)d << this.s2) | ((long)c << this.s3);
                            } else {      result = (b | (d << this.s1)) | ((long)a << this.s2) | ((long)c << this.s3); }
                        } else {          result = (b | (d << this.s1)) | ((long)c << this.s2) | ((long)a << this.s3); }
                    } else {
                        if (a <= c) {     result = (d | (b << this.s1)) | ((long)a << this.s2) | ((long)c << this.s3);
                        } else {
                            if (b <= c) { result = (d | (b << this.s1)) | ((long)c << this.s2) | ((long)a << this.s3);
                            } else {      result = (d | (c << this.s1)) | ((long)b << this.s2) | ((long)a << this.s3); }
                        }
                    }
                }
            }
            return result | ((long)state[4] << this.s4);
        }
    }


}

