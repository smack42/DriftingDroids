/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011, 2012, 2013  Michael Henke

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

package driftingdroids.ui;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import driftingdroids.model.Board;
import driftingdroids.model.KeyDepthMapFactory;
import driftingdroids.model.KeyDepthMapTrieGeneric;
import driftingdroids.model.KeyDepthMapTrieSpecial;
import driftingdroids.model.Solution;
import driftingdroids.model.Solver;
import driftingdroids.model.SolverIDDFS;




public class Starter {
    
    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        new SwingGUI("DriftingDroids 1.3");
        //runTestRandom1000();
//        runTestKeyDepthMap();
    }
    
    
    @SuppressWarnings("unused")
    private static void runTestRandom1000() throws InterruptedException {
        final Date startDate = new Date();
        
        final int numGames = 1000;
        
        final Board theBoard = Board.createBoardGameID("0765+42+2E21BD0F+93");
        
        int maxMoves = -1;
        String maxSolution = "";
        for (int i = 1; i <= numGames; ++i) {
            
            theBoard.setRobotsRandom();
            final Solver theSolver = Solver.createInstance(theBoard);
            final Solution theSolution = theSolver.execute().get(0);
            final int moves = theSolution.size();
            
            //System.err.println(i + " usedMem=" + (getBytesUsed() >> 20) + " MiB  " + theSolver.getKnownStatesNumber());
            
            if ((0 == i % 100) || (moves > maxMoves)) {
                String msg = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                msg += "  gamesSolved=" + i + "/" + numGames + "  maxMoves=" + (moves > maxMoves ? moves : maxMoves);
                System.out.println(msg);
            }
            if (moves > maxMoves) {
                maxMoves = moves;
                maxSolution = theBoard.toString() + "\n" + theSolver.toString() + '\n';
                System.out.println(maxSolution);
            } else {
                System.out.println("\n***** run #" + i + "  -  current maxMoves still is " + maxMoves + " *****\n");
            }
        }
        
        final long seconds = (new Date().getTime() - startDate.getTime() + 500) / 1000;
        System.out.println("finished.  runTime: " + seconds + " seconds  (" +
                ((double)numGames / seconds) + " games per second)");
    }
    
    @SuppressWarnings("unused")
    private static long getBytesUsed() {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();
        final long totalMem = Runtime.getRuntime().totalMemory();
        final long freeMem  = Runtime.getRuntime().freeMemory();
        return totalMem - freeMem;
    }
    
    
    @SuppressWarnings("unused")
    private static void runTestKeyDepthMap() throws InterruptedException {
//        final Board board = Board.createBoardRandom(5);
        final Board board = Board.createBoardGameID("0765+42+2E21BD0F+93");
        
        System.err.println("GameID\t#Sol\t#Mov\tmsGnrc\tmsSpcl\tMBgnrc\tMBspcl");
        for(;;) {
            board.setRobotsRandom();
//            board.setGoalRandom();
            final StringBuilder sb = new StringBuilder();
            sb.append(board.getGameID()).append('\t');
            
            KeyDepthMapFactory.setDefaultClass(KeyDepthMapTrieGeneric.class);
            final SolverIDDFS solverGeneric = (SolverIDDFS)Solver.createInstance(board);
            final List<Solution> solutionsGeneric = solverGeneric.execute();
            
            KeyDepthMapFactory.setDefaultClass(KeyDepthMapTrieSpecial.class);
            final SolverIDDFS solverSpecial = (SolverIDDFS)Solver.createInstance(board);
            final List<Solution> solutionsSpecial = solverSpecial.execute();
            
            sb.append(solutionsGeneric.size()).append('\t');
            sb.append(solutionsGeneric.get(0).size()).append('\t');
            sb.append(solverGeneric.getSolutionMilliSeconds()).append('\t');
            sb.append(solverSpecial.getSolutionMilliSeconds()).append('\t');
            sb.append(solverGeneric.getSolutionMemoryMegabytes()).append('\t');
            sb.append(solverSpecial.getSolutionMemoryMegabytes()).append('\t');
            System.err.println(sb);
            
            if (!solutionsGeneric.equals(solutionsSpecial)) {
                System.err.println("solutions are not equal!");
                System.out.println("solutions are not equal!");
                break;
            }
        }
    }
}
