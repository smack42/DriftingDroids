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
    private static final int[] BIT_POS = {
        0x00000001, 0x00000002, 0x00000004, 0x00000008, 0x00000010, 0x00000020, 0x00000040, 0x00000080,
        0x00000100, 0x00000200, 0x00000400, 0x00000800, 0x00001000, 0x00002000, 0x00004000, 0x00008000,
        0x00010000, 0x00020000, 0x00040000, 0x00080000, 0x00100000, 0x00200000, 0x00400000, 0x00800000,
        0x01000000, 0x02000000, 0x04000000, 0x08000000, 0x10000000, 0x20000000, 0x40000000, 0x80000000
    };
    
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
    private final int nodeNumber, nodeSize, nodeMask;
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
        int nodeIndex = 0,  nidx;
        int[] nodeArray = this.rootNode;
        //go through nodes
        for (int i = 1;  i < this.nodeNumber;  ++i) {
            nidx = (nodeIndex + (value & this.nodeMask)) & NODE_ARRAY_MASK;
            value >>>= this.nodeBits;
            nodeIndex = nodeArray[nidx];
            if (0 == nodeIndex) {
                //create a new node
                nodeIndex = this.newNode();
                nodeArray[nidx] = nodeIndex;
            }
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
        }
        //get leaf
        nidx = (nodeIndex + (value & this.nodeMask)) & NODE_ARRAY_MASK;
        value >>>= this.nodeBits;
        int leafIndex = nodeArray[nidx];
        if (0 == leafIndex) {
            //create a new leaf
            leafIndex = this.newLeaf();
            nodeArray[nidx] = leafIndex;
        }
        final int[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        //set bit in leaf
        final int lidx = (leafIndex + (value & this.leafMask)) & LEAF_ARRAY_MASK;
        final int oldBits = leafArray[lidx];
        final int newBits = oldBits | BIT_POS[(value >>> this.leafShift) & 31];
        final boolean bitHasBeenAdded = (oldBits != newBits);
        if (true == bitHasBeenAdded) {
            leafArray[lidx] = newBits;
        }
        return bitHasBeenAdded;
    }
    
    
    
    /**
     * Adds the specified <tt>long</tt> value to this set if it is not already present.
     * 
     * @param value to be added to this set
     * @return <code>true</code> if this set did not already contain the specified value
     */
    public final boolean add(long value) {
        //root node
        int nodeIndex = 0,  nidx;
        int[] nodeArray = this.rootNode;
        //go through nodes
        for (int i = 1;  i < this.nodeNumber;  ++i) {
            nidx = (nodeIndex + ((int)value & this.nodeMask)) & NODE_ARRAY_MASK;
            value >>>= this.nodeBits;
            nodeIndex = nodeArray[nidx];
            if (0 == nodeIndex) {
                //create a new node
                nodeIndex = this.newNode();
                nodeArray[nidx] = nodeIndex;
            }
            nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
        }
        //get leaf
        nidx = (nodeIndex + ((int)value & this.nodeMask)) & NODE_ARRAY_MASK;
        value >>>= this.nodeBits;
        int leafIndex = nodeArray[nidx];
        if (0 == leafIndex) {
            //create a new leaf
            leafIndex = this.newLeaf();
            nodeArray[nidx] = leafIndex;
        }
        final int[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        //set bit in leaf
        final int lidx = (leafIndex + ((int)value & this.leafMask)) & LEAF_ARRAY_MASK;
        final int oldBits = leafArray[lidx];
        final int newBits = oldBits | BIT_POS[((int)value >>> this.leafShift) & 31];
        final boolean bitHasBeenAdded = (oldBits != newBits);
        if (true == bitHasBeenAdded) {
            leafArray[lidx] = newBits;
        }
        return bitHasBeenAdded;
    }
    
    
    
    private final int newNode() {
        if (this.nextNode >= this.nextNodeArray) {
            if (this.nodeArrays.length <= this.numNodeArrays) {
                this.nodeArrays = Arrays.copyOf(this.nodeArrays, (this.nodeArrays.length * 3) >> 1);
            }
            this.nodeArrays[this.numNodeArrays++] = new int[NODE_ARRAY_SIZE];
            this.nextNodeArray += NODE_ARRAY_SIZE;
        }
        final int result = this.nextNode;
        this.nextNode += this.nodeSize;
        return result;
    }
    
    
    
    private final int newLeaf() {
        if (this.nextLeaf >= this.nextLeafArray) {
            if (this.leafArrays.length <= this.numLeafArrays) {
                this.leafArrays = Arrays.copyOf(this.leafArrays, (this.leafArrays.length * 3) >> 1);
            }
            this.leafArrays[this.numLeafArrays++] = new int[LEAF_ARRAY_SIZE];
            this.nextLeafArray += LEAF_ARRAY_SIZE;
        }
        final int result = this.nextLeaf;
        this.nextLeaf += this.leafSize;
        return result;
    }
    
    
    
//    /**
//     * A simple test case for this class.
//     * @param args not used
//     */
//    public static final void main(String[] args) {
//        final TrieSet t = new TrieSet(32);
//        for (long i = 0;  i < 0x100000000L;  ++i) {
//            final boolean addedA = t.add(i);
//            final boolean addedB = t.add(i);
//            if ((true != addedA) || (false != addedB)) {
//                System.out.println("BUG!");
//            }
//        }
//        System.out.println("done.");
//    }
}
