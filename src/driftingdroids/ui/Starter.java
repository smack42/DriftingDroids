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

package driftingdroids.ui;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

import driftingdroids.model.Board;
import driftingdroids.model.Solution;
import driftingdroids.model.Solver;




public class Starter {
    
    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        new SwingGUI("DriftingDroids 1.2 __WIP_2012-11-14__");
        //runTestRandom1000();
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
    
}
