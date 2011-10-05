/*******************************************************************************
 * Copyright 2000, 2006 Visual Systems Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License version 2
 * which accompanies this distribution in a file named "COPYING".
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *      
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *      
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *******************************************************************************/
// Ener-J
// Copyright 2004 Visual Systems Corporation
// $Header: /cvsroot/ener-j/ener-j/src/org/enerj/core/SparseBitSet.java,v 1.6 2006/05/07 17:57:27 dsyrstad Exp $

package org.enerj.core;

import java.util.Arrays;
import java.util.NoSuchElementException;

//import org.enerj.annotations.Persist;

/**
 * Sparse bitset which supports very large bitsets in a semi-sparse format.
 * Includes an efficient iterator which only interates over the set bits.
 * <p>
 * The bitset is implemented as a three-level tree of array nodes with
 * nodeSize elements on each node. The top level (root) is a single array of 
 * nodeSize references to the second-level nodes. The second-level nodes, in turn,
 * point to third-level nodes which contain the bits represented as an array of longs.
 * For example, a nodeSize of 4,096 (32KB) can support a maximum of
 * 4,398,046,511,104 (4,096^3 * 8*8 or 4 trillion) bits.
 * However, to set the just the first and four trillionth bit, only five
 * nodes need to be allocated.
 * <p>
 *
 * @version $Id: SparseBitSet.java,v 1.6 2006/05/07 17:57:27 dsyrstad Exp $
 * @author <a href="mailto:dsyrstad@ener-j.org">Dan Syrstad</a>
 */
//@Persist
public class SparseBitSet implements Cloneable
{
    /** Bit mask indexed by bit position. */
    private static final long[] BIT_POS = {
        0x0000000000000001L /* position  0 */,     0x0000000000000002L /* position  1 */,
        0x0000000000000004L /* position  2 */,     0x0000000000000008L /* position  3 */,
        0x0000000000000010L /* position  4 */,     0x0000000000000020L /* position  5 */,
        0x0000000000000040L /* position  6 */,     0x0000000000000080L /* position  7 */,
        0x0000000000000100L /* position  8 */,     0x0000000000000200L /* position  9 */,
        0x0000000000000400L /* position 10 */,     0x0000000000000800L /* position 11 */,
        0x0000000000001000L /* position 12 */,     0x0000000000002000L /* position 13 */,
        0x0000000000004000L /* position 14 */,     0x0000000000008000L /* position 15 */,
        0x0000000000010000L /* position 16 */,     0x0000000000020000L /* position 17 */,
        0x0000000000040000L /* position 18 */,     0x0000000000080000L /* position 19 */,
        0x0000000000100000L /* position 20 */,     0x0000000000200000L /* position 21 */,
        0x0000000000400000L /* position 22 */,     0x0000000000800000L /* position 23 */,
        0x0000000001000000L /* position 24 */,     0x0000000002000000L /* position 25 */,
        0x0000000004000000L /* position 26 */,     0x0000000008000000L /* position 27 */,
        0x0000000010000000L /* position 28 */,     0x0000000020000000L /* position 29 */,
        0x0000000040000000L /* position 30 */,     0x0000000080000000L /* position 31 */,
        0x0000000100000000L /* position 32 */,     0x0000000200000000L /* position 33 */,
        0x0000000400000000L /* position 34 */,     0x0000000800000000L /* position 35 */,
        0x0000001000000000L /* position 36 */,     0x0000002000000000L /* position 37 */,
        0x0000004000000000L /* position 38 */,     0x0000008000000000L /* position 39 */,
        0x0000010000000000L /* position 40 */,     0x0000020000000000L /* position 41 */,
        0x0000040000000000L /* position 42 */,     0x0000080000000000L /* position 43 */,
        0x0000100000000000L /* position 44 */,     0x0000200000000000L /* position 45 */,
        0x0000400000000000L /* position 46 */,     0x0000800000000000L /* position 47 */,
        0x0001000000000000L /* position 48 */,     0x0002000000000000L /* position 49 */,
        0x0004000000000000L /* position 50 */,     0x0008000000000000L /* position 51 */,
        0x0010000000000000L /* position 52 */,     0x0020000000000000L /* position 53 */,
        0x0040000000000000L /* position 54 */,     0x0080000000000000L /* position 55 */,
        0x0100000000000000L /* position 56 */,     0x0200000000000000L /* position 57 */,
        0x0400000000000000L /* position 58 */,     0x0800000000000000L /* position 59 */,
        0x1000000000000000L /* position 60 */,     0x2000000000000000L /* position 61 */,
        0x4000000000000000L /* position 62 */,     0x8000000000000000L /* position 63 */,
    };

    /** Number of elements in each node. */
    final private int mNodeSize;
    /** mNodeSize squared * 64. This is the number of bits under a single root entry. */
    final private long mNodeSizeSquared64;
    /** The number of bits in a leaf node. mNodeSize * 64. */
    final private int mLeafNodeBitSize;
    /** The maximum number of bits. */
    final private long mMaxSize;
    /** The root node of the tree. */
    private RootNode mRootNode;
    

    /**
     * Constructs a new SparseBitSet with the specified node size.
     * The maximum size of the bitset is aNodeSize^3 * 64.
     * 
     * @param aNodeSize the size of a single node in the tree. See class
     *  description for more information.
     */
    public SparseBitSet(int aNodeSize)
    {
        mNodeSize = aNodeSize;
        mLeafNodeBitSize = mNodeSize * 64;
        mNodeSizeSquared64 = (long)mNodeSize * (long)mLeafNodeBitSize;
        mRootNode = new RootNode(mNodeSize);
        mMaxSize = mNodeSizeSquared64 * mNodeSize;
    }
    

    /**
     * Constructs a new SparseBitSet with the default node size (1024).
     * This node size supports a maximum of 68,719,476,736 (about 68 billion) elements.
     */
    public SparseBitSet()
    {
        this(1024);
    }
    

    /**
     * Gets the leaf node for the corresponding bit index. Allocates new nodes if
     * shouldAllocate is true. If the size of the list changes, mModCount is
     * incremented.
     *
     * @param aBitIndex the bit index whose node will be retrieved.
     * @param shouldAllocate if true, nodes will be allocated if they don't
     *  exist. If false and nodes don't exist, null will be returned.
     *
     * @return the LeafNode corresponding to aBitIndex, or null if shouldAllocate
     *  is false and a node does not exist.
     *
     * @throws IndexOutOfBoundsException if index is out of range (aBitIndex < 0 ||
     *  (aBitIndex >= getMaxSize() && !shouldAllocate)).
     */
    protected LeafNode getLeafNodeForIndex(long aBitIndex, boolean shouldAllocate)
    {
        if (aBitIndex < 0 || (!shouldAllocate && aBitIndex >= mMaxSize)) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + aBitIndex);
        }
        
        int rootIndex = (int)(aBitIndex / mNodeSizeSquared64);
        SecondLevelNode secondLevelNode = mRootNode.get(rootIndex);
        if (secondLevelNode == null) {
            if (shouldAllocate) {
                secondLevelNode = new SecondLevelNode(mNodeSize);
                mRootNode.set(rootIndex, secondLevelNode);
            }
            else {
                return null;
            }
        }
        
        int secondLevelIndex = (int)((aBitIndex - (rootIndex * mNodeSizeSquared64)) / mLeafNodeBitSize);
        LeafNode leafNode = secondLevelNode.get(secondLevelIndex);
        if (leafNode == null) {
            if (shouldAllocate) {
                leafNode = new LeafNode(mNodeSize);
                secondLevelNode.set(secondLevelIndex, leafNode);
            }
            else {
                return null;
            }
        }

        return leafNode;
    }


    /**
     * Gets the maximum number of bits that can be contained in the set.
     *
     * @return the maximum number of bits that can be contained in the set.
     */
    public long getMaxSize()
    {
        return mMaxSize;
    }


    /**
     * Sets the bit at aBitIndex.
     *
     * @param aBitIndex the index of the bit to set.
     *
     * @throws IndexOutOfBoundsException if index is out of range (aBitIndex < 0 ||
     *  aBitIndex >= getMaxSize()).
     */
    public void set(long aBitIndex)
    {
        set(aBitIndex, true);
    }


    /**
     * Clears the bit at aBitIndex.
     *
     * @param aBitIndex the index of the bit to clear.
     *
     * @throws IndexOutOfBoundsException if index is out of range (aBitIndex < 0 ||
     *  aBitIndex >= getMaxSize()).
     */
    public void clear(long aBitIndex)
    {
        set(aBitIndex, false);
    }


    /**
     * Sets or clears the bit at aBitIndex.
     *
     * @param aBitIndex the index of the bit to set or clear.
     * @param isSet true if the bit should be set, false to clear.
     *
     * @throws IndexOutOfBoundsException if index is out of range (aBitIndex < 0 ||
     *  aBitIndex >= getMaxSize()).
     */
    public void set(long aBitIndex, boolean isSet)
    {
        LeafNode node = getLeafNodeForIndex(aBitIndex, isSet);
        if (node == null && !isSet) {
            // Bit (and node) is already clear
            return;
        }

        // 64 = bits in a long
        int leafIndex = (int)(aBitIndex % mLeafNodeBitSize) / 64;
        long bitPos = BIT_POS[ (int)(aBitIndex % 64L) ];
        long bits = node.get(leafIndex);
        //System.out.println("Setting aBitIndex=" + aBitIndex + " leafIndex=" + leafIndex + " bitPos=" + Long.toHexString(bitPos) + " bits=" + Long.toHexString(bits) + (isSet ? " set=" + Long.toHexString(bits | bitPos) : " unset=" + Long.toHexString(bits & ~bitPos)));
        if (isSet) {
            node.set(leafIndex, bits | bitPos);
        }
        else {
            node.set(leafIndex, bits & ~bitPos);
            // If node is now all clear, remove this node, and possibly second level too.
            if (node.isClear()) {
                int rootIndex = (int)(aBitIndex / mNodeSizeSquared64);
                SecondLevelNode secondLevelNode = mRootNode.get(rootIndex);
                int secondLevelIndex = (int)((aBitIndex - (rootIndex * mNodeSizeSquared64)) / mLeafNodeBitSize);
                // Dereference leaf node
                secondLevelNode.set(secondLevelIndex, null);

                // Is second level node now clear?
                if (secondLevelNode.isClear()) {
                    // Dereference second level node
                    mRootNode.set(rootIndex, null);
                }
            }
        }
    }


    /**
     * Gets the bit at aBitIndex.
     *
     * @param aBitIndex the index of the bit to get.
     *
     * @return true if the bit is set, else false.
     *
     * @throws IndexOutOfBoundsException if index is out of range (aBitIndex < 0 ||
     *  aBitIndex >= getMaxSize()).
     */
    public boolean get(long aBitIndex)
    {
        LeafNode node = getLeafNodeForIndex(aBitIndex, false);
        if (node == null) {
            return false;
        }

        // 64 = bits in a long
        int leafIndex = (int)(aBitIndex % mLeafNodeBitSize) / 64;
        long bitPos = BIT_POS[ (int)(aBitIndex % 64L) ];
        return (node.get(leafIndex) & bitPos) == bitPos;
    }
    
    
    /**
     * 
     * implemented by combining the functions get(long aBitIndex) and
     * set(long aBitIndex, boolean isSet)
     * 
     * @param aBitIndex the index of the bit to set
     * 
     * @return bitHasBeenAdded: true if the bit has been set by this function call,
     * false if the bit had been set before and this function has not changed the SparseBitSet.
     */
    public boolean add(final long aBitIndex) {
        final LeafNode node = getLeafNodeForIndex(aBitIndex, true);
        final int leafIndex = (int)(aBitIndex % mLeafNodeBitSize) / 64;
        final long bitPos = BIT_POS[ (int)(aBitIndex % 64L) ];
        final long bits = node.get(leafIndex);
        
        final boolean bitHasBeenAdded = ((bits & bitPos) == 0);
        if (true == bitHasBeenAdded) {
            node.set(leafIndex, bits | bitPos);
        }
        return bitHasBeenAdded;
    }


    /**
     * Determines whether this bit set is completely clear (no bits set).
     *
     * @return true if no bits are set, else false.
     */
    public boolean isClear()
    {
        // If root node has no second-level nodes, we know that no bits are set. Also,
        // when bits are cleared, leaf and second-level nodes are removed as they become
        // empty. So an empty root node always is the sole indicator of whether any bits are set.
        return mRootNode.isClear();
    }


    /**
     * Clears all bits in the bit set that are currently set.
     */
    public void clear()
    {
        // The previous tree is GCed.
        mRootNode = new RootNode(mNodeSize);
    }


    /**
     * Gets the index of the next true bit set in the bit set, starting from aStartingBitIndex.
     * This method quickly skips over large chunks of unset bits, hence it is much faster than
     * just incrementing a bit index and calling get(index).
     *
     * @param aStartingBitIndex the bit index from which the search for the next set bit begins.
     *
     * @return the index of the set bit in the bit set.
     *
     * @throws NoSuchElementException if there is no other set bit.
     *
     * @throws IndexOutOfBoundsException if index is out of range (aBitIndex < 0 ||
     *  (aBitIndex >= getMaxSize() && !shouldAllocate)).
     */
    public long getNextSetBitIndex(long aStartingBitIndex) throws NoSuchElementException
    {
        if (aStartingBitIndex < 0 || aStartingBitIndex >= getMaxSize()) {
            throw new IndexOutOfBoundsException("Bad index " + aStartingBitIndex);
        }

        int rootIndex = (int)(aStartingBitIndex / mNodeSizeSquared64);
        int secondLevelIndex = (int)((aStartingBitIndex - (rootIndex * mNodeSizeSquared64)) / mLeafNodeBitSize);
        int leafIndex = (int)(aStartingBitIndex % mLeafNodeBitSize) / 64;
        int bitIdx = (int)(aStartingBitIndex % 64L);
        for (; rootIndex < mNodeSize; ++rootIndex, secondLevelIndex = 0, leafIndex = 0, bitIdx = 0) {
            SecondLevelNode secondLevelNode = mRootNode.get(rootIndex);
            if (secondLevelNode == null) {
                continue;
            }

            for (; secondLevelIndex < mNodeSize; ++secondLevelIndex, leafIndex = 0, bitIdx = 0) {
                LeafNode leafNode = secondLevelNode.get(secondLevelIndex);
                if (leafNode == null) {
                    continue;
                }

                for (; leafIndex < mNodeSize; ++leafIndex, bitIdx = 0) {
                    long bits = leafNode.get(leafIndex);
                    if (bits == 0) {
                        continue;
                    }

                    for (; bitIdx < 64; ++bitIdx) {
                        long bitPos = BIT_POS[bitIdx];
                        if ((bits & bitPos) != 0) {
                            // Found one - calculate the global bit index.
                            long returnBitIndex = ((long)rootIndex * mNodeSizeSquared64) +
                                    ((long)secondLevelIndex * mLeafNodeBitSize) +
                                    ((long)leafIndex * 64L) + (long)bitIdx;
                            return returnBitIndex;
                        }
                    }
                }
            }
        }

        throw new NoSuchElementException("End of bitset reached");
    }

    

    /**
     * Gets the number of bits set in the set.
     *
     * @return the number of bits set.
     */
    public long getNumBitsSet() 
    {
        long numSet = 0;

        // The current implementation iterates over all leaf nodes to accumulate the count. 
        for (SecondLevelNode secondLevelNode : mRootNode.mNodes) {
            if (secondLevelNode != null) {
                for (LeafNode leafNode : secondLevelNode.mNodes) {
                    if (leafNode != null) {
                        for (long bits : leafNode.getAllBits()) {
                            for (; bits != 0; bits >>>= 1) {
                                if ((bits & 1) == 1) {
                                    ++numSet;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return numSet;
    }
    

    /**
     * Creates an iterator for this bit set which iterates over all "true" bits.
     *
     * @return a SparseBitSet.Iterator positioned on the first true bit in the set.
     */
    public Iterator getIterator()
    {
        return new Iterator(this);
    }


//    /**
//     * {@inheritDoc}
//     */
//    public int hashCode()
//    {
//        int hashCode = ((int)mMaxSize + (int)EnerJImplementation.getEnerJObjectId(mRootNode)) ^ 292876537;
//        return hashCode;
//    }


    /**
     * {@inheritDoc}
     */
    public boolean equals(Object anObject)
    {
        if (anObject == this) {
            return true;
        }

        if ( !(anObject instanceof SparseBitSet)) {
            return false;
        }

        SparseBitSet otherSet = (SparseBitSet)anObject;
        if (this.mMaxSize != otherSet.mMaxSize) {
            return false;
        }
        
        for (long idx = 0; idx < mMaxSize; idx += mLeafNodeBitSize) {
            LeafNode leaf = this.getLeafNodeForIndex(idx, false);
            LeafNode otherLeaf = otherSet.getLeafNodeForIndex(idx, false);
            if (leaf == null || otherLeaf == null || !leaf.equals(otherLeaf)) {
                return false;
            }
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getClass().getName() + ": size=" + mMaxSize;
    }


    /**
     * {@inheritDoc}
     */
    public Object clone() throws CloneNotSupportedException
    {
        SparseBitSet clone = (SparseBitSet)super.clone();
        clone.mRootNode = (RootNode)mRootNode.clone();
        return clone;
    }



    /**
     * Iterator for SparseBitSet. Quickly skips over large chunks of unset bits.
     */
    public static final class Iterator {
        private SparseBitSet mBitSet;
        private long mNextBitIndex = 0;


        private Iterator(SparseBitSet aBitSet) {
            mBitSet = aBitSet;
        }


        /**
         * Gets the index of the next true bit set in the bit set.
         *
         * @return the index of the true bit in the bit set.
         *
         * @throws NoSuchElementException if there is no other set bit.
         */
        public long next() throws NoSuchElementException
        {
            if (mNextBitIndex >= mBitSet.getMaxSize()) {
                throw new NoSuchElementException("End of bitset reached");
            }

            long result = mBitSet.getNextSetBitIndex(mNextBitIndex);
            mNextBitIndex = result + 1;
            return result;
        }


        /**
         * Determines if there are more set bits to get from the iterator.
         *
         * @return true if there are more set bits to get from the iterator, else false.
         */
        public boolean hasNext()
        {
            if (mNextBitIndex >= mBitSet.getMaxSize()) {
                return false;
            }

            try {
                // Scan ahead and save position while we're at it.
                mNextBitIndex = mBitSet.getNextSetBitIndex(mNextBitIndex);
                return true;
            }
            catch (NoSuchElementException e) {
                return false;
            }
        }
    }



    /** 
     * Represents a root node in the tree. This is a separate object so that it
     * is not directly recoginized as an SCO and will be demand-loaded.
     */
//    @Persist
    private static final class RootNode implements Cloneable
    {
        private SecondLevelNode[] mNodes;
        private int mNumNodes = 0;


        RootNode(int mNodeSize)
        {
            mNodes = new SecondLevelNode[mNodeSize];
        }


        SecondLevelNode get(int anIndex)
        {
            return mNodes[anIndex];
        }


        void set(int anIndex, SecondLevelNode aNode)
        {
            if (mNodes[anIndex] != aNode) {
                if (mNodes[anIndex] == null && aNode != null) {
                    // Transitioning from null to non-null.
                    ++mNumNodes;
                }
                else if (mNodes[anIndex] != null && aNode == null) {
                    // Transitioning from non-null to null.
                    --mNumNodes;
                }

                mNodes[anIndex] = aNode;
//                EnerJImplementation.setModified(this);
            }
        }


        /**
         * Determines if this node is clear (has no second level nodes).
         *
         * @return true if clear, false if it has at least one second level node.
         */
        boolean isClear()
        {
            return mNumNodes == 0;
        }


        /**
         * {@inheritDoc}
         */
        public Object clone() throws CloneNotSupportedException
        {
            RootNode clone = (RootNode)super.clone();
            clone.mNodes = new SecondLevelNode[ mNodes.length ];
            System.arraycopy(mNodes, 0, clone.mNodes, 0, mNodes.length);
            return clone;
        }
    }



    /**
     * Represents a second level node in the tree. This is a separate object so that it
     * is not directly recoginized as an SCO and will be demand-loaded.
     */
//    @Persist
    private static final class SecondLevelNode implements Cloneable
    {
        private LeafNode[] mNodes;
        private int mNumLeafs = 0;


        SecondLevelNode(int mNodeSize)
        {
            mNodes = new LeafNode[mNodeSize];
        }


        LeafNode get(int anIndex)
        {
            return mNodes[anIndex];
        }


        void set(int anIndex, LeafNode aLeafNode)
        {
            if (mNodes[anIndex] != aLeafNode) {
                if (mNodes[anIndex] == null && aLeafNode != null) {
                    // Transitioning from null to non-null.
                    ++mNumLeafs;
                }
                else if (mNodes[anIndex] != null && aLeafNode == null) {
                    // Transitioning from non-null to null.
                    --mNumLeafs;
                }

                mNodes[anIndex] = aLeafNode;
//                EnerJImplementation.setModified(this);
            }
        }


        /**
         * Determines if this node is clear (has no leafs).
         *
         * @return true if clear, false if it has at least one leaf.
         */
        boolean isClear()
        {
            return mNumLeafs == 0;
        }


        /**
         * {@inheritDoc}
         */
        public Object clone() throws CloneNotSupportedException
        {
            SecondLevelNode clone = (SecondLevelNode)super.clone();
            clone.mNodes = new LeafNode[ mNodes.length ];
            System.arraycopy(mNodes, 0, clone.mNodes, 0, mNodes.length);
            return clone;
        }
    }



    /**
     * Represents a leaf node in the tree. This is a separate object so that it
     * is not directly recoginized as an SCO and will be demand-loaded.
     */
//    @Persist
    private static final class LeafNode implements Cloneable
    {
        private long[] mBits;
        /** Number of non-zero elements in mBits. */
        private int mNumNonZeroElements = 0;


        LeafNode(int mNodeSize)
        {
            mBits = new long[mNodeSize];
        }


        long get(int anIndex)
        {
            return mBits[anIndex];
        }


        /**
         *  Gets all of the bits in the node for read-only purposes.
         *
         * @return an array of longs representing the bits in this node.
         */
        long[] getAllBits()
        {
            return mBits;
        }


        void set(int anIndex, long someBits)
        {
            if (mBits[anIndex] != someBits) {
                if (mBits[anIndex] == 0 && someBits != 0) {
                    // Transitioning from zero to non-zero.
                    ++mNumNonZeroElements;
                }
                else if (mBits[anIndex] != 0 && someBits == 0) {
                    // Transitioning from non-zero to zero.
                    --mNumNonZeroElements;
                }

                mBits[anIndex] = someBits;
//                EnerJImplementation.setModified(this);
            }
        }


        /**
         * Determines if all bits in this node are clear.
         */
        boolean isClear()
        {
            return mNumNonZeroElements == 0;
        }


        /**
         * {@inheritDoc}
         */
        public boolean equals(Object anObject)
        {
            if (anObject == this) {
                return true;
            }

            if ( !(anObject instanceof LeafNode)) {
                return false;
            }

            LeafNode otherNode = (LeafNode)anObject;
            return Arrays.equals(this.mBits, otherNode.mBits);
        }


        /**
         * {@inheritDoc}
         */
        public Object clone() throws CloneNotSupportedException
        {
            LeafNode clone = (LeafNode)super.clone();
            clone.mBits = new long[ mBits.length ];
            System.arraycopy(mBits, 0, clone.mBits, 0, mBits.length);
            return clone;
        }
    }
}

