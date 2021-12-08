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



/**
 * This class is a minimal <tt>Set</tt> implementation for primitive <tt>int</tt>
 * or <tt>long</tt> values, based on a trie (prefix tree) data structure.
 * <p>
 * The aim is to balance a fast recognition of duplicate values and a
 * compact storage of data.
 * <p>
 * Thanks go to David Hansel for the idea and for sending his C implementation
 * that served as the template of this class.
 */
public final class TrieSet {
    
    private static final int NODE_ARRAY_SHIFT = 16;
    private static final int NODE_ARRAY_SIZE = 1 << NODE_ARRAY_SHIFT;
    private static final int NODE_ARRAY_MASK = NODE_ARRAY_SIZE - 1;
    private final int[] rootNode;
    private int[][] nodeArrays;
    private int numNodeArrays, nextNode, nextNodeArray;
    
    private static final int LEAF_ARRAY_SHIFT = 16;
    private static final int LEAF_ARRAY_SIZE = 1 << LEAF_ARRAY_SHIFT;
    private static final int LEAF_ARRAY_MASK = LEAF_ARRAY_SIZE - 1;
    private int[][] leafArrays;
    private int numLeafArrays, nextLeaf, nextLeafArray;
    
    private final int nodeBits, leafBits;
    private final int nodeNumber, nodeNumberLong31, nodeSize, nodeMask;
    private final int leafShift, leafSize, leafMask;
    
    
    
    /**
     * Constructs an empty TrieSet that is tuned to the expected bit-size of values.
     * 
     * @param valueBits the maximum number of bits used by any value that will be added to the set.
     * (e.g. specify 32 if your values are of type <tt>int</tt>, or specify a lower number
     * if you are sure that your application uses only a subset of all <tt>int</tt> values)
     */
    public TrieSet(final int valueBits) {
        this.nodeBits = 4;  //tuning parameter: number of value bits per internal node
        this.leafBits = 8;  //tuning parameter: number of value bits per leaf
        
        this.nodeNumber = (valueBits - this.leafBits + (this.nodeBits - 1)) / this.nodeBits;
        this.nodeNumberLong31 = (valueBits - 31 + (this.nodeBits - 1)) / this.nodeBits;
        this.nodeSize = 1 << this.nodeBits;
        this.nodeMask = this.nodeSize - 1;
        this.leafShift = this.leafBits - 5;
        this.leafSize = 1 << this.leafShift;
        this.leafMask = this.leafSize - 1;
        
        this.nodeArrays = new int[32][];
        this.rootNode = new int[NODE_ARRAY_SIZE];
        this.nodeArrays[0] = this.rootNode;
        this.numNodeArrays = 1;
        this.nextNode = this.nodeSize;          //root node already exists
        this.nextNodeArray = NODE_ARRAY_SIZE;   //first array already exists
        
        this.leafArrays = new int[32][];
        this.numLeafArrays = 0;
        this.nextLeaf = this.leafSize;  //no leaves yet, but skip leaf "0" because this is the special value
        this.nextLeafArray = 0;         //no leaf arrays yet
    }
    
    
    
    /**
     * Adds the specified <tt>int</tt> value to this set if it is not already present.
     * 
     * @param value to be added to this set
     * @return <code>true</code> if this set did not already contain the specified value
     */
    public final boolean add(int value) {
        //root node
        int[] nodeArray = this.rootNode;
        int nidx = value & this.nodeMask;
        //go through nodes (with compression)
        for (int nodeIndex, i = 1;  i < this.nodeNumber;  ++i) {
            nodeIndex = nodeArray[nidx];
            value >>>= this.nodeBits;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ~value;   //negative
                return true;    //added
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                //exit immediately if previous and current values are equal (duplicate)
                final int prevValue = ~nodeIndex;
                if (prevValue == value) {
                    return false;   //not added
                }
                //create a new node
                if (this.nextNode >= this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += this.nodeSize;
                nodeArray[nidx] = nodeIndex;
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevValue & this.nodeMask);
                nodeArray[nidx] = ~(prevValue >>> this.nodeBits);
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx = (nodeIndex & NODE_ARRAY_MASK) + (value & this.nodeMask);
        }
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        value >>>= this.nodeBits;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            //write current value as a "compressed branch" (negative leaf index)
            //exit immediately because no leaf needs to be stored
            nodeArray[nidx] = ~value;   //negative
            return true;    //added
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            //exit immediately if previous and current values are equal (duplicate)
            final int prevValue = ~leafIndex;
            if (prevValue == value) {
                return false;   //not added
            }
            //create a new leaf
            if (this.nextLeaf >= this.nextLeafArray) {
                if (this.leafArrays.length <= this.numLeafArrays) {
                    this.leafArrays = Arrays.copyOf(this.leafArrays, this.leafArrays.length << 1);
                }
                this.leafArrays[this.numLeafArrays++] = new int[LEAF_ARRAY_SIZE];
                this.nextLeafArray += LEAF_ARRAY_SIZE;
            }
            leafIndex = this.nextLeaf;
            this.nextLeaf += this.leafSize;
            nodeArray[nidx] = leafIndex;
            //push the previous "compressed branch" further to the leaf
            final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (prevValue & this.leafMask);
            this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT][lidx] = (1 << (prevValue >>> this.leafShift));
        }
        final int[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (value & this.leafMask);
        //set bit in leaf
        final int oldBits = leafArray[lidx];
        final int newBits = oldBits | (1 << (value >>> this.leafShift));
        if (oldBits != newBits) {
            leafArray[lidx] = newBits;
            return true;    //added
        } else {
            return false;   //not added
        }
    }
    
    
    
    /**
     * Adds the specified <tt>long</tt> value to this set if it is not already present.
     * 
     * @param value to be added to this set
     * @return <code>true</code> if this set did not already contain the specified value
     */
    public final boolean add(long value) {
        //root node
        int[] nodeArray = this.rootNode;
        int nidx = (int)value & this.nodeMask;  //(int)
        int nodeIndex, i;   //used by both for() loops
        //go through nodes (without compression because value is greater than "int")
        for (i = 1;  i < this.nodeNumberLong31;  ++i) {
            nodeIndex = nodeArray[nidx];
            value >>>= this.nodeBits;
            if (0 == nodeIndex) {
                //create a new node
                if (this.nextNode >= this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += this.nodeSize;
                nodeArray[nidx] = nodeIndex;
            }
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)value & this.nodeMask);    //(int)
        }
        //go through nodes (with compression because value is inside "int" range now)
        for ( ;  i < this.nodeNumber;  ++i) {
            nodeIndex = nodeArray[nidx];
            value >>>= this.nodeBits;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ~((int)value);   //negative  //(int)
                return true;    //added
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                //exit immediately if previous and current values are equal (duplicate)
                final int prevValue = ~nodeIndex;
                if (prevValue == (int)value) {  //(int)
                    return false;   //not added
                }
                //create a new node
                if (this.nextNode >= this.nextNodeArray) {
                    if (this.nodeArrays.length <= this.numNodeArrays) {
                        this.nodeArrays = Arrays.copyOf(this.nodeArrays, this.nodeArrays.length << 1);
                    }
                    this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
                    this.nextNodeArray += NODE_ARRAY_SIZE;
                }
                nodeIndex = this.nextNode;
                this.nextNode += this.nodeSize;
                nodeArray[nidx] = nodeIndex;
                //push previous "compressed branch" one node further
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevValue & this.nodeMask);
                nodeArray[nidx] = ~(prevValue >>> this.nodeBits);
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)value & this.nodeMask);    //(int)
        }
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        value >>>= this.nodeBits;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            //write current value as a "compressed branch" (negative leaf index)
            //exit immediately because no leaf needs to be stored
            nodeArray[nidx] = ~((int)value);    //negative  //(int)
            return true;    //added
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            //exit immediately if previous and current values are equal (duplicate)
            final int prevValue = ~leafIndex;
            if (prevValue == (int)value) {  //(int)
                return false;   //not added
            }
            //create a new leaf
            if (this.nextLeaf >= this.nextLeafArray) {
                if (this.leafArrays.length <= this.numLeafArrays) {
                    this.leafArrays = Arrays.copyOf(this.leafArrays, this.leafArrays.length << 1);
                }
                this.leafArrays[this.numLeafArrays++] = new int[LEAF_ARRAY_SIZE];
                this.nextLeafArray += LEAF_ARRAY_SIZE;
            }
            leafIndex = this.nextLeaf;
            this.nextLeaf += this.leafSize;
            nodeArray[nidx] = leafIndex;
            //push the previous "compressed branch" further to the leaf
            final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (prevValue & this.leafMask);
            this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT][lidx] = (1 << (prevValue >>> this.leafShift));
        }
        final int[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + ((int)value & this.leafMask);  //(int)
        //set bit in leaf
        final int oldBits = leafArray[lidx];
        final int newBits = oldBits | (1 << ((int)value >>> this.leafShift));   //(int)
        if (oldBits != newBits) {
            leafArray[lidx] = newBits;
            return true;    //added
        } else {
            return false;   //not added
        }
    }
    
    
    
    /**
     * Returns <code>true</code> if this set contains the specified <tt>int</tt> value.
     * 
     * @param value whose presence in this set is to be tested
     * @return <code>true</code> if this set contains the specified value
     */
    public final boolean contains(int value) {
        //root node
        int[] nodeArray = this.rootNode;
        int nidx = value & this.nodeMask;
        //go through nodes (with compression)
        for (int nodeIndex, i = 1;  i < this.nodeNumber;  ++i) {
            nodeIndex = nodeArray[nidx];
            value >>>= this.nodeBits;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                return false;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                return (~nodeIndex == value);
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (value & this.nodeMask);
            }
        }
        //get leaf (with compression)
        final int leafIndex = nodeArray[nidx];
        value >>>= this.nodeBits;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            return false;
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            return (~leafIndex == value);
        }
        //test bit in leaf
        final int[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (value & this.leafMask);
        return (0 != (leafArray[lidx] & (1 << (value >>> this.leafShift))));
    }
    
    
    
    /**
     * Returns <code>true</code> if this set contains the specified <tt>long</tt> value.
     * 
     * @param value whose presence in this set is to be tested
     * @return <code>true</code> if this set contains the specified value
     */
    public final boolean contains(long value) {
        //root node
        int[] nodeArray = this.rootNode;
        int nidx = (int)value & this.nodeMask;  //(int)
        int nodeIndex, i;   //used by both for() loops
        //go through nodes (without compression because value is greater than "int")
        for (i = 1;  i < this.nodeNumberLong31;  ++i) {
            nodeIndex = nodeArray[nidx];
            value >>>= this.nodeBits;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                return false;
            }
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)value & this.nodeMask);    //(int)
        }
        //go through nodes (with compression because value is inside "int" range now)
        for ( ;  i < this.nodeNumber;  ++i) {
            nodeIndex = nodeArray[nidx];
            value >>>= this.nodeBits;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                return false;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                return (~nodeIndex == (int)value);  //(int)
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)value & this.nodeMask);    //(int)
            }
        }
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        value >>>= this.nodeBits;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            return false;
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            return (~leafIndex == (int)value);  //(int)
        }
        //test bit in leaf
        final int[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + ((int)value & this.leafMask);  //(int)
        return (0 != (leafArray[lidx] & (1 << ((int)value >>> this.leafShift))));   //(int)
    }
    
    
    
    public final long getBytesAllocated() {
        long result = 0;
        for (int i = 0;  i < this.numNodeArrays;  ++i) {
            result += this.nodeArrays[i].length * 4L;
        }
        for (int i = 0;  i < this.numLeafArrays;  ++i) {
            result += this.leafArrays[i].length * 4L;
        }
        return result;
    }
    
    
//    /**
//     * A simple test case for this class.
//     * @param args not used
//     */
//    public static final void main(String[] args) {
//        final TrieSet t = new TrieSet(32);
//        for (long i = 0;  i < 0x100000000L;  ++i) {
//            final boolean containsA = t.contains(i);
//            final boolean addedA = t.add((int)i);
//            final boolean addedB = t.add(i);
//            final boolean containsB = t.contains(i);
//            if ((false != containsA) || (true != addedA) || (false != addedB) || (true != containsB)) {
//                System.out.println("BUG!");
//            }
//        }
//        System.out.println("done.");
//    }
}
