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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BrainCanevasMainA extends Brain {
    //--CONSTANTS--//
    private static final double POSITION_ROCKY_X = 1500;
    private static final double POSITION_ROCKY_Y = 800;
    private static final double POSITION_VANDAMME_X = 1500;
    private static final double POSITION_VANDAMME_Y = 1000;
    private static final double POSITION_THOR_X = 1500;
    private static final double POSITION_THOR_Y = 1200;

    //---PARAMETERS---//
    private static final double HEADINGPRECISION = 0.001;
    private static final double ANGLEPRECISION = 0.15;
    private static final int THOR = 0x1EADDA;
    private static final int ROCKY = 0x5EC0;
    private static final int VANDAMME = 0x1ADA;

    //---VARIABLES---//
    private double myX, myY;
    private double speed;
    private boolean isMoving;
    private int whoAmI;
    private double finalPositionX;
    private double finalPositionY;

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
        finalPositionX = POSITION_ROCKY_X;
        finalPositionY = POSITION_ROCKY_Y;

        if (someoneAtNorth && someoneAtSouth) {
            whoAmI = VANDAMME;
            myX = Parameters.teamAMainBot2InitX;
            myY = Parameters.teamAMainBot2InitY;
            finalPositionX = POSITION_VANDAMME_X;
            finalPositionY = POSITION_VANDAMME_Y;
        } else {
            if (someoneAtNorth) {
                whoAmI = THOR;
                myX = Parameters.teamAMainBot3InitX;
                myY = Parameters.teamAMainBot3InitY;
                finalPositionX = POSITION_THOR_X;
                finalPositionY = POSITION_THOR_Y;
            }
        }

        //INIT
        speed = Parameters.teamAMainBotSpeed;
        isMoving = false;
    }

    public void step() {
        //Odometry
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
            fire(directionEnnemie);
            return;
        }

        for (String str: fetchAllMessages()) {
            String[] msg = str.split(",");
            if (msg[0].equals("Enemies")) {
                double x = Double.valueOf(msg[1]);
                double y = Double.valueOf(msg[2]);
                if (calculDistanceFromPoint(x,y) < Parameters.bulletRange) {
                    fire(getDirectionFromPoint(x, y));
                    return;
                }
            }
        }

        if (!isInPosition(finalPositionX, finalPositionY)) {
            moveTo(finalPositionX, finalPositionY);
        } else {
            sendLogMessage("Finish: (" + myX + ";" + myY + ")");
            fire(Math.random()*Math.PI*2);
            switch (whoAmI) {
                case ROCKY:
                    fire(ThreadLocalRandom.current().nextDouble(-Math.PI/4, Math.PI/4));
                    break;
                case VANDAMME:
                    fire(ThreadLocalRandom.current().nextDouble(-Math.PI/4, Math.PI/4));
                    break;
                case THOR:
                    fire(ThreadLocalRandom.current().nextDouble(-Math.PI/4, Math.PI/4));
                    break;
            }
        }
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
        return direction < 0 ? direction + 2 * Math.PI : direction;
    }

    private boolean isInPosition(double x, double y) {
        return calculDistanceFromPoint(x, y) < 10;
    }

    private boolean isEnemies(IRadarResult.Types type) {
        return type == IRadarResult.Types.OpponentMainBot || type == IRadarResult.Types.OpponentSecondaryBot;
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

    private double sqr(double value) {
        return value * value;
    }

    private double getXfromDirectionAndDistance(double dir, double distance) {
        return myX + (distance * Math.cos(dir));
    }

    private double getYfromDirectionAndDistance(double dir, double distance) {
        return myY + (distance * Math.sin(dir));
    }

    private double calculDistanceFromPoint(double x, double y) {
        return Math.sqrt(sqr(x - myX) + sqr(y - myY));
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(getHeading() - dir)) < HEADINGPRECISION;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        if (dir1 < 0) dir1 = dir1 + 2 * Math.PI;
        if (dir2 < 0) dir2 = dir2 + 2 * Math.PI;
        return Math.abs(dir1 - dir2) < ANGLEPRECISION;
    }
}
