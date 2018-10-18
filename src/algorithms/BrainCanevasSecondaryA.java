/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BrainCanevas.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

public class BrainCanevasSecondaryA extends Brain {
    //---PARAMETERS---//
    private static final double HEADINGPRECISION = 0.001;
    private static final double ANGLEPRECISION = 0.1;
    private static final int ECLAIREUR = 0x1EADDA;
    private static final int ECLAIREUR_AUSSI = 0x5EC0;

    //---VARIABLES---//
    private boolean turnNorthTask, turnLeftTask;
    private double endTaskDirection;
    private double myX, myY;
    private double xMax, yMax;
    private double speed, detectionRange;
    private boolean isMoving;
    private int whoAmI;

    //---CONSTRUCTORS---//
    public BrainCanevasSecondaryA() {
        super();
    }

    //---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
        //ODOMETRY CODE
        whoAmI = ECLAIREUR;
        for (IRadarResult o : detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) whoAmI = ECLAIREUR_AUSSI;
        if (whoAmI == ECLAIREUR) {
            myX = Parameters.teamASecondaryBot1InitX;
            myY = Parameters.teamASecondaryBot1InitY;
        } else {
            myX = Parameters.teamASecondaryBot2InitX;
            myY = Parameters.teamASecondaryBot2InitY;
        }

        //INIT
        xMax = 0;
        yMax = 0;
        speed = Parameters.teamASecondaryBotSpeed;
        detectionRange = Parameters.teamBSecondaryBotFrontalDetectionRange;
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
        String message;
        if (whoAmI == ECLAIREUR) {
            message = "Eclaireur ";
        } else {
            message = "Eclaireur_Aussi ";
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
