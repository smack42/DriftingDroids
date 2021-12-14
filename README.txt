DriftingDroids - yet another Ricochet Robots solver program.

Version   1.3.9 (2020-08-30)
Homepage  https://github.com/smack42/DriftingDroids/wiki



## about

Ricochet Robots is a board game designed by Alex Randolph.
If you don't know the game yet then you can start to read about it here:
https://en.wikipedia.org/wiki/Ricochet_Robot
https://de.wikipedia.org/wiki/Rasende_Roboter
https://boardgamegeek.com/boardgame/51/ricochet-robots

DriftingDroids is a computer version of the Ricochet Robots board game.
It includes a solver algorithm that finds the optimal solutions to every
game problem.
You can use it as your trainer for solo playing or as a referee during
real board gaming sessions.



## usage

Java SE Runtime Environment (JRE version 8 or newer) is required to run
this program. You can download Java here:
https://www.oracle.com/java/technologies/javase-downloads.html
https://adoptopenjdk.net/

To run DriftingDroids follow these steps on first run and after each update of the repository:

    ./compilerun.sh & sleep 15 # or quit DriftingDroids
    ./buildrelease.sh
    cd DriftingDroids_release/
    java -jar start.jar

From now on you can start with:
    
    java -jar DriftingDroids_release/start.jar

For more info please read the included documentation.



## license

DriftingDroids - yet another Ricochet Robots solver program.
Copyright (C) 2011-2020 Michael Henke <smack42@gmail.com>

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




This program uses:

DesignGridLayout - Swing LayoutManager that implements "Canonical Grids"
- https://web.archive.org/web/20170409233103/https://java.net/projects/designgridlayout/pages/Home
- https://search.maven.org/artifact/net.java.dev.designgridlayout/designgridlayout
- Copyright 2005-2013 Jason Aaron Osgood, Jean-Francois Poilpret
- DesignGridLayout is open source licensed under the Apache License 2.0

FlatLaf - Flat Look and Feel (with Darcula/IntelliJ themes support)
- https://www.formdev.com/flatlaf
- https://github.com/JFormDesigner/FlatLaf
- Copyright 2019 FormDev Software GmbH
- FlatLaf is open source licensed under the Apache License 2.0
