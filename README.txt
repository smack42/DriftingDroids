DriftingDroids - yet another Ricochet Robots solver program.

version: (work in progress 2011-12-10)

homepage: https://github.com/smack42/DriftingDroids/wiki



## about

Ricochet Robots is a board game designed by Alex Randolph.
If you don't know the game yet then you can start to read about it here:
http://en.wikipedia.org/wiki/Ricochet_Robot
http://de.wikipedia.org/wiki/Rasende_Roboter
http://boardgamegeek.com/boardgame/51/ricochet-robots

DriftingDroids is a computer version of the Ricochet Robots board game.
It includes a solver algorithm that finds the optimal solutions for every
possible game problem. You can use it as your "trainer" for solo playing
or as a "referee" during real board gaming sessions.



## usage

DriftingDroids is a Java application. Therefore a Java SE Runtime Environment
(JRE version 6 or later) is required to run this program. Download Java here:
http://www.oracle.com/technetwork/java/javase/downloads/index.html

To run DriftingDroids just doubleclick "start.cmd".

The program generates a unique "game ID" for every board configuration, which
includes the layout of the 4 board pieces, the number and positions of robots
on the board, the color and position of the current goal. If you want to save
or share an interesting game problem then just copy&paste the game ID.



## acknowledgements

this program uses the following Java libraries:
- DesignGridLayout  http://designgridlayout.java.net/

tools used to create this program:
- Eclipse   http://www.eclipse.org/
- ProGuard  http://proguard.sourceforge.net/

thanks for ideas and inspiration go to:
- my sister who introduced the Ricochet Robots board game to me.
- the people who wrote review articles or scientific papers about the game.
- the developers who have published their own Ricochet Robots programs.
  special thanks to David Hansel for sending the sources of his fast solver.



## license

  DriftingDroids - yet another Ricochet Robots solver program.
  Copyright (C) 2011  Michael Henke <smack42@googlemail.com>

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

