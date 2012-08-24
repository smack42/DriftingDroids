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

import java.util.Arrays;



/**
 * This class is a minimal <tt>Map&ltK,V&gt</tt> implementation for primitive
 * <tt>int</tt> or <tt>long</tt> keys K and unsigned <tt>byte</tt> values V,
 *  based on a trie (prefix tree) data structure.
 * <p>
 * The aim is to balance a fast recognition of duplicate keys
 * and a compact storage of data.
 */
public final class TrieMapUByte {
    
    private static final byte DEFAULT_VALUE = -1;   //unsigned byte: 255
    
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
    
    private final int nodeBits, leafBits;
    private final int nodeNumber, nodeNumberUnCompr, nodeSize, nodeMask;
    private final int leafShift, leafSize, leafMask;
    
    
    
    /**
     * Constructs an empty TrieMapUByte that is tuned to the expected bit-size of keys.
     * 
     * @param keyBits the maximum number of bits used by any key that will be put into the map.
     * (e.g. specify 32 if your keys are of type <tt>int</tt>, or specify a lower number
     * if you are sure that your application uses only a subset of all <tt>int</tt> keys)
     */
    public TrieMapUByte(final int keyBits) {
        this.nodeBits = 4;  //tuning parameter: number of value bits per internal node
        this.leafBits = 4;  //tuning parameter: number of value bits per leaf
        
        this.nodeNumber = (keyBits - this.leafBits + (this.nodeBits - 1)) / this.nodeBits;
        this.nodeNumberUnCompr = (keyBits + 8 - 31 + (this.nodeBits - 1)) / this.nodeBits;
        this.nodeSize = 1 << this.nodeBits;
        this.nodeMask = this.nodeSize - 1;
        this.leafShift = this.leafBits;
        this.leafSize = 1 << this.leafShift;
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
    
    
    
    /**
     * Adds the specified pair of <tt>int</tt> key and unsigned <tt>byte</tt> value to this map,
     * if the key is not already present or if the new value is less than the previously stored value.
     * 
     * @param key to be put into this map
     * @param value to be associated with this key in this map (treated as unsigned byte: 0 <= value <= 255)
     * @return <code>true</code> if this map did not already contain the specified key or if the key
     * had already been present and the new value is less than the previously stored value.
     */
    public final boolean putIfLess(int key, final byte value) {
        final int intVal = 0xff & value;    //unsigned byte: 0 <= intVal <= 255
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
                nodeArray[nidx] = ((~key) << 8) | intVal;   //negative
                return true;    //added
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                //put new value if it's less than previous value.
                //exit immediately if new value is equal or greater than previous value
                if (prevKey == key) {
                    if (intVal < prevVal) {
                        nodeArray[nidx] = (nodeIndex ^ prevVal) | intVal;   //negative
                        return true;    //added
                    } else {
                        return false;   //not added
                    }
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
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevKey & this.nodeMask);
                nodeArray[nidx] = (~(prevKey >>> this.nodeBits) << 8) | intVal; //negative
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
            nodeArray[nidx] = ((~key) << 8) | intVal;   //negative
            return true;    //added
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            final int prevKey = (~leafIndex) >> 8;
            final int prevVal = 0xff & leafIndex;
            //previous and current keys are equal (duplicate key)
            //put new value if it's less than previous value.
            //exit immediately if new value is equal or greater than previous value
            if (prevKey == key) {
                if (intVal < prevVal) {
                    nodeArray[nidx] = (leafIndex ^ prevVal) | intVal;   //negative
                    return true;    //added
                } else {
                    return false;   //not added
                }
            }
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
        final int prevVal = 0xff & leafArray[lidx];
        if (intVal < prevVal) {
            leafArray[lidx] = value;
            return true;    //added
        } else {
            return false;   //not added
        }
    }
    
    
    
    /**
     * Adds the specified pair of <tt>long</tt> key and unsigned <tt>byte</tt> value to this map,
     * if the key is not already present or if the new value is less than the previously stored value.
     * 
     * @param key to be put into this map
     * @param value to be associated with this key in this map (treated as unsigned byte: 0 <= value <= 255)
     * @return <code>true</code> if this map did not already contain the specified key or if the key
     * had already been present and the new value is less than the previously stored value.
     */
    //this method is copy&paste from putIfLess(int,byte) with only a few (int) casts
    //added where required, those lines are marked with //(int)
    public final boolean putIfLess(long key, final byte value) {
        final int intVal = 0xff & value;    //unsigned byte: 0 <= intVal <= 255
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
                nodeArray[nidx] = ((~(int)key) << 8) | intVal;  //negative  //(int)
                return true;    //added
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final int prevVal = 0xff & nodeIndex;
                //previous and current keys are equal (duplicate key)
                //put new value if it's less than previous value.
                //exit immediately if new value is equal or greater than previous value
                if (prevKey == (int)key) {  //(int)
                    if (intVal < prevVal) {
                        nodeArray[nidx] = (nodeIndex ^ prevVal) | intVal;   //negative
                        return true;    //added
                    } else {
                        return false;   //not added
                    }
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
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (prevKey & this.nodeMask);
                nodeArray[nidx] = (~(prevKey >>> this.nodeBits) << 8) | intVal; //negative
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
            nodeArray[nidx] = ((~(int)key) << 8) | intVal;  //negative  //(int)
            return true;    //added
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            final int prevKey = (~leafIndex) >> 8;
            final int prevVal = 0xff & leafIndex;
            //previous and current keys are equal (duplicate key)
            //put new value if it's less than previous value.
            //exit immediately if new value is equal or greater than previous value
            if (prevKey == (int)key) {  //(int)
                if (intVal < prevVal) {
                    nodeArray[nidx] = (leafIndex ^ prevVal) | intVal;   //negative
                    return true;    //added
                } else {
                    return false;   //not added
                }
            }
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
        final int prevVal = 0xff & leafArray[lidx];
        if (intVal < prevVal) {
            leafArray[lidx] = value;
            return true;    //added
        } else {
            return false;   //not added
        }
    }
    
    
    
    /**
     * Returns the value to which the specified <tt>int</tt> key is mapped, or DEFAULT_VALUE (0xff)
     * if this map contains no mapping for the key.
     * 
     * @param key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or DEFAULT_VALUE (0xff)
     * if this map contains no mapping for the key
     */
    public final byte get(int key) {
        //root node
        int[] nodeArray = this.rootNode;
        int nidx = key & this.nodeMask;
        int nodeIndex, i;   //used by both for() loops
        //go through nodes (without compression because (key<<8)+value is greater than "int")
        for (i = 1;  i < this.nodeNumberUnCompr;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeBits;
            if (0 == nodeIndex) {
                return DEFAULT_VALUE;
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
                return DEFAULT_VALUE;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final byte prevVal = (byte)nodeIndex;
                return ((prevKey == key) ? prevVal : DEFAULT_VALUE);
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                nidx = (nodeIndex & NODE_ARRAY_MASK) + (key & this.nodeMask);
            }
        }
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        key >>>= this.nodeBits;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            return DEFAULT_VALUE;
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            final int prevKey = (~leafIndex) >> 8;
            final byte prevVal = (byte)leafIndex;
            return ((prevKey == key) ? prevVal : DEFAULT_VALUE);
        }
        final byte[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + (key & this.leafMask);
        return leafArray[lidx];
    }
    
    
    
    /**
     * Returns the value to which the specified <tt>long</tt> key is mapped, or DEFAULT_VALUE (0xff)
     * if this map contains no mapping for the key.
     * 
     * @param key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or DEFAULT_VALUE (0xff)
     * if this map contains no mapping for the key
     */
    //this method is copy&paste from get(int) with only a few (int) casts
    //added where required, those lines are marked with //(int)
    public final byte get(long key) {
        //root node
        int[] nodeArray = this.rootNode;
        int nidx = (int)key & this.nodeMask;    //(int)
        int nodeIndex, i;   //used by both for() loops
        //go through nodes (without compression because (key<<8)+value is greater than "int")
        for (i = 1;  i < this.nodeNumberUnCompr;  ++i) {
            nodeIndex = nodeArray[nidx];
            key >>>= this.nodeBits;
            if (0 == nodeIndex) {
                return DEFAULT_VALUE;
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
                return DEFAULT_VALUE;
            } else if (0 > nodeIndex) {
                // -> node index is negative = used by a single "compressed branch"
                final int prevKey = (~nodeIndex) >> 8;
                final byte prevVal = (byte)nodeIndex;
                return ((prevKey == (int)key) ? prevVal : DEFAULT_VALUE);   //(int)
            } else {
                // -> node index is positive = go to next node
                nodeArray = this.nodeArrays[nodeIndex >>> NODE_ARRAY_SHIFT];
                nidx = (nodeIndex & NODE_ARRAY_MASK) + ((int)key & this.nodeMask);  //(int)
            }
        }
        //get leaf (with compression)
        int leafIndex = nodeArray[nidx];
        key >>>= this.nodeBits;
        if (0 == leafIndex) {
            // -> leaf index is null = unused
            return DEFAULT_VALUE;
        } else if (0 > leafIndex) {
            // -> leaf index is negative = used by a single "compressed branch"
            final int prevKey = (~leafIndex) >> 8;
            final byte prevVal = (byte)leafIndex;
            return ((prevKey == (int)key) ? prevVal : DEFAULT_VALUE);   //(int)
        }
        final byte[] leafArray = this.leafArrays[leafIndex >>> LEAF_ARRAY_SHIFT];
        final int lidx = (leafIndex & LEAF_ARRAY_MASK) + ((int)key & this.leafMask);    //(int)
        return leafArray[lidx];
    }
    
    
    
    public final long getBytesAllocated() {
        long result = 0;
        for (int i = 0;  i < this.numNodeArrays;  ++i) {
            result += this.nodeArrays[i].length << 2;
        }
        for (int i = 0;  i < this.numLeafArrays;  ++i) {
            result += this.leafArrays[i].length;
        }
        return result;
    }
    
    
    
    /**
     * A simple test case for this class.
     * @param args not used
     */
    public static final void main(String[] args) {
        final TrieMapUByte t = new TrieMapUByte(28);
        for (long i = 0;  i < 0x04000000L;  ++i) {
            final byte get0 = t.get((int)i);
            final boolean putA = t.putIfLess((int)i, (byte)0x42);
            final byte getA = t.get((int)i);
            final boolean putB = t.putIfLess((int)i, (byte)  42);
            final byte getB = t.get((int)i);
            final boolean putC = t.putIfLess((int)i, (byte)0x42);
            final byte getC = t.get((int)i);
            if ((DEFAULT_VALUE != get0) || (true != putA) || ((byte)0x42 != getA)
                    || (true != putB) || ((byte)42 != getB)
                    || (false != putC) || ((byte)42 != getC)) {
                System.out.println("TrieMapUByte BUG! " + i);   //debugger breakpoint here
            }
        }
        System.out.println("TrieMapUByte TEST done.");
    }

}
