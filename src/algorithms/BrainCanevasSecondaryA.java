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
    //--CONSTANTS--//
    private static final double FIRST_POSITION_X = 200;
    private static final double FINAL_POSITION_X = 2800;
    private static final double POSITION_Y_ECLAIREUR = 300;
    private static final double POSITION_Y_ECLAIREUR2 = 1700;
    //---PARAMETERS---//
    private static final double HEADINGPRECISION = 0.001;
    private static final double ANGLEPRECISION = 0.15;
    private static final int ECLAIREUR = 0x1EADDA;
    private static final int ECLAIREUR_AUSSI = 0x5EC0;

    //---VARIABLES---//
    private double endTaskDirection;
    private double myX, myY;
    private double speed;
    private boolean isMoving;
    private int whoAmI;
    private boolean turnTask, turnRight, moveTask;
    private int endTaskCounter;
    private boolean firstMove;
    private double positionX, positionY;

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
            positionY = POSITION_Y_ECLAIREUR;
        } else {
            myX = Parameters.teamASecondaryBot2InitX;
            myY = Parameters.teamASecondaryBot2InitY;
            positionY = POSITION_Y_ECLAIREUR2;
        }

        //INIT
        positionX = FIRST_POSITION_X;
        speed = Parameters.teamASecondaryBotSpeed;
        isMoving = false;
        turnTask = true;
        moveTask = false;
        firstMove = true;
        endTaskDirection = (Math.random() - 0.5) * 0.5 * Math.PI;
        turnRight = (endTaskDirection > 0);
        endTaskDirection += getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
    }

    public void step() {
        //ODOMETRY CODE
        if (isMoving) {
            myX += speed * Math.cos(getHeading());
            myY += speed * Math.sin(getHeading());
            isMoving = false;
        }
        sendLogMessage("My position: (" + myX + ";" + myY + ")");

        boolean ennemieDetected = false;
        double directionEnnemie = 0;
        double distanceEnnemie = Double.MAX_VALUE;
        for (IRadarResult o : detectRadar()) {
            IRadarResult.Types type = o.getObjectType();
            if (isEnemies(type)) {
                ennemieDetected = true;
                double distance = o.getObjectDistance();
                if (distance < distanceEnnemie) {
                    distanceEnnemie = distance;
                    directionEnnemie = o.getObjectDirection();
                }
            }
        }

        if (ennemieDetected) {
            broadcast("Enemies," + getXfromDirectionAndDistance(directionEnnemie, distanceEnnemie) + "," + getYfromDirectionAndDistance(directionEnnemie, distanceEnnemie));
            return;
        }

        if (isInPosition(positionX, positionY)) {
            if (positionX == FIRST_POSITION_X) {
                positionX = FINAL_POSITION_X;
            } else {
                positionX = FIRST_POSITION_X;
            }
        }
        moveTo(positionX, positionY);

        /*

        if (turnTask) {
            if (isHeading(endTaskDirection)) {
                if (firstMove) {
                    firstMove = false;
                    turnTask = false;
                    moveTask = true;
                    endTaskCounter = 400;
                    myMove();
                    sendLogMessage("Moving a head. Waza!");
                    return;
                }
                turnTask = false;
                moveTask = true;
                endTaskCounter = 200;
                myMove();
                sendLogMessage("Moving a head. Waza!");
            } else {
                if (turnRight) stepTurn(Parameters.Direction.RIGHT);
                else stepTurn(Parameters.Direction.LEFT);
            }
            return;
        }
        if (moveTask) {
      /*if (detectFront()!=NOTHING) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      }*/
        /*
            if (endTaskCounter < 0) {
                turnTask = true;
                moveTask = false;
                endTaskDirection = (Math.random() - 0.5) * 2 * Math.PI;
                turnRight = (endTaskDirection > 0);
                endTaskDirection += getHeading();
                if (turnRight) stepTurn(Parameters.Direction.RIGHT);
                else stepTurn(Parameters.Direction.LEFT);
                sendLogMessage("Turning point. Waza!");
            } else {
                endTaskCounter--;
                myMove();
            }
            return;
        }
        return;
    */
    }

    private void moveTo(double x, double y) {
        double direction = getDirectionFromPoint(x, y);
        if (isSameDirection(getHeading(), direction)) {
            myMove();
        } else {
            if (getHeading() > direction) {
                stepTurn(Parameters.Direction.LEFT);
            } else {
                stepTurn(Parameters.Direction.RIGHT);
            }
        }
    }

    private double getDirectionFromPoint(double x, double y) {
        double direction = myX - x < 0 ? Math.atan((myY - y) / (myX - x)) : Math.PI + Math.atan((myY - y) / (myX - x));
        return direction;
    }

    private double getXfromDirectionAndDistance(double dir, double distance) {
        return myX + (distance * Math.cos(dir));
    }

    private double getYfromDirectionAndDistance(double dir, double distance) {
        return myY + (distance * Math.sin(dir));
    }

    private boolean isEnemies(IRadarResult.Types type) {
        return type == IRadarResult.Types.OpponentMainBot || type == IRadarResult.Types.OpponentSecondaryBot;
    }

    private boolean isInPosition(double x, double y) {
        return Math.sqrt(sqr(x - myX) + sqr(y - myY)) < 10;
    }

    private double sqr(double value) {
        return value * value;
    }

    private void myMove() {
        boolean somethingFront = false;
        for (IRadarResult o : detectRadar()) {
            if(isSameDirection(getHeading(), o.getObjectDirection())) somethingFront = true;
        }

        if (somethingFront) {
            stepTurn(Parameters.Direction.LEFT);
        }else {
            isMoving = true;
            move();
        }
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(getHeading() - dir)) < Parameters.teamAMainBotStepTurnAngle;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        if (dir1 < 0) dir1 = dir1 + 2 * Math.PI;
        if (dir2 < 0) dir2 = dir2 + 2 * Math.PI;
        return Math.abs(dir1 - dir2) < ANGLEPRECISION;
    }
}
