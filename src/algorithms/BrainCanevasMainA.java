/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BrainCanevas.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;
import characteristics.IFrontSensorResult;

public class BrainCanevasMainA extends Brain {
    //---PARAMETERS---//
    private static final double HEADINGPRECISION = 0.001;
    private static final double ANGLEPRECISION = 0.1;
    private static final int THOR = 0x1EADDA;
    private static final int ROCKY = 0x5EC0;
    private static final int VANDAMME = 0x1ADA;

    //---VARIABLES---//
    private boolean turnNorthTask, turnLeftTask;
    private double endTaskDirection;
    private double myX, myY;
    private double xMax, yMax;
    private double speed, detectionRange;
    private boolean isMoving;
    private int whoAmI;

    //---CONSTRUCTORS---//
    public BrainCanevasMainA() {
        super();
    }

    //---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
        //ODOMETRY CODE
        boolean someoneAtNorth = false, someoneAtSouth = false;
        for (IRadarResult o : detectRadar()) {
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) someoneAtNorth = true;
            if (isSameDirection(o.getObjectDirection(), Parameters.SOUTH)) someoneAtSouth = true;
        }

        whoAmI = ROCKY;
        myX = Parameters.teamAMainBot1InitX;
        myY = Parameters.teamAMainBot1InitY;

        if (someoneAtNorth && someoneAtSouth) {
            whoAmI = VANDAMME;
            myX = Parameters.teamAMainBot2InitX;
            myY = Parameters.teamAMainBot2InitY;
        } else {
            if (someoneAtNorth) {
                whoAmI = THOR;
                myX = Parameters.teamAMainBot3InitX;
                myY = Parameters.teamAMainBot3InitY;
            }
        }

        //INIT
        xMax = 0;
        yMax = 0;
        speed = Parameters.teamAMainBotSpeed;
        detectionRange = Parameters.teamBMainBotFrontalDetectionRange;
        turnNorthTask = true;
        turnLeftTask = false;
        isMoving = false;
    }

    public void step() {
        //ODOMETRY CODE
        if (isMoving) {
            myX += speed * Math.cos(getHeading());
            myY += speed * Math.sin(getHeading());

            if (myX + detectionRange > xMax) xMax = myX + detectionRange;
            if (myY + detectionRange > yMax) yMax = myY + detectionRange;

            isMoving = false;
        }
        //DEBUG MESSAGE
        String message = "";
        switch (whoAmI) {
            case ROCKY:
                message = "Rocky ";
                break;
            case VANDAMME:
                message = "Van Damme ";
                break;
            case THOR:
                message = "Thor ";
                break;
            default:
                break;
        }

        sendLogMessage(message + "position (" + (int) myX + ", " + (int) myY + "). xMax: " + xMax + " | yMax: " + yMax);


        //AUTOMATON
        if (turnNorthTask && isHeading(Parameters.NORTH)) {
            turnNorthTask = false;
            myMove();
            //sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (turnNorthTask && !isHeading(Parameters.NORTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            //sendLogMessage("Initial TeamB position. Heading North!");
            return;
        }
        if (turnLeftTask && isHeading(endTaskDirection)) {
            turnLeftTask = false;
            myMove();
            //sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (turnLeftTask && !isHeading(endTaskDirection)) {
            stepTurn(Parameters.Direction.LEFT);
            //sendLogMessage("Iceberg at 12 o'clock. Heading 9!");
            return;
        }
        if (!turnNorthTask && !turnLeftTask && detectFront().getObjectType() == IFrontSensorResult.Types.WALL) {
            turnLeftTask = true;
            endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
            stepTurn(Parameters.Direction.LEFT);
            //sendLogMessage("Iceberg at 12 o'clock. Heading 9!");
            return;
        }
        if (!turnNorthTask && !turnLeftTask && detectFront().getObjectType() != IFrontSensorResult.Types.WALL) {
            myMove(); //And what to do when blind blocked?
            //sendLogMessage("Moving a head. Waza!");
            return;
        }
    }

    private void myMove() {
        isMoving = true;
        move();
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(getHeading() - dir)) < HEADINGPRECISION;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(dir1 - dir2) < ANGLEPRECISION;
    }
}
