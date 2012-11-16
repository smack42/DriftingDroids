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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;



public class Board {
    static final ResourceBundle L10N = ResourceBundle.getBundle("driftingdroids-localization-model");   //L10N = Localization
    
    public static final int WIDTH_STANDARD = 16;
    public static final int HEIGHT_STANDARD = 16;
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
        "1A", "2A", "3A", "4A", "1B", "2B", "3B", "4B"
    };
    private static final Board[] QUADRANTS = {
        new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD),
        new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD),
        new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD),
        new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD),
        new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD),
        new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD),
        new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD),
        new Board(WIDTH_STANDARD, HEIGHT_STANDARD, NUMROBOTS_STANDARD)
    };
    static {
        QUADRANTS[0]    //1A
                  .addWall(1, 0, "E")
                  .addWall(4, 1, "NW")  .addGoal(4, 1, 0, GOAL_CIRCLE)      //R
                  .addWall(1, 2, "NE")  .addGoal(1, 2, 1, GOAL_TRIANGLE)    //G
                  .addWall(6, 3, "SE")  .addGoal(6, 3, 3, GOAL_HEXAGON)     //Y
                  .addWall(0, 5, "S")
                  .addWall(3, 6, "SW")  .addGoal(3, 6, 2, GOAL_SQUARE)      //B
                  .addWall(7, 7, "NESW");
        QUADRANTS[1]    //2A
                  .addWall(3, 0, "E")
                  .addWall(5, 1, "SE")  .addGoal(5, 1, 1, GOAL_HEXAGON)     //G
                  .addWall(1, 2, "SW")  .addGoal(1, 2, 0, GOAL_SQUARE)      //R
                  .addWall(0, 3, "S")
                  .addWall(6, 4, "NW")  .addGoal(6, 4, 3, GOAL_CIRCLE)      //Y
                  .addWall(2, 6, "NE")  .addGoal(2, 6, 2, GOAL_TRIANGLE)    //B
                  .addWall(7, 7, "NESW");
        QUADRANTS[2]    //3A
                  .addWall(3, 0, "E")
                  .addWall(5, 2, "SE")  .addGoal(5, 2, 2, GOAL_HEXAGON)     //B
                  .addWall(0, 4, "S")
                  .addWall(2, 4, "NE")  .addGoal(2, 4, 1, GOAL_CIRCLE)      //G
                  .addWall(7, 5, "SW")  .addGoal(7, 5, 0, GOAL_TRIANGLE)    //R
                  .addWall(1, 6, "NW")  .addGoal(1, 6, 3, GOAL_SQUARE)      //Y
                  .addWall(7, 7, "NESW");
        QUADRANTS[3]    //4A
                  .addWall(3, 0, "E")
                  .addWall(6, 1, "SW")  .addGoal(6, 1, 2, GOAL_CIRCLE)      //B
                  .addWall(1, 3, "NE")  .addGoal(1, 3, 3, GOAL_TRIANGLE)    //Y
                  .addWall(5, 4, "NW")  .addGoal(5, 4, 1, GOAL_SQUARE)      //G
                  .addWall(2, 5, "SE")  .addGoal(2, 5, 0, GOAL_HEXAGON)     //R
                  .addWall(7, 5, "SE")  .addGoal(7, 5, -1, GOAL_VORTEX)     //W*
                  .addWall(0, 6, "S")
                  .addWall(7, 7, "NESW");
        QUADRANTS[4]    //1B
                  .addWall(4, 0, "E")
                  .addWall(6, 1, "SE")  .addGoal(6, 1, 3, GOAL_HEXAGON)     //Y
                  .addWall(1, 2, "NW")  .addGoal(1, 2, 1, GOAL_TRIANGLE)    //G
                  .addWall(0, 5, "S")
                  .addWall(6, 5, "NE")  .addGoal(6, 5, 2, GOAL_SQUARE)      //B
                  .addWall(3, 6, "SW")  .addGoal(3, 6, 0, GOAL_CIRCLE)      //R
                  .addWall(7, 7, "NESW");
        QUADRANTS[5]    //2B
                  .addWall(4, 0, "E")
                  .addWall(2, 1, "NW")  .addGoal(2, 1, 3, GOAL_CIRCLE)      //Y
                  .addWall(6, 3, "SW")  .addGoal(6, 3, 2, GOAL_TRIANGLE)    //B
                  .addWall(0, 4, "S")
                  .addWall(4, 5, "NE")  .addGoal(4, 5, 0, GOAL_SQUARE)      //R
                  .addWall(1, 6, "SE")  .addGoal(1, 6, 1, GOAL_HEXAGON)     //G
                  .addWall(7, 7, "NESW");
        QUADRANTS[6]    //3B
                  .addWall(3, 0, "E")
                  .addWall(1, 1, "SW")  .addGoal(1, 1, 0, GOAL_TRIANGLE)    //R
                  .addWall(6, 2, "NE")  .addGoal(6, 2, 1, GOAL_CIRCLE)      //G
                  .addWall(2, 4, "SE")  .addGoal(2, 4, 2, GOAL_HEXAGON)     //B
                  .addWall(0, 5, "S")
                  .addWall(7, 5, "NW")  .addGoal(7, 5, 3, GOAL_SQUARE)      //Y
                  .addWall(7, 7, "NESW");
        QUADRANTS[7]    //4B
                  .addWall(4, 0, "E")
                  .addWall(2, 1, "SE")  .addGoal(2, 1, 0, GOAL_HEXAGON)     //R
                  .addWall(1, 3, "SW")  .addGoal(1, 3, 1, GOAL_SQUARE)      //G
                  .addWall(0, 4, "S")
                  .addWall(6, 4, "NW")  .addGoal(6, 4, 3, GOAL_TRIANGLE)    //Y
                  .addWall(5, 6, "NE")  .addGoal(5, 6, 2, GOAL_CIRCLE)      //B
                  .addWall(3, 7, "SE")  .addGoal(3, 7, -1, GOAL_VORTEX)     //W*
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
    private final int[] robots;         // index=robot, value=position
    private final List<Goal> goals;     // all possible goals on the board
    private final List<Goal> randomGoals;
    private Goal goal;                  // the current goal

    public class Goal {
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
        this.goals = new ArrayList<Goal>();
        this.randomGoals = new ArrayList<Goal>();
        this.goal = new Goal(0, 0, 0, 0); //dummy
    }

    
    public static Board createBoardQuadrants(int quadrantNW, int quadrantNE, int quadrantSE, int quadrantSW, int numRobots) {
        Board b = new Board(WIDTH_STANDARD, HEIGHT_STANDARD, numRobots);
        //add walls and goals
        b.addQuadrant(quadrantNW, 0);
        b.addQuadrant(quadrantNE, 1);
        b.addQuadrant(quadrantSE, 2);
        b.addQuadrant(quadrantSW, 3);
        b.addOuterWalls();
        //place the robots
        b.setRobot(0, 14 +  2 * b.width, false); //R
        b.setRobot(1,  1 +  2 * b.width, false); //G
        b.setRobot(2, 13 + 11 * b.width, false); //B
        b.setRobot(3, 15 +  0 * b.width, false); //Y
        b.setRobot(4, 15 +  7 * b.width, false); //S
        //choose a goal
        b.setGoalRandom();
        return b;
    }
    
    
    public static Board createBoardRandom(int numRobots) {
        final ArrayList<Integer> indexList = new ArrayList<Integer>();
        for (int i = 0;  i < 4;  ++i) { indexList.add(Integer.valueOf(i)); }
        Collections.shuffle(indexList, RANDOM);
        return createBoardQuadrants(
                indexList.get(0).intValue() + (RANDOM.nextBoolean() ? 4 : 0),
                indexList.get(1).intValue() + (RANDOM.nextBoolean() ? 4 : 0),
                indexList.get(2).intValue() + (RANDOM.nextBoolean() ? 4 : 0),
                indexList.get(3).intValue() + (RANDOM.nextBoolean() ? 4 : 0),
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
            if ((q0 > 7) || (q1 > 7) || (q2 > 7) || (q3 > 7)) {
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
            str += String.valueOf(idStr.charAt(index++));
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
        boolean result = false;
        //is this a zero-move solution?
        if (this.goal.robotNumber < 0) {
            for (int pos : this.robots) {
                if (this.goal.position == pos) {
                    result = true;  break;
                }
            }
        } else if (this.goal.position == this.robots[this.goal.robotNumber]) {
            result = true;
        }
        //is this a one-move solution?
        if (false == result) {
            //copied from SolverBFS.getFinalStates()
            final boolean[] expandRobotPositions = new boolean[this.size];
            Arrays.fill(expandRobotPositions, false);
            for (int pos : this.robots) { expandRobotPositions[pos] = true; }
            for (int robo = 0;  robo < this.robots.length;  ++robo) {
                final int oldRoboPos = this.robots[robo];
                int dir = -1;
                for (int dirIncr : this.directionIncrement) {
                    ++dir;
                    int newRoboPos = oldRoboPos;
                    while (false == this.walls[dir][newRoboPos]) {  //move the robot until it reaches a wall or another robot.
                        newRoboPos += dirIncr;                  //NOTE: we rely on the fact that all boards are surrounded
                        if (expandRobotPositions[newRoboPos]) { //by outer walls. without the outer walls we would need
                            newRoboPos -= dirIncr;              //some additional boundary checking here.
                            break;
                        }
                    }
                    if ( (oldRoboPos != newRoboPos) && (this.goal.position == newRoboPos) &&
                            ((this.goal.robotNumber == robo) || (this.goal.robotNumber == -1)) ) {
                        result = true;
                    }
                }
            }
        }
        return result;
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
                for (int j = 0; j < backup.length; ++j) {
                    this.robots[j] = backup[j];         //undo all changes
                }
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
        if (this.randomGoals.size() == 0) {
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
    
    private Board addGoal(int pos, int robot, int shape) {
        this.goals.add(new Goal(pos%this.width, pos/this.width, robot, shape));
        return this;
    }
    
    private Board addGoal(int x, int y, int robot, int shape) {
        this.goals.add(new Goal(x, y, robot, shape));
        return this;
    }

    private Board addOuterWalls() {
        for (int x = 0; x < this.width; ++x) {
            this.addWall(x, 0,               "N");
            this.addWall(x, this.height - 1, "S");
        }
        for (int y = 0; y < this.height; ++y) {
            this.addWall(0,              y, "W");
            this.addWall(this.width - 1, y, "E");
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
}
