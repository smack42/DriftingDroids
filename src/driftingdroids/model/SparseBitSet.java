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



// you're looking at a reduced and optimized version of this class:
// http://code.google.com/p/ener-j/source/browse/trunk/ener-j/src/org/enerj/core/SparseBitSet.java



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




package driftingdroids.model;



public class SparseBitSet {
    
    private static final int[] BIT_POS = {
        0x00000001, 0x00000002, 0x00000004, 0x00000008,
        0x00000010, 0x00000020, 0x00000040, 0x00000080
    };

    /** nodeSize * nodeSize * 8. This is the number of bytes under a single root entry. */
    private final long nodeSizeSquared8;

    /** The number of bytes in a leaf node. nodeSize * 8. */
    private final int leafNodeByteSize;
    
    /** The root node of the tree. */
    private final RootNode rootNode;
    

    public SparseBitSet(final int nodeSize) {
        this.leafNodeByteSize = nodeSize * 8;
        this.nodeSizeSquared8 = (long)nodeSize * this.leafNodeByteSize;
        this.rootNode = new RootNode(nodeSize);
    }
    

    public SparseBitSet() {
        this(1024);
    }
    
    
    public boolean add(final long bitIndex) {
        final long byteIndex = bitIndex >>> 3;
        final byte[] node = this.getLeafNode(byteIndex);
        final int leafIndex = (int)(byteIndex % this.leafNodeByteSize);
        final int oldBits = node[leafIndex];
        final int newBits = oldBits | BIT_POS[(int)(bitIndex) & 7];
        final boolean bitHasBeenAdded = (oldBits != newBits);
        if (true == bitHasBeenAdded) {
            node[leafIndex] = (byte)newBits;
        }
        return bitHasBeenAdded;
    }
    
    
    public boolean add(final int bitIndex) {
        final int byteIndex = bitIndex >>> 3;
        final byte[] node = this.getLeafNode(byteIndex);
        final int leafIndex = byteIndex % this.leafNodeByteSize;
        final int oldBits = node[leafIndex];
        final int newBits = oldBits | BIT_POS[bitIndex & 7];
        final boolean bitHasBeenAdded = (oldBits != newBits);
        if (true == bitHasBeenAdded) {
            node[leafIndex] = (byte)newBits;
        }
        return bitHasBeenAdded;
    }
    
    
    
    
    private byte[] getLeafNode(final long byteIndex) {
        final int rootIndex = (int)(byteIndex / this.nodeSizeSquared8);
        final SecondLevelNode secondLevelNode = this.rootNode.get(rootIndex);
        final int secondLevelIndex = (int)((byteIndex - (rootIndex * this.nodeSizeSquared8)) / this.leafNodeByteSize);
        final byte[] leafNode = secondLevelNode.get(secondLevelIndex);
        return leafNode;
    }
    
    
    private byte[] getLeafNode(final int byteIndex) {
        final int rootIndex = byteIndex / (int)this.nodeSizeSquared8;
        final SecondLevelNode secondLevelNode = this.rootNode.get(rootIndex);
        final int secondLevelIndex = (byteIndex - (rootIndex * (int)this.nodeSizeSquared8)) / this.leafNodeByteSize;
        final byte[] leafNode = secondLevelNode.get(secondLevelIndex);
        return leafNode;
    }
    
    
    
    
    private final class RootNode {
        private final SecondLevelNode[] allNodes;
        public RootNode(final int nodeSize) {
            this.allNodes = new SecondLevelNode[nodeSize];
            for (int i = 0;  i < this.allNodes.length;  ++i) {
                this.allNodes[i] = new SecondLevelNode(nodeSize);
            }
        }
        public SecondLevelNode get(final int index) {
            return this.allNodes[index];
        }
    }
    
    
    private final class SecondLevelNode {
        private final byte[][] allNodes;
        public SecondLevelNode(final int nodeSize) {
            this.allNodes = new byte[nodeSize][];
        }
        public byte[] get(final int index) {
            final byte[] oldNode = this.allNodes[index];
            if (oldNode != null) {
                return oldNode;
            } else {
                final byte[] newNode = new byte[leafNodeByteSize];
                this.allNodes[index] = newNode;
                return newNode;
            }
        }
    }
}

