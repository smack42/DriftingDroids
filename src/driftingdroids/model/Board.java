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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.Inflater;



public class Board {
    static final ResourceBundle L10N = ResourceBundle.getBundle("driftingdroids-localization-model");   //L10N = Localization
    
    public static final int WIDTH_STANDARD = 16;
    public static final int WIDTH_MIN = 3;
    public static final int WIDTH_MAX = 100;
    public static final int HEIGHT_STANDARD = 16;
    public static final int HEIGHT_MIN = 3;
    public static final int HEIGHT_MAX = 100;
    public static final int SIZE_MAX = 4096; // 12 bits
    public static final int NUMROBOTS_STANDARD = 4;
    
    public static final String[] ROBOT_COLOR_NAMES_SHORT = {    //also used as part of L10N-keys
        "r", "g", "b", "y", "s"
    };
    public static final String[] ROBOT_COLOR_NAMES_LONG = {     //also used as part of L10N-keys
        "red", "green", "blue", "yellow", "silver"
    };
    
    public static final int GOAL_CIRCLE   = 0;
    public static final int GOAL_TRIANGLE = 1;
    public static final int GOAL_SQUARE   = 2;
    public static final int GOAL_HEXAGON  = 3;
    public static final int GOAL_VORTEX   = 4;
    public static final String[] GOAL_SHAPE_NAMES = {
        "circle", "triangle", "square", "hexagon", "vortex"
    };
    
    public static final String[] QUADRANT_NAMES = {
        "1A", "2A", "3A", "4A",
        "1B", "2B", "3B", "4B",
        "1C", "2C", "3C", "4C",
        "1D", "2D", "3D", "4D"
    };
    private static final Board[] QUADRANTS = new Board[16];
    static {
        QUADRANTS[0] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //1A
            .addWall(1, 0, "E")
            .addWall(4, 1, "NW")  .addGoal(4, 1, 0, GOAL_CIRCLE)      //R
            .addWall(1, 2, "NE")  .addGoal(1, 2, 1, GOAL_TRIANGLE)    //G
            .addWall(6, 3, "SE")  .addGoal(6, 3, 3, GOAL_HEXAGON)     //Y
            .addWall(0, 5, "S")
            .addWall(3, 6, "SW")  .addGoal(3, 6, 2, GOAL_SQUARE)      //B
            .addWall(7, 7, "NESW");
        QUADRANTS[1] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //2A
            .addWall(3, 0, "E")
            .addWall(5, 1, "SE")  .addGoal(5, 1, 1, GOAL_HEXAGON)     //G
            .addWall(1, 2, "SW")  .addGoal(1, 2, 0, GOAL_SQUARE)      //R
            .addWall(0, 3, "S")
            .addWall(6, 4, "NW")  .addGoal(6, 4, 3, GOAL_CIRCLE)      //Y
            .addWall(2, 6, "NE")  .addGoal(2, 6, 2, GOAL_TRIANGLE)    //B
            .addWall(7, 7, "NESW");
        QUADRANTS[2] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //3A
            .addWall(3, 0, "E")
            .addWall(5, 2, "SE")  .addGoal(5, 2, 2, GOAL_HEXAGON)     //B
            .addWall(0, 4, "S")
            .addWall(2, 4, "NE")  .addGoal(2, 4, 1, GOAL_CIRCLE)      //G
            .addWall(7, 5, "SW")  .addGoal(7, 5, 0, GOAL_TRIANGLE)    //R
            .addWall(1, 6, "NW")  .addGoal(1, 6, 3, GOAL_SQUARE)      //Y
            .addWall(7, 7, "NESW");
        QUADRANTS[3] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //4A
            .addWall(3, 0, "E")
            .addWall(6, 1, "SW")  .addGoal(6, 1, 2, GOAL_CIRCLE)      //B
            .addWall(1, 3, "NE")  .addGoal(1, 3, 3, GOAL_TRIANGLE)    //Y
            .addWall(5, 4, "NW")  .addGoal(5, 4, 1, GOAL_SQUARE)      //G
            .addWall(2, 5, "SE")  .addGoal(2, 5, 0, GOAL_HEXAGON)     //R
            .addWall(7, 5, "SE")  .addGoal(7, 5, -1, GOAL_VORTEX)     //W*
            .addWall(0, 6, "S")
            .addWall(7, 7, "NESW");
        QUADRANTS[4] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //1B
            .addWall(4, 0, "E")
            .addWall(6, 1, "SE")  .addGoal(6, 1, 3, GOAL_HEXAGON)     //Y
            .addWall(1, 2, "NW")  .addGoal(1, 2, 1, GOAL_TRIANGLE)    //G
            .addWall(0, 5, "S")
            .addWall(6, 5, "NE")  .addGoal(6, 5, 2, GOAL_SQUARE)      //B
            .addWall(3, 6, "SW")  .addGoal(3, 6, 0, GOAL_CIRCLE)      //R
            .addWall(7, 7, "NESW");
        QUADRANTS[5] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //2B
            .addWall(4, 0, "E")
            .addWall(2, 1, "NW")  .addGoal(2, 1, 3, GOAL_CIRCLE)      //Y
            .addWall(6, 3, "SW")  .addGoal(6, 3, 2, GOAL_TRIANGLE)    //B
            .addWall(0, 4, "S")
            .addWall(4, 5, "NE")  .addGoal(4, 5, 0, GOAL_SQUARE)      //R
            .addWall(1, 6, "SE")  .addGoal(1, 6, 1, GOAL_HEXAGON)     //G
            .addWall(7, 7, "NESW");
        QUADRANTS[6] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //3B
            .addWall(3, 0, "E")
            .addWall(1, 1, "SW")  .addGoal(1, 1, 0, GOAL_TRIANGLE)    //R
            .addWall(6, 2, "NE")  .addGoal(6, 2, 1, GOAL_CIRCLE)      //G
            .addWall(2, 4, "SE")  .addGoal(2, 4, 2, GOAL_HEXAGON)     //B
            .addWall(0, 5, "S")
            .addWall(7, 5, "NW")  .addGoal(7, 5, 3, GOAL_SQUARE)      //Y
            .addWall(7, 7, "NESW");
        QUADRANTS[7] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //4B
            .addWall(4, 0, "E")
            .addWall(2, 1, "SE")  .addGoal(2, 1, 0, GOAL_HEXAGON)     //R
            .addWall(1, 3, "SW")  .addGoal(1, 3, 1, GOAL_SQUARE)      //G
            .addWall(0, 4, "S")
            .addWall(6, 4, "NW")  .addGoal(6, 4, 3, GOAL_TRIANGLE)    //Y
            .addWall(5, 6, "NE")  .addGoal(5, 6, 2, GOAL_CIRCLE)      //B
            .addWall(3, 7, "SE")  .addGoal(3, 7, -1, GOAL_VORTEX)     //W*
            .addWall(7, 7, "NESW");
        QUADRANTS[8] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //1C
            .addWall(1, 0, "E")
            .addWall(3, 1, "NW")  .addGoal(3, 1, 1, GOAL_TRIANGLE)    //G
            .addWall(6, 3, "SE")  .addGoal(6, 3, 3, GOAL_HEXAGON)     //Y
            .addWall(1, 4, "SW")  .addGoal(1, 4, 0, GOAL_CIRCLE)      //R
            .addWall(0, 6, "S")
            .addWall(4, 6, "NE")  .addGoal(4, 6, 2, GOAL_SQUARE)      //B
            .addWall(7, 7, "NESW");
        QUADRANTS[9] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //2C
            .addWall(5, 0, "E")
            .addWall(3, 2, "NW")  .addGoal(3, 2, 3, GOAL_CIRCLE)      //Y
            .addWall(0, 3, "S")
            .addWall(5, 3, "SW")  .addGoal(5, 3, 2, GOAL_TRIANGLE)    //B
            .addWall(2, 4, "NE")  .addGoal(2, 4, 0, GOAL_SQUARE)      //R
            .addWall(4, 5, "SE")  .addGoal(4, 5, 1, GOAL_HEXAGON)     //G
            .addWall(7, 7, "NESW");
        QUADRANTS[10] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //3C
            .addWall(1, 0, "E")
            .addWall(4, 1, "NE")  .addGoal(4, 1, 1, GOAL_CIRCLE)      //G
            .addWall(1, 3, "SW")  .addGoal(1, 3, 0, GOAL_TRIANGLE)    //R
            .addWall(0, 5, "S")
            .addWall(5, 5, "NW")  .addGoal(5, 5, 3, GOAL_SQUARE)      //Y
            .addWall(3, 6, "SE")  .addGoal(3, 6, 2, GOAL_HEXAGON)     //B
            .addWall(7, 7, "NESW");
        QUADRANTS[11] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //4C
            .addWall(2, 0, "E")
            .addWall(5, 1, "SW")  .addGoal(5, 1, 2, GOAL_CIRCLE)      //B
            .addWall(7, 2, "SE")  .addGoal(7, 2, -1, GOAL_VORTEX)     //W*
            .addWall(0, 3, "S")
            .addWall(3, 4, "SE")  .addGoal(3, 4, 0, GOAL_HEXAGON)     //R
            .addWall(6, 5, "NW")  .addGoal(6, 5, 1, GOAL_SQUARE)      //G
            .addWall(1, 6, "NE")  .addGoal(1, 6, 3, GOAL_TRIANGLE)    //Y
            .addWall(7, 7, "NESW");
        QUADRANTS[12] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //1D
            .addWall(5, 0, "E")
            .addWall(1, 3, "NW")  .addGoal(1, 3, 0, GOAL_CIRCLE)      //R
            .addWall(6, 4, "SE")  .addGoal(6, 4, 3, GOAL_HEXAGON)     //Y
            .addWall(0, 5, "S")
            .addWall(2, 6, "NE")  .addGoal(2, 6, 1, GOAL_TRIANGLE)    //G
            .addWall(3, 6, "SW")  .addGoal(3, 6, 2, GOAL_SQUARE)      //B
            .addWall(7, 7, "NESW");
        QUADRANTS[13] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //2D
            .addWall(2, 0, "E")
            .addWall(5, 2, "SE")  .addGoal(5, 2, 1, GOAL_HEXAGON)     //G
            .addWall(6, 2, "NW")  .addGoal(6, 2, 3, GOAL_CIRCLE)      //Y
            .addWall(1, 5, "SW")  .addGoal(1, 5, 0, GOAL_SQUARE)      //R
            .addWall(0, 6, "S")
            .addWall(4, 7, "NE")  .addGoal(4, 7, 2, GOAL_TRIANGLE)    //B
            .addWall(7, 7, "NESW");
        QUADRANTS[14] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //3D
            .addWall(4, 0, "E")
            .addWall(0, 2, "S")
            .addWall(6, 2, "SE")  .addGoal(6, 2, 2, GOAL_HEXAGON)     //B
            .addWall(2, 4, "NE")  .addGoal(2, 4, 1, GOAL_CIRCLE)      //G
            .addWall(3, 4, "SW")  .addGoal(3, 4, 0, GOAL_TRIANGLE)    //R
            .addWall(5, 6, "NW")  .addGoal(5, 6, 3, GOAL_SQUARE)      //Y
            .addWall(7, 7, "NESW");
        QUADRANTS[15] = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD) //4D
            .addWall(4, 0, "E")
            .addWall(6, 2, "NW")  .addGoal(6, 2, 3, GOAL_TRIANGLE)    //Y
            .addWall(2, 3, "NE")  .addGoal(2, 3, 2, GOAL_CIRCLE)      //B
            .addWall(3, 3, "SW")  .addGoal(3, 3, 1, GOAL_SQUARE)      //G
            .addWall(1, 5, "SE")  .addGoal(1, 5, 0, GOAL_HEXAGON)     //R
            .addWall(0, 6, "S")
            .addWall(5, 7, "SE")  .addGoal(5, 7, -1, GOAL_VORTEX)     //W*
            .addWall(7, 7, "NESW");
    }
    
    public final int width;
    public final int height;
    public final int size;      // width * height
    public final int sizeNumBits;   //number of bits required to store any board position (size - 1)

    public static final int NORTH = 0;  // up
    public static final int EAST  = 1;  // right
    public static final int SOUTH = 2;  // down
    public static final int WEST  = 3;  // left
    
    public final int[] directionIncrement;
    
    private static final Random RANDOM = new Random();
    
    private final int[] quadrants;      // quadrants used for this board (indexes in QUADRANTS) 
    private final boolean[][] walls;    // [4][width*height] 4 directions
    private final List<Goal> goals;     // all possible goals on the board
    private final List<Goal> randomGoals;
    private Goal goal;                  // the current goal
    
    private int[] robots;               // index=robot, value=position
    private boolean isFreestyleBoard;

    public class Goal implements Comparable<Goal> {
        public final int x, y, position, robotNumber, shape;
        public Goal(int x, int y, int robotNumber, int shape) {
            this.x = x;
            this.y = y;
            this.position = x + y * width;
            this.robotNumber = robotNumber;
            this.shape = shape;
        }
        @Override
        public boolean equals(Object obj) {
            if ((null == obj) || !(obj instanceof Goal)) { return false; }
            final Goal other = (Goal) obj;
            return ((this.x == other.x) &&
                    (this.y == other.y) &&
                    (this.position == other.position) &&
                    (this.robotNumber == other.robotNumber) &&
                    (this.shape == other.shape));
        }
        @Override
        public int hashCode() {
            int result = this.x;
            result = 1000003 * result + this.y;
            result = 1000003 * result + this.position;
            result = 1000003 * result + this.robotNumber;
            result = 1000003 * result + this.shape;
            return result;
        }
        // a List<Goal> will be sorted by:
        // 1. robotNumber a.k.a. color
        // 2. shape
        // 3. position
        @Override
        public int compareTo(final Goal other) {
            final int thisColor = (this.robotNumber < 0 ? Integer.MAX_VALUE : this.robotNumber);
            final int otherColor = (other.robotNumber < 0 ? Integer.MAX_VALUE : other.robotNumber);
            if (thisColor < otherColor) {
                return -1; // less
            } else if (thisColor > otherColor) {
                return 1; // greater
            } else { // robotNumbers are equal
                if (this.shape < other.shape) {
                    return -1; // less
                } else if (this.shape > other.shape) {
                    return 1; // greater
                } else { // shapes are equal
                    if (this.position < other.position) {
                        return -1; // less
                    } else if (this.position > other.position) {
                        return 1; // greater
                    } else {
                        return 0; // equal
                    }
                }
            }
        }
    }


    public static List<Goal> getStaticQuadrantGoals(final int quadrant) {
        final List<Goal> result = new ArrayList<>(QUADRANTS[quadrant].goals);
        Collections.sort(result);
        return result;
    }


    private Board(int width, int height, int numRobots) {
        this.width = width;
        this.height = height;
        this.size = width * height;
        this.sizeNumBits = 32 - Integer.numberOfLeadingZeros(this.size - 1);   //ceil(log2(x))
        this.directionIncrement = new int[4];
        this.directionIncrement[NORTH] = -width;
        this.directionIncrement[EAST]  = 1;
        this.directionIncrement[SOUTH] = width;
        this.directionIncrement[WEST]  = -1;
        this.quadrants = new int[4];
        this.walls = new boolean[4][width * height];    //filled with "false"
        this.robots = new int[numRobots];
        this.setRobots(numRobots);
        this.goals = new ArrayList<Goal>();
        this.randomGoals = new ArrayList<Goal>();
        this.goal = new Goal(0, 0, 0, 0); //dummy
        this.isFreestyleBoard = false;
    }


    public static Board createClone(final Board oldBoard) {
        // 1. board size, numRobots
        final Board newBoard = new Board(oldBoard.width, oldBoard.height, oldBoard.robots.length);
        // 2. robots
        System.arraycopy(oldBoard.robots, 0, newBoard.robots, 0, newBoard.robots.length);
        // 3. quadrants
        System.arraycopy(oldBoard.quadrants, 0, newBoard.quadrants, 0, newBoard.quadrants.length);
        // 4. walls
        for (int i = 0;  i < oldBoard.walls.length;  ++i) {
            System.arraycopy(oldBoard.walls[i], 0, newBoard.walls[i], 0, newBoard.walls[i].length);
        }
        // 5. list of goals
        newBoard.goals.clear();
        newBoard.goals.addAll(oldBoard.goals);
        // 6. active goal
        newBoard.goal = oldBoard.goal;
        // 7. isFreestyleBoard
        newBoard.isFreestyleBoard = oldBoard.isFreestyleBoard;
        return newBoard;
    }


    public static Board createBoardFreestyle(final Board oldBoard, final int width, final int height, final int numRobots) {
        if ((width < WIDTH_MIN) || (height < HEIGHT_MIN) || (width*height > SIZE_MAX)) {
            System.out.println("error in createBoardFreestyle(): invalid parameter: width=" + width + " height=" + height + " size=" + width*height);
            return oldBoard;
        }
        final Board newBoard = new Board(width, height, numRobots);
        newBoard.setFreestyleBoard();
        if (null != oldBoard) {
            // copy walls, goals and active goal
            oldBoard.removeOuterWalls();
            newBoard.goal = null;
            for (int y = 0;  y < Math.min(newBoard.height, oldBoard.height);  ++y) {
                int newPos = y * newBoard.width;
                int oldPos = y * oldBoard.width;
                for (int x = 0;  x < Math.min(newBoard.width, oldBoard.width);  ++x, ++newPos, ++oldPos) {
                    for (int d = 0;  d < newBoard.walls.length;  ++d) {
                        newBoard.walls[d][newPos] = oldBoard.walls[d][oldPos];
                    }
                    final Goal oldGoal = oldBoard.getGoalAt(oldPos);
                    if (null != oldGoal) {
                        newBoard.addGoal(newPos, oldGoal.robotNumber, oldGoal.shape);
                        if (oldGoal.equals(oldBoard.getGoal())) {
                            newBoard.setGoal(newPos);
                        }
                    }
                }
            }
            if (null == newBoard.goal) {
                newBoard.setGoalRandom();
            }
            oldBoard.addOuterWalls();
            // copy robots
            Arrays.fill(newBoard.robots, -1);
            for (int robot = 0;  robot < Math.min(newBoard.robots.length, oldBoard.robots.length);  ++robot) {
                final int oldX = oldBoard.robots[robot] % oldBoard.width;
                final int oldY = oldBoard.robots[robot] / oldBoard.width;
                final int newPos = oldX + oldY * newBoard.width;
                newBoard.setRobot(robot, newPos, false);
            }
            // copy of some robot didn't succeed, set it on lowest possible position
            for (int robot = 0;  robot < newBoard.getNumRobots();  ++robot) {
                if (0 > newBoard.robots[robot]) {
                    for (int pos = 0;  pos < newBoard.size;  ++pos) {
                        if (true == newBoard.setRobot(robot, pos, false)) {
                            break; // setRobot succeeded
                        }
                    }
                }
            }
        }
        newBoard.addOuterWalls();
        return newBoard;
    }


    public static Board createBoardQuadrants(int quadrantNW, int quadrantNE, int quadrantSE, int quadrantSW, int numRobots) {
        Board b = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, numRobots);
        //add walls and goals
        b.addQuadrant(quadrantNW, 0);
        b.addQuadrant(quadrantNE, 1);
        b.addQuadrant(quadrantSE, 2);
        b.addQuadrant(quadrantSW, 3);
        b.addOuterWalls();
        //place the robots ->  done by constructor / setRobots(num)
        //choose a goal
        b.setGoalRandom();
        return b;
    }
    
    
    public static Board createBoardRandom(int numRobots) {
        final ArrayList<Integer> indexList = new ArrayList<Integer>();
        for (int i = 0;  i < 4;  ++i) { indexList.add(Integer.valueOf(i)); }
        Collections.shuffle(indexList, RANDOM);
        return createBoardQuadrants(
                indexList.get(0).intValue() + RANDOM.nextInt(3 + 1) * 4,
                indexList.get(1).intValue() + RANDOM.nextInt(3 + 1) * 4,
                indexList.get(2).intValue() + RANDOM.nextInt(3 + 1) * 4,
                indexList.get(3).intValue() + RANDOM.nextInt(3 + 1) * 4,
                numRobots);
    }
    
    
    public static Board createBoardGameID(final String idStr) {
        Board result = null;
        int index = 0;
        try {
            //example game ID: 0765+41+2E21BD0F+1C
            final int q0 = Integer.parseInt(String.valueOf(idStr.charAt(index++)), 16);
            final int q1 = Integer.parseInt(String.valueOf(idStr.charAt(index++)), 16);
            final int q2 = Integer.parseInt(String.valueOf(idStr.charAt(index++)), 16);
            final int q3 = Integer.parseInt(String.valueOf(idStr.charAt(index++)), 16);
            final int qMax = QUADRANTS.length - 1;
            if ((q0 > qMax) || (q1 > qMax) || (q2 > qMax) || (q3 > qMax)) {
                throw new IllegalArgumentException("quadrant numbers out of range");
            }
            if (idStr.charAt(index++) != '+') {
                throw new IllegalArgumentException("missing '+' at index=" + (index-1));
            }
            final int numRobots = Integer.parseInt(String.valueOf(idStr.charAt(index++)), 16);
            int goalRobot = Integer.parseInt(String.valueOf(idStr.charAt(index++)), 16);
            if (goalRobot == 0x0f) { goalRobot = -1; }
            if ((numRobots > ROBOT_COLOR_NAMES_SHORT.length) || (goalRobot >= numRobots)) {
                throw new IllegalArgumentException("robot numbers out of range");
            }
            if (idStr.charAt(index++) != '+') {
                throw new IllegalArgumentException("missing '+' at index=" + (index-1));
            }
            final int[] robotPositions = new int[numRobots];
            for (int i = 0;  i < numRobots;  ++i) {
                String str = String.valueOf(idStr.charAt(index++));
                str += String.valueOf(idStr.charAt(index++));
                robotPositions[i] = Integer.parseInt(str, 16);
            }
            if (idStr.charAt(index++) != '+') {
                throw new IllegalArgumentException("missing '+' at index=" + (index-1));
            }
            String str = String.valueOf(idStr.charAt(index++));
            str += String.valueOf(idStr.charAt(index));
            final int goalPosition = Integer.parseInt(str, 16);
            result = createBoardQuadrants(q0, q1, q2, q3, numRobots);
            final boolean successRobots = result.setRobots(robotPositions);
            final boolean successGoal = result.setGoal(goalPosition);
            if (!successRobots || !successGoal || (result.goal.robotNumber != goalRobot)) {
                throw new IllegalArgumentException("robots or goal position are not valid");
            }
        } catch (Exception e) {
            System.out.println("error while parsing fingerprint(" + idStr +") :  " + e.toString());
            result = null;
        }
        return result;
    }
    
    
    /**
     * The game ID string consists of this info:
     * - the 4 board quadrants (4 board pieces, front or back)
     * - how many robots are on board
     * - which one is the active robot (goalRobot)
     * - positions of all robots
     * - position of goal
     * @return ID string of this board configuration
     */
    public String getGameID() {
        if (this.isFreestyleBoard()) {
            return "freestyle";
        }
        final Formatter fmt = new Formatter();
        final int quad01 = (this.getQuadrantNum(0) << 4) | this.getQuadrantNum(1);
        final int quad23 = (this.getQuadrantNum(2) << 4) | this.getQuadrantNum(3);
        fmt.format("%02X%02X+", Integer.valueOf(quad01), Integer.valueOf(quad23));
        final int robos = (this.robots.length << 4) | (this.goal.robotNumber >= 0 ? this.goal.robotNumber : 0x0f);
        fmt.format("%02X+", Integer.valueOf(robos));
        for (int robot : this.robots) {
            fmt.format("%02X", Integer.valueOf(robot));
        }
        fmt.format("+%02X", Integer.valueOf(this.goal.position));
        return fmt.toString();
    }
    
    
    /**
     * Creates a new Board object based on the state information contained in the input string.
     * 
     * @param dump a String that represents the state of a Board object.
     * @return a new Board object.
     */
    public static Board createBoardGameDump(final String dump) {
        final byte[] data = unb64unzip(dump.replaceAll("\\s", "")); //remove whitespace
        if (null == data) {
            return null;    //invalid input String
        }
        int didx = 0;
        // 0. data structure version
        final int version = data[didx++];
        if (0 != version) {
            return null;    //unknown data structure version
        }
        // 1. board size
        final int width = getInteger(data, didx);               didx += 4;
        final int height = getInteger(data, didx);              didx += 4;
        // 2. robots
        final int numRobots = 0xff & data[didx++];
        final Board board = new Board(width, height, numRobots);
        for (int i = 0;  numRobots > i;  ++i) {
            board.setRobot(i, getInteger(data, didx), true);    didx += 4;
        }
        // 3. quadrants
        for (int i = 0;  board.quadrants.length > i;  ++i) {
            board.quadrants[i] = 0xff & data[didx++];
        }
        // 4. walls
        for (int dir = 0;  dir < board.walls.length;  ++dir) {
            for (int pos = 0;  pos < board.walls[dir].length;  ++pos) {
                board.walls[dir][pos] = (0 != data[didx++]);
            }
        }
        // 5. list of goals
        final int numGoals = getInteger(data, didx);            didx += 4;
        for (int i = 0;  numGoals > i;  ++i) {
            final int pos = getInteger(data, didx);             didx += 4;
            final int robot = 0xff & data[didx++];
            final int shape = 0xff & data[didx++];
            board.addGoal(pos, (255 == robot ? -1 : robot), shape);
        }
        // 6. active goal
        final int pos = getInteger(data, didx);                 didx += 4;
        final int robot = 0xff & data[didx++];
        final int shape = 0xff & data[didx++];
        if (-1 == pos) {
            board.goal = null;
        } else {
            final boolean setGoalResult = board.setGoal(pos);
            final Goal goal = board.getGoal();
            if ((false == setGoalResult) || (goal.position != pos) || (goal.robotNumber != (255 == robot ? -1 : robot)) || (goal.shape != shape)) {
                return null;    //invalid active goal
            }
        }
        // 7. isFreestyleBoard
        board.isFreestyleBoard = (0 != data[didx]);
        return board;
    }
    
    
    /**
     * Creates a specific text representation of this Board object. This is printable
     * text which is suitable for copy&paste into e-mails, internet forums etc.
     * 
     * @return a String that represents the state of this Board object.
     */
    public String getGameDump() {
        final List<Byte> data = new ArrayList<Byte>();
        // 0. data structure version
        data.add(Byte.valueOf((byte)0));
        // 1. board size
        putInteger(this.width, data);
        putInteger(this.height, data);
        // 2. robots
        data.add(Byte.valueOf((byte)this.robots.length));
        for (int robot : this.robots) {
            putInteger(robot, data);
        }
        // 3. quadrants
        for (int quadrant : this.quadrants) {
            data.add(Byte.valueOf((byte)quadrant));
        }
        // 4. walls
        for (int dir = 0;  dir < this.walls.length;  ++dir) {
            for (int pos = 0;  pos < this.walls[dir].length;  ++pos) {
                data.add(Byte.valueOf(this.walls[dir][pos] ? (byte)1 : (byte)0));
            }
        }
        // 5. list of goals
        putInteger(this.goals.size(), data);
        for (Goal goal : this.goals) {  //6 bytes
            putInteger(goal.position, data);
            data.add(Byte.valueOf((byte)goal.robotNumber));
            data.add(Byte.valueOf((byte)goal.shape));
        }
        // 6. active goal
        putInteger((null == this.goal ? -1 : this.goal.position), data);
        data.add(Byte.valueOf((byte)(null == this.goal ? 0 : this.goal.robotNumber)));
        data.add(Byte.valueOf((byte)(null == this.goal ? 0 : this.goal.shape)));
        // 7. isFreestyleBoard
        data.add(Byte.valueOf(this.isFreestyleBoard() ? (byte)1 : (byte)0));
        //convert data to String
        final byte[] dataArray = new byte[data.size()];
        int i = 0;
        for (Byte dat : data) {
            dataArray[i++] = dat.byteValue();
        }
        final String str = zipb64(dataArray);
        return str;
    }
    
    
    private static void putInteger(final int value, final List<Byte> data) {
        data.add(Byte.valueOf((byte)(0xff & (value >> 24))));
        data.add(Byte.valueOf((byte)(0xff & (value >> 16))));
        data.add(Byte.valueOf((byte)(0xff & (value >> 8))));
        data.add(Byte.valueOf((byte)(0xff & value)));
    }
    
    
    private static int getInteger(final byte[] data, int didx) {
        int result = data[didx++];
        result = (result << 8) | (0xff & data[didx++]);
        result = (result << 8) | (0xff & data[didx++]);
        result = (result << 8) | (0xff & data[didx]);
        return result;
    }
    
    
    private static String zipb64(final byte[] input) {
        //store uncompressed length
        final byte[] zipOutput = new byte[input.length * 2 + 128];  //large enough?!
        for (int i = 0, rshift = 24;  i < 4;  rshift -= 8, i++) {
            zipOutput[i] = (byte)((input.length >>> rshift) & 0xff);
        }
        //zip/deflate data
        final Deflater zip = new Deflater(9);
        zip.setInput(input);
        zip.finish();
        final int zipOutLen = 4 + zip.deflate(zipOutput, 4, zipOutput.length-4);    //skip uncompressed length
        //encode base64
        final byte[] b64Input = Arrays.copyOf(zipOutput, zipOutLen);
        final String b64Output = Base64.getEncoder().encodeToString(b64Input);
        //compute CRC of encoded data
        final CRC32 crc32 = new CRC32();
        crc32.update(b64Output.getBytes(StandardCharsets.UTF_8));
        final long crc32Value = crc32.getValue();
        final String crc32String = new Formatter().format("%08X", Long.valueOf(crc32Value)).toString();
        //build output string:  starts and ends with "!", to be split at "!"
        final String result = "!DriftingDroids_game!" + crc32String + "!" + b64Output + "!";
        return result;
    }
    
    
    private static byte[] unb64unzip(final String input) {
        byte[] result = null;
        try {
            //split input string and first validation
            final String[] inputSplit = input.split("!");
            if ((4 != inputSplit.length) || (!inputSplit[1].equals("DriftingDroids_game")) ||
                    ('!' != input.charAt(0)) || ('!' != input.charAt(input.length()-1))) {
                throw new IllegalArgumentException("input string has wrong format");
            }
            //validate data
            final long b64crc = Long.parseLong(inputSplit[2], 16);      //throws NumberFormatException
            final CRC32 crc32 = new CRC32();
            crc32.update(inputSplit[3].getBytes(StandardCharsets.UTF_8));
            if (crc32.getValue() != b64crc) {
                throw new IllegalArgumentException("data CRC mismatch");
            }
            //parse base64 string
            final byte[] b64Output = Base64.getDecoder().decode(inputSplit[3]);    //throws IllegalArgumentException
            //unzip/inflate data
            int unzipLen = 0;
            for (int i = 0;  i < 4;  ++i) {
                unzipLen = (unzipLen << 8) | (0xff & b64Output[i]);
            }
            result = new byte[unzipLen];
            final Inflater unzip = new Inflater();
            unzip.setInput(b64Output, 4, b64Output.length-4);
            final int unzipLenActual = unzip.inflate(result);   //throws DataFormatException
            //validate unzip
            if (unzipLen != unzipLenActual) {
                throw new IllegalArgumentException("uncompressed data length mismatch");
            }
        } catch(Exception e) {
            System.out.println("error in unb64unzip: " + e.toString());
            result = null;
        }
        return result;
    }

    
    public Board rotate90(final boolean clockwise) {
        final Board newBoard = new Board(this.height, this.width, this.robots.length);
        //quadrants
        for (int q = 0;  q < 4;  ++q) {
            newBoard.quadrants[(q + (clockwise ? 1 : -1)) & 3] = this.quadrants[q];
        }
        //walls
        for (int d = 0;  d < 4;  ++d) {
            for (int pos = 0;  pos < this.size;  ++pos) {
                newBoard.walls[(d + (clockwise ? 1 : -1)) & 3][this.rotatePosition90(pos, clockwise)] = this.walls[d][pos];
            }
        }
        //robots
        for (int i = 0;  i < this.robots.length;  ++i) {
            newBoard.robots[i] = this.rotatePosition90(this.robots[i], clockwise);
        }
        //goals
        for (Goal g : this.goals) {
            newBoard.addGoal(this.rotatePosition90(g.position, clockwise), g.robotNumber, g.shape);
        }
        //goal
        newBoard.setGoal(this.rotatePosition90(this.goal.position, clockwise));
        return newBoard;
    }
    
    
    private int rotatePosition90(final int pos, final boolean clockwise) {
        final int x = pos % this.width;
        final int y = pos / this.width;
        final int newx, newy;
        if (clockwise) {
            newx = this.height - 1 - y;
            newy = x;
        } else {
            newx = y;
            newy = this.width - 1 - x;
        }
        return newx + newy * this.height;
    }
    
    
    private int transformQuadrantX(final int qX, final int qY, final int qPos) {
        //qPos (quadrant target position): 0==NW, 1==NE, 2==SE, 3==SW
        final int resultX;
        switch (qPos) {
        case 1: resultX = this.width - 1 - qY;  break;
        case 2: resultX = this.width - 1 - qX;  break;
        case 3: resultX = qY;                   break;
        default:resultX = qX;                   break;
        }
        return resultX; 
    }
    private int transformQuadrantY(final int qX, final int qY, final int qPos) {
        //qPos (quadrant target position): 0==NW, 1==NE, 2==SE, 3==SW
        final int resultY;
        switch (qPos) {
        case 1: resultY = qX;                   break;
        case 2: resultY = this.height - 1 - qY; break;
        case 3: resultY = this.height - 1 - qX; break;
        default:resultY = qY;                   break;
        }
        return resultY; 
    }
    private int transformQuadrantPosition(final int qX, final int qY, final int qPos) {
        return (this.transformQuadrantX(qX, qY, qPos) + this.transformQuadrantY(qX, qY, qPos) * this.width); 
    }
    
    
    private Board addQuadrant(final int qNum, final int qPos) {
        this.quadrants[qPos] = qNum;
        final Board quadrant = QUADRANTS[qNum];
        //qPos (quadrant target position): 0==NW, 1==NE, 2==SE, 3==SW
        int qX, qY;
        //add walls
        for (qY = 0;  qY < quadrant.height/2;  ++qY) {
            for (qX = 0;  qX < quadrant.width/2;  ++qX) {
                for (int dir = 0;  dir < 4;  ++dir) {
                    this.walls[(dir + qPos) & 3][this.transformQuadrantPosition(qX, qY, qPos)] |=
                        quadrant.walls[dir][qX + qY * quadrant.width];
                }
            }
            this.walls[(WEST + qPos) & 3][this.transformQuadrantPosition(qX, qY, qPos)] |=
                quadrant.walls[EAST][qX - 1 + qY * quadrant.width];
        }
        for (qX = 0;  qX < quadrant.width/2;  ++qX) {
            this.walls[(NORTH + qPos) & 3][this.transformQuadrantPosition(qX, qY, qPos)] |=
                quadrant.walls[SOUTH][qX + (qY - 1) * quadrant.width];
        }
        //add goals
        for (Goal g : quadrant.goals) {
            this.addGoal(this.transformQuadrantX(g.x, g.y, qPos), this.transformQuadrantY(g.x, g.y, qPos), g.robotNumber, g.shape);
        }
        return this;
    }
    
    
    public boolean isSolution01() {
        for (int robo = 0;  robo < this.robots.length;  ++robo) {
            if ((this.goal.robotNumber != robo) && (this.goal.robotNumber != -1)) {
                continue; // skip because it's not the goal robot
            }
            final int oldRoboPos = this.robots[robo];
            if (this.goal.position == oldRoboPos) {
                return true; // already on goal
            }
            int dir = -1;
            for (final int dirIncr : this.directionIncrement) {
                ++dir;
                int newRoboPos = oldRoboPos;
                int prevRoboPos = oldRoboPos;
                // move the robot until it reaches a wall or another robot.
                // NOTE: we rely on the fact that all boards are surrounded
                // by outer walls. without the outer walls we would need
                // some additional boundary checking here.
                while (true) {
                    if (true == this.walls[dir][newRoboPos]) { // stopped by wall
                        if (this.goal.position == newRoboPos) {
                            return true; // one move to goal
                        }
                        break; // can't go on
                    }
                    if (true == this.isRobotPos(newRoboPos)) { // stopped by robot
                        if (this.goal.position == prevRoboPos) {
                            return true; // one move to goal
                        }
                        // go on in this direction
                    }
                    prevRoboPos = newRoboPos;
                    newRoboPos += dirIncr;
                }
            }
        }
        return false;
    }
    
    private boolean isRobotPos(final int position) {
        for (final int roboPos : this.robots) {
            if (position == roboPos) {
                return true;
            }
        }
        return false;
    }
    
    public void setRobots(final int numRobots) {
        this.robots = new int[numRobots];
        if (this.isFreestyleBoard()) {
            this.setRobotsRandom();
        } else {
            //original board / made out of quadrants
            this.setRobot(0, 14 +  2 * this.width, false); //R
            this.setRobot(1,  1 +  2 * this.width, false); //G
            this.setRobot(2, 13 + 11 * this.width, false); //B
            this.setRobot(3, 15 +  0 * this.width, false); //Y
            this.setRobot(4, 15 +  7 * this.width, false); //S
        }
    }
    
    public void setRobotsRandom() {
        do {
            Arrays.fill(this.robots, -1);
            for (int i = 0;  i < this.robots.length;  ++i) {
                int position;
                do {
                    position = RANDOM.nextInt(this.size);
                } while (false == this.setRobot(i, position, false));
            }
        } while (true == this.isSolution01());
    }
    
    public boolean setRobots(final int[] newRobots) {
        if (this.robots.length != newRobots.length) { return false; }
        final int[] backup = Arrays.copyOf(this.robots, this.robots.length);
        Arrays.fill(this.robots, -1);
        for (int i = 0; i < newRobots.length; ++i) {
            if ( ! this.setRobot(i, newRobots[i], false)) {    //failed to set a robot
                //undo all changes
                System.arraycopy(backup, 0, this.robots, 0, backup.length);
                return false;
            }
        }
        return true;
    }
    
    public boolean setRobot(final int robot, final int position, final boolean allowSwapRobots) {
        //invalid robot number?
        //impossible position (out of bounds or obstacle)?
        if ((robot < 0) || (robot >= this.robots.length) ||
                (position < 0) || (position >= this.size) ||
                this.isObstacle(position) ||
                ((false == allowSwapRobots) && (this.getRobotNum(position) >= 0) && (this.getRobotNum(position) != robot)) )  {
            return false;   
        } else {
            //position already occupied by another robot?
            final int otherRobot = this.getRobotNum(position);
            final int oldPosition = this.robots[robot];
            if ((otherRobot >= 0) && (otherRobot != robot) && (oldPosition >= 0)) {
                this.robots[otherRobot] = oldPosition;
            }
            //set this robot's position
            this.robots[robot] = position;
            return true;
        }
    }
    
    public void setGoalRandom() {
        if (this.goals.isEmpty()) {
            this.goal = null;
            return;
        }
        if (this.randomGoals.isEmpty()) {
            this.randomGoals.addAll(this.goals);
            Collections.shuffle(this.randomGoals, RANDOM);
        }
        this.goal = this.randomGoals.remove(0);
        if (this.goal.robotNumber >= this.robots.length) {  //goal not usable
            this.setGoalRandom();   //recursion
        }
        if (this.isSolution01() && (this.randomGoals.size() > 0)) {
            //the resulting board configuration has a solution of 0 or 1 move
            //and there are some other goals available in list randomGoals.
            final Goal goal01 = this.goal;
            this.setGoalRandom();   //recursion
            this.randomGoals.add(goal01);
        }
    }
    
    public boolean setGoal(final int position) {
        boolean result = false;
        for (Goal g : this.goals) {
            if ((g.position == position) && (g.robotNumber < this.robots.length)) {
                this.goal = g;
                result = true;
                break;
            }
        }
        return result;
    }
    
    public Board addGoal(int pos, int robot, int shape) {
        this.removeGoal(pos);
        return this.addGoal(pos%this.width, pos/this.width, robot, shape);
    }
    
    private Board addGoal(int x, int y, int robot, int shape) {
        final Goal g = new Goal(x, y, robot, shape);
        this.goals.add(g);
        if (null == this.goal) {
            this.goal = g;
        }
        return this;
    }
    
    public boolean removeGoal(final int position) {
        boolean result = false;
        final Iterator<Goal> iter = this.goals.iterator();
        while (iter.hasNext()) {
            final Goal g = iter.next();
            if (g.position == position) {
                iter.remove();
                this.randomGoals.remove(g);
                result = true;
                if (g.equals(this.goal)) {
                    this.setGoalRandom();
                }
            }
        }
        return result;
    }
    
    public void removeGoals() {
        this.goals.clear();
        this.goal = null;
        this.randomGoals.clear();
    }

    private Board addOuterWalls() {
        return this.setOuterWalls(true);
    }
    private Board removeOuterWalls() {
        return this.setOuterWalls(false);
    }
    private Board setOuterWalls(boolean value) {
        for (int x = 0; x < this.width; ++x) {
            this.setWall(x, 0,               NORTH, value);
            this.setWall(x, this.height - 1, SOUTH, value);
        }
        for (int y = 0; y < this.height; ++y) {
            this.setWall(0,              y, WEST, value);
            this.setWall(this.width - 1, y, EAST, value);
        }
        return this;
    }

    private Board addWall(int x, int y, String str) {
        this.setWalls(x, y, str, true);
        return this;
    }
    
    private void setWalls(int x, int y, String str, boolean value) {
        if (str.contains("N")) {
            this.setWall(x, y,     NORTH,  value);
            this.setWall(x, y - 1, SOUTH,  value);
        }
        if (str.contains("E")) {
            this.setWall(x,     y, EAST,   value);
            this.setWall(x + 1, y, WEST,   value);
        }
        if (str.contains("S")) {
            this.setWall(x, y,     SOUTH,  value);
            this.setWall(x, y + 1, NORTH,  value);
        }
        if (str.contains("W")) {
            this.setWall(x,     y, WEST,   value);
            this.setWall(x - 1, y, EAST,   value);
        }
    }

    private void setWall(int x, int y, int direction, boolean value) {
        if ((x >= 0) && (x < this.width) && (y >= 0) && (y < this.height)) {
            this.walls[direction][x + y * this.width] = value;
        }
    }
    
    public void setWall(int position, String direction, boolean doSet) {
        final int x = position % this.width;
        final int y = position / this.width;
        if (false == doSet) {
            //prevent removal of outer walls
            if (0 == x)                 { direction = direction.replace('W', ' '); }
            if (this.width - 1 == x)    { direction = direction.replace('E', ' '); }
            if (0 == y)                 { direction = direction.replace('N', ' '); }
            if (this.height - 1 == y)   { direction = direction.replace('S', ' '); }
        }
        this.setWalls(x, y, direction, doSet);
    }
    
    public void removeWalls() {
        for (boolean[] w : this.walls) {
            Arrays.fill(w, false);
        }
        this.addOuterWalls();
    }

    public boolean isWall(int position, int direction) {
        return this.walls[direction][position];
    }
    
    public boolean isObstacle(int position) {
        return (this.isWall(position, NORTH) &&
                this.isWall(position, EAST) &&
                this.isWall(position, SOUTH) &&
                this.isWall(position, WEST));
    }
    
    private int getRobotNum(final int position) {
        int robotNum = -1;  //default: not found
        for (int i = 0; i < this.robots.length; ++i) {
            if (this.robots[i] == position) {
                robotNum = i;
                break;
            }
        }
        return robotNum;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        //s.append("Board (").append(this.width).append(",").append(this.height)
        //        .append(",").append(this.robots.length).append(")").append('\n');
        
        // print board graphically
        // horizontal wall = "---", vertical wall = "|",
        // empty cell = ".", robots = "01234", goal = "X"
        int position = 0;
        for (int y = 0; y < this.height; ++y) {
            StringBuilder sWC = new StringBuilder(); // west walls and cells
            for (int x = 0; x < this.width; ++x, ++position) {
                s.append(this.isWall(position, NORTH) ? " ---" : "    ");
                sWC.append(this.isWall(position, WEST) ? "| " : "  ");
                int robotNum;
                if (isObstacle(position)) {
                    sWC.append('#');
                } else if ((robotNum = getRobotNum(position)) >= 0) {
                    sWC.append(robotNum);
                } else if (position == this.goal.position) {
                    sWC.append('X');
                } else {
                    sWC.append('.');
                }
                sWC.append(' ');
            }
            s.append(' ').append('\n');
            sWC.append(this.isWall(position - 1, EAST) ? '|' : ' ');
            s.append(sWC).append('\n');
        }
        for (int x = 0; x < this.width; ++x, ++position) {
            s.append(this.isWall(position - this.width, SOUTH) ? " ---" : "    ");
        }
        s.append(' ').append('\n');
//        // print list of wall coordinates and directions
//        s.append("walls:").append('\n');
//        position = 0;
//        for (int y = 0; y < this.height; ++y) {
//            for (int x = 0; x < this.width; ++x, ++position) {
//                String t = "";
//                if (this.isWall(position, NORTH)) t += "N";
//                if (this.isWall(position, EAST )) t += "E";
//                if (this.isWall(position, SOUTH)) t += "S";
//                if (this.isWall(position, WEST )) t += "W";
//                if (! t.equals("")) {
//                    s.append("(").append(x).append(",").append(y).append(") ")
//                            .append(t).append('\n');
//                }
//            }
//        }
        // print list of robot coordinates
        for (int i = 0; i < this.robots.length; ++i) {
            s.append("robot #").append(i)
             .append(" (").append(this.robots[i] % this.width)
             .append(", ").append(this.robots[i] / this.width)
             .append(")").append('\n');
        }
        // print goal coordinates
        s.append("goal #").append(this.goal.robotNumber)
         .append(" (").append(this.goal.position % this.width)
         .append(", ").append(this.goal.position / this.width).append(")");
        return s.toString();
    }

    public int[] getRobotPositions() {
        return this.robots;
    }
    
    public Goal getGoal() {
        return this.goal;
    }
    
    public Goal getGoalAt(final int position) {
        Goal result = null;
        for (Goal g : this.goals) {
            if (g.position == position) {
                result = g;
                break;
            }
        }
        return result;
    }
    
    public int getQuadrantNum(final int qPos) { //qPos: 0=NW, 1=NE, 2=SE, 3=SW
        return this.quadrants[qPos];
    }
    
    public boolean[][] getWalls() {
        return this.walls;
    }
    
    /**
     * determine the direction from position "old" to "new".
     * @param diffPos difference of positions (new - old)
     * @return direction (Board.NORTH, Board.EAST, Board.SOUTH or Board.WEST)
     */
    public int getDirection(final int diffPos) {
        final int dir;
        if (diffPos < 0) {
            if  (-diffPos < this.width) { dir = Board.WEST; }
            else                        { dir = Board.NORTH; }
        } else {
            if  (diffPos < this.width)  { dir = Board.EAST; }
            else                        { dir = Board.SOUTH; }
        }
        return dir;
    }
    
    public static String getColorLongL10N(int color) {
        if (color < 0) {    //wildcard
            return L10N.getString("board.color.wildcard.text");
        } else {
            return L10N.getString("board.color." + ROBOT_COLOR_NAMES_LONG[color] +".text");
        }
    }
    public static String getColorShortL10N(int color) {
        if (color < 0) {    //wildcard
            return L10N.getString("board.color.w.text");
        } else {
            return L10N.getString("board.color." + ROBOT_COLOR_NAMES_SHORT[color] +".text");
        }
    }
    public static String getGoalShapeL10N(int shape) {
        return L10N.getString("board.shape." + GOAL_SHAPE_NAMES[shape] + ".text");
    }
    
    public int getNumRobots() {
        return this.robots.length;
    }
    
    public void setFreestyleBoard() {
        this.isFreestyleBoard = true;
    }
    public boolean isFreestyleBoard() {
        return this.isFreestyleBoard;
    }
}
