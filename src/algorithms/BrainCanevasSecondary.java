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

public class BrainCanevasSecondary extends Brain {
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
    private Position myPosition;
    private double speed;
    private boolean isMoving;
    private int whoAmI;
    private Position destination;

    //---CONSTRUCTORS---//
    public BrainCanevasSecondary() {
        super();
    }

    //---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
        destination = new Position();
        Position eclaireurPos, eclaireurAussiPos;
        if (isSameDirection(getHeading(), Parameters.EAST)) {
            eclaireurPos = new Position(Parameters.teamASecondaryBot1InitX, Parameters.teamASecondaryBot1InitY);
            eclaireurAussiPos = new Position(Parameters.teamASecondaryBot2InitX, Parameters.teamASecondaryBot2InitY);
            destination.x = FIRST_POSITION_X;
        } else {
            eclaireurPos = new Position(Parameters.teamBSecondaryBot1InitX, Parameters.teamBSecondaryBot1InitY);
            eclaireurAussiPos = new Position(Parameters.teamBSecondaryBot2InitX, Parameters.teamBSecondaryBot2InitY);
            destination.x = FINAL_POSITION_X;
        }

        //ODOMETRY CODE
        whoAmI = ECLAIREUR;
        for (IRadarResult o : detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) whoAmI = ECLAIREUR_AUSSI;
        if (whoAmI == ECLAIREUR) {
            myPosition = eclaireurPos;
            destination.y = POSITION_Y_ECLAIREUR;
        } else {
            myPosition = eclaireurAussiPos;
            destination.y = POSITION_Y_ECLAIREUR2;
        }

        //INIT
        speed = Parameters.teamASecondaryBotSpeed;
        isMoving = false;
    }

    public void step() {
        //ODOMETRY CODE
        if (isMoving) {
            myPosition.x += speed * Math.cos(getHeading());
            myPosition.y += speed * Math.sin(getHeading());
            isMoving = false;
        }
        broadcast(myPosition.toString());
        sendLogMessage(myPosition.toString());

        boolean ennemieDetected = false;
        double directionEnnemie = 0;
        double distanceEnnemie = Double.MAX_VALUE;
        for (IRadarResult o : detectRadar()) {
            IRadarResult.Types type = o.getObjectType();
            double distance = o.getObjectDistance();
            if (isEnemies(type)) {
                ennemieDetected = true;
                if (distance < distanceEnnemie) {
                    distanceEnnemie = distance;
                    directionEnnemie = o.getObjectDirection();
                }
            }

            if (type.equals(IRadarResult.Types.BULLET) && distance < 50) {
                moveBack();
            }
        }

        if (ennemieDetected) {
            broadcast("Enemies," + getPostionfromDirectionAndDistance(directionEnnemie, distanceEnnemie).toString());
            return;
        }

        if (isInPosition(destination)) {
            if (destination.x == FIRST_POSITION_X) {
                destination.x = FINAL_POSITION_X;
            } else {
                destination.x = FIRST_POSITION_X;
            }
        }
        moveTo(destination);
    }

    private void moveTo(Position position) {
        double direction = getDirectionFromPoint(position);
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

    private double getDirectionFromPoint(Position position) {
        return myPosition.x - position.x < 0 ?
                Math.atan((myPosition.y - position.y) / (myPosition.x - position.x)) :
                Math.PI + Math.atan((myPosition.y - position.y) / (myPosition.x - position.x));
    }

    private Position getPostionfromDirectionAndDistance(double dir, double distance) {
        double x = myPosition.x + (distance * Math.cos(dir));
        double y = myPosition.y + (distance * Math.sin(dir));
        return new Position(x, y);
    }

    private boolean isEnemies(IRadarResult.Types type) {
        return type == IRadarResult.Types.OpponentMainBot || type == IRadarResult.Types.OpponentSecondaryBot;
    }

    private boolean isInPosition(Position position) {
        return Math.sqrt(sqr(position.x - myPosition.x) + sqr(position.y - myPosition.y)) < 10;
    }

    private double sqr(double value) {
        return value * value;
    }

    private void myMove() {
        boolean somethingFront = false;
        for (IRadarResult o : detectRadar()) {
            if (isSameDirection(getHeading(), o.getObjectDirection())) somethingFront = true;
        }

        if (somethingFront) {
            stepTurn(Parameters.Direction.LEFT);
        } else {
            isMoving = true;
            move();
        }
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(getHeading() - dir)) < Parameters.teamAMainBotStepTurnAngle;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(dir1 - dir2) < ANGLEPRECISION;
    }

    private class Position {
        public double x;
        public double y;

        public Position(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Position(String position) {
            String pos[] = position.split(",");
            int x = 1, y = 2;
            if (pos.length == 4) {
                x++;
                y++;
            }
            this.x = Double.valueOf(pos[x]);
            this.y = Double.valueOf(pos[y]);

        }

        public Position() {
            this.x = 0;
            this.y = 0;
        }

        @Override
        public String toString() {
            return "Position," + x + "," + y;
        }
    }
}
