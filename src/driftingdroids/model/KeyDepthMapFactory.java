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

/**
 * Factory that creates instances of KeyDepthMap.
 */
public class KeyDepthMapFactory {

    private static Class<? extends KeyDepthMap> defaultClazz = KeyDepthMapTrieSpecial.class;


    /**
     * Set this factory's default implementation class of KeyDepthMap.
	 
     * @param clazz the implementation class of KeyDepthMap
     */
    public static void setDefaultClass(Class<? extends KeyDepthMap> clazz) {
        defaultClazz = clazz;
    }


    /**
     * Creates a new instance of KeyDepthMap.
     * Uses this factory's default implementation class of KeyDepthMap.
     * 
     * @param board the board that is to be solved
     * @return
     */
    public static KeyDepthMap newInstance(Board board) {
        return newInstance(board, defaultClazz);
    }


    /**
     * Creates a new instance of KeyDepthMap.
     * 
     * @param board the board that is to be solved
     * @param clazz the implementation class of KeyDepthMap
     * @return a new instance of KeyDepthMap
     */
    public static KeyDepthMap newInstance(Board board, Class<? extends KeyDepthMap> clazz) {
        if (KeyDepthMapTrieGeneric.class.equals(clazz)) {
            return new KeyDepthMapTrieGeneric(Math.max(12, board.getNumRobots() * board.sizeNumBits));
        } else if (KeyDepthMapTrieSpecial.class.equals(clazz)) {
            return KeyDepthMapTrieSpecial.createInstance(board, true);
        } else {
            throw new IllegalArgumentException("unknown KeyDepthMap class: " + clazz);
        }
    }

}
