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
 * This class is a minimal <code>Map&ltK,V&gt</code> implementation for primitive
 * <code>int</code> or <code>long</code> keys K and <tt>byte</tt> values V,
 *  based on a trie (prefix tree) data structure.
 * <p>
 * The aim is to balance a fast recognition of duplicate keys
 * and a compact storage of data.
 */
public final class KeyDepthMapTrieGeneric implements KeyDepthMap {

    public static final byte DEFAULT_VALUE = -1;    //unsigned byte: 255

    private static final int NODE_ARRAY_SHIFT = 16;
    private static final int NODE_ARRAY_SIZE = 1 << NODE_ARRAY_SHIFT;
    private static final int NODE_ARRAY_MASK = NODE_ARRAY_SIZE - 1;
    private final int[] rootNode;
    private int[][] nodeArrays;
    private int numNodeArrays, nextNode, nextNodeArray;

    private static final int LEAF_ARRAY_SHIFT = 16;
    private static final int LEAF_ARRAY_SIZE = 1 << LEAF_ARRAY_SHIFT;
    private static final int LEAF_ARRAY_MASK = LEAF_ARRAY_SIZE - 1;
    private byte[][] leafArrays;
    private int numLeafArrays, nextLeaf, nextLeafArray;

    private final int nodeBits, nodeNumber, nodeNumberUnCompr, nodeSize, nodeMask;
    private final int leafBits, leafSize, leafMask;



    /**
     * Constructs an empty map that is tuned to the expected bit-size of keys.
     * 
     * @param keyBits the maximum number of bits used by any key that will be put into the map.
     * (e.g. specify 32 if your keys are of type <tt>int</tt>, or specify a lower number
     * if you are sure that your application uses only a subset of all <tt>int</tt> keys)
     */
    public KeyDepthMapTrieGeneric(final int keyBits) {
        this.nodeBits = 4;  //tuning parameter: number of value bits per internal node
        this.leafBits = 4;  //tuning parameter: number of value bits per leaf

        this.nodeNumber = (keyBits - this.leafBits + (this.nodeBits - 1)) / this.nodeBits;
        this.nodeNumberUnCompr = (keyBits + 8 - 31 + (this.nodeBits - 1)) / this.nodeBits;
        this.nodeSize = 1 << this.nodeBits;
        this.nodeMask = this.nodeSize - 1;
        this.leafSize = 1 << this.leafBits;
        this.leafMask = this.leafSize - 1;

        this.nodeArrays = new int[32][];
        this.rootNode = new int[NODE_ARRAY_SIZE];
        this.nodeArrays[0] = this.rootNode;
        this.numNodeArrays = 1;
        this.nextNode = this.nodeSize;          //root node already exists
        this.nextNodeArray = NODE_ARRAY_SIZE;   //first array already exists

        this.leafArrays = new byte[32][];
        this.numLeafArrays = 0;
        this.nextLeaf = this.leafSize;  //no leaves yet, but skip leaf "0" because this is the special value
        this.nextLeafArray = 0;         //no leaf arrays yet
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#putIfGreater(int, int)
     */
    @Override
    public final boolean putIfGreater(int key, final int byteValue) {
        //root node
        int[] nodeArray = this.rootNode;
        int nidx = key & this.nodeMask;
        int nodeIndex, i;   //used by both for() loops
        //go through nodes (without compression because (key<<8)+value is greater than "int")
        for (i = 1;  i < this.nodeNumberUnCompr;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeBits;
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
            nidx = (nodeIndex & NODE_ARRAY_MASK) + (key & this.nodeMask);
        }
        //go through nodes (with compression because (key<<8)+value is inside "int" range now)
        for ( ;  i < this.nodeNumber;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeBits;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ((~key) << 8) | byteValue;    //negative
                return true;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == key) {
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
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
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevKey & this.nodeMask);
                nodeArray[nidx] = (~(prevKey >>> this.nodeBits) << 8) | prevVal;    //negative
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx = (nodeIndex & NODE_ARRAY_MASK) + (key & this.nodeMask);
        }
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        key >>>= this.nodeBits;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            //write current value as a "compressed branch" (negative leaf index)
            //exit immediately because no leaf needs to be stored
            nodeArray[nidx] = ((~key) << 8) | byteValue;    //negative
            return true;
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            final int prevKey = (~leafIndex) >> 8;
            final int prevVal = 0xff & leafIndex;
            //previous and current keys are equal (duplicate key)
            if (prevKey == key) {
                if (byteValue > prevVal) {  //putIfGreater
                    nodeArray[nidx] = (leafIndex ^ prevVal) | byteValue;    //negative
                    return true;
                }
                return false;
            }
            //previous and current keys are not equal
            //create a new leaf
            if (this.nextLeaf >= this.nextLeafArray) {
                if (this.leafArrays.length <= this.numLeafArrays) {
                    this.leafArrays = Arrays.copyOf(this.leafArrays, this.leafArrays.length << 1);
                }
                final byte[] newLeafArray = new byte[LEAF_ARRAY_SIZE];
                Arrays.fill(newLeafArray, DEFAULT_VALUE);
                this.leafArrays[this.numLeafArrays++] = newLeafArray;
                this.nextLeafArray += LEAF_ARRAY_SIZE;
            }
            leafIndex = this.nextLeaf;
            this.nextLeaf += this.leafSize;
            nodeArray[nidx] = leafIndex;
            //push the previous "compressed branch" further to the leaf
            final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (prevKey & this.leafMask);
            this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT][lidx] = (byte)prevVal;
        }
        final byte[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (key & this.leafMask);
        final byte prevVal = leafArray[lidx];
        if (byteValue > prevVal) {  //putIfGreater
            leafArray[lidx] = (byte)byteValue;
            return true;
        }
        return false;
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#putIfGreater(long, int)
     */
    @Override
    public final boolean putIfGreater(long key, final int byteValue) {
        //this method is copy&paste from put(int,byte) with only a few (int) casts added where required.
        //those lines are marked with //(int)
        //root node
        int[] nodeArray = this.rootNode;
        int nidx = (int)key & this.nodeMask;    //(int)
        int nodeIndex, i;   //used by both for() loops
        //go through nodes (without compression because (key<<8)+value is greater than "int")
        for (i = 1;  i < this.nodeNumberUnCompr;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeBits;
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
            nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)key & this.nodeMask);  //(int)
        }
        //go through nodes (with compression because (key<<8)+value is inside "int" range now)
        for ( ;  i < this.nodeNumber;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeBits;
            if (0 == nodeIndex) {
                // -> node index is null = unused
                //write current key+value as a "compressed branch" (negative node index)
                //exit immediately because no further nodes and no leaf need to be stored
                nodeArray[nidx] = ((~(int)key) << 8) | byteValue;   //negative  //(int)
                return true;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                if (prevKey == (int)key) {  //(int)
                    if (byteValue > prevVal) {  //putIfGreater
                        nodeArray[nidx] = (nodeIndex ^ prevVal) | byteValue;    //negative
                        return true;
                    }
                    return false;
                }
                //previous and current keys are not equal
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
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevKey & this.nodeMask);
                nodeArray[nidx] = (~(prevKey >>> this.nodeBits) << 8) | prevVal;    //negative
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
            }
            nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)key & this.nodeMask);  //(int)
        }
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        key >>>= this.nodeBits;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            //write current value as a "compressed branch" (negative leaf index)
            //exit immediately because no leaf needs to be stored
            nodeArray[nidx] = ((~(int)key) << 8) | byteValue;   //negative  //(int)
            return true;
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            final int prevKey = (~leafIndex) >> 8;
            final int prevVal = 0xff & leafIndex;
            //previous and current keys are equal (duplicate key)
            if (prevKey == (int)key) {  //(int)
                if (byteValue > prevVal) {  //putIfGreater
                    nodeArray[nidx] = (leafIndex ^ prevVal) | byteValue;    //negative
                    return true;
                }
                return false;
            }
            //previous and current keys are not equal
            //create a new leaf
            if (this.nextLeaf >= this.nextLeafArray) {
                if (this.leafArrays.length <= this.numLeafArrays) {
                    this.leafArrays = Arrays.copyOf(this.leafArrays, this.leafArrays.length << 1);
                }
                final byte[] newLeafArray = new byte[LEAF_ARRAY_SIZE];
                Arrays.fill(newLeafArray, DEFAULT_VALUE);
                this.leafArrays[this.numLeafArrays++] = newLeafArray;
                this.nextLeafArray += LEAF_ARRAY_SIZE;
            }
            leafIndex = this.nextLeaf;
            this.nextLeaf += this.leafSize;
            nodeArray[nidx] = leafIndex;
            //push the previous "compressed branch" further to the leaf
            final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (prevKey & this.leafMask);
            this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT][lidx] = (byte)prevVal;
        }
        final byte[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + ((int)key & this.leafMask);    //(int)
        final byte prevVal = leafArray[lidx];
        if (byteValue > prevVal) {  //putIfGreater
            leafArray[lidx] = (byte)byteValue;
            return true;
        }
        return false;
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#size()
     */
    @Override
    public int size() {
        int size = 0;
        for(int i = 0;  this.nodeSize > i;  ++i) {
            final int nextNodeIndex = this.rootNode[i];
            if (0 > nextNodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                ++size;
            } else if (0 < nextNodeIndex) {
                // -> node index is positive = go to next node
                size += this.sizeRecursion(2, nextNodeIndex);
            }
        }
        return size;
    }
    private int sizeRecursion(final int thisNodeDepth, final int thisNodeIndex) {
        assert 0 < thisNodeIndex : thisNodeIndex;
        int size = 0;
        final int[] nodeArray = this.nodeArrays[thisNodeIndex >>> NODE_ARRAY_SHIFT];
        int nidx = thisNodeIndex & NODE_ARRAY_MASK;
        for(int i = 0;  this.nodeSize > i;  ++i, ++nidx) {
            final int nextNodeIndex = nodeArray[nidx];
            if (0 > nextNodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                ++size;
            } else if (0 < nextNodeIndex) {
                if (thisNodeDepth < this.nodeNumber) {
                    // -> node index is positive = go to next node
                    size += this.sizeRecursion(thisNodeDepth + 1, nextNodeIndex);
                } else {
                    // -> node index is positive = go to leaf node
                    final byte[] leafArray = this.leafArrays[nextNodeIndex >>> LEAF_ARRAY_SHIFT];
                    int lidx = nextNodeIndex & LEAF_ARRAY_MASK;
                    for (int j = 0;  this.leafSize > j;  ++j) {
                        if (DEFAULT_VALUE != leafArray[lidx++]) {
                            ++size;
                        }
                    }
                }
            }
        }
        return size;
    }


    /* (non-Javadoc)
     * @see driftingdroids.model.KeyDepthMap#allocatedBytes()
     */
    @Override
    public final long allocatedBytes() {
        long result = 0;
        for (int i = 0;  i < this.numNodeArrays;  ++i) {
            result += this.nodeArrays[i].length * 4L;
        }
//        final long nodeResult = result;
        for (int i = 0;  i < this.numLeafArrays;  ++i) {
            result += this.leafArrays[i].length;
        }
//        System.out.println("getBytesAllocated TrieMapByte  nodes = " + nodeResult);
//        System.out.println("getBytesAllocated TrieMapByte leaves = " + (result - nodeResult));
        return result;
    }

}
