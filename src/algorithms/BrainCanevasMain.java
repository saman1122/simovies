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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BrainCanevasMain extends Brain {
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
    private Position myPosition;
    private Position finalPosition;
    private double speed;
    private boolean isMoving;
    private int whoAmI;
    private List<Position> friends;
    private double MIN_RAND, MAX_RAND;

    //---CONSTRUCTORS---//
    public BrainCanevasMain() {
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

        Position rockyPos, vandammePos, thorPos;
        if (isSameDirection(getHeading(), Parameters.EAST)) {
            rockyPos = new Position(Parameters.teamAMainBot1InitX, Parameters.teamAMainBot1InitY);
            vandammePos = new Position(Parameters.teamAMainBot2InitX, Parameters.teamAMainBot2InitY);
            thorPos = new Position(Parameters.teamAMainBot3InitX, Parameters.teamAMainBot3InitY);
            MIN_RAND = -Math.PI/4;
            MAX_RAND = Math.PI/4;

        } else {
            rockyPos = new Position(Parameters.teamBMainBot1InitX, Parameters.teamBMainBot1InitY);
            vandammePos = new Position(Parameters.teamBMainBot2InitX, Parameters.teamBMainBot2InitY);
            thorPos = new Position(Parameters.teamBMainBot3InitX, Parameters.teamBMainBot3InitY);
            MIN_RAND = 2*Math.PI/3;
            MAX_RAND = 4*Math.PI/3;
        }

        whoAmI = ROCKY;
        myPosition = rockyPos;
        finalPosition = new Position(POSITION_ROCKY_X, POSITION_ROCKY_Y);

        if (someoneAtNorth && someoneAtSouth) {
            whoAmI = VANDAMME;
            myPosition = vandammePos;
            finalPosition = new Position(POSITION_VANDAMME_X, POSITION_VANDAMME_Y);
        } else {
            if (someoneAtNorth) {
                whoAmI = THOR;
                myPosition = thorPos;
                finalPosition = new Position(POSITION_THOR_X, POSITION_THOR_Y);
            }
        }

        //INIT
        speed = Parameters.teamAMainBotSpeed;
        isMoving = false;
        friends = new ArrayList<>();
    }

    public void step() {
        //Odometry
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
            broadcast("Enemies," + getPostionfromDirectionAndDistance(directionEnnemie, distanceEnnemie).toString());
            myFire(directionEnnemie);
            return;
        }

        List<Position> enemies = new ArrayList<>();
        friends.clear();

        for (String str : fetchAllMessages()) {
            if (str.startsWith("Enemies")) {
                enemies.add(new Position(str));
            } else {
                friends.add(new Position());
            }
        }

        for(Position ennemiePos: enemies) {
            if (calculDistanceFromPoint(ennemiePos) < Parameters.bulletRange) {
                myFire(getDirectionFromPoint(ennemiePos));
                return;
            }
        }

        if (!isInPosition(finalPosition)) {
            moveTo(finalPosition);
        } else {
            sendLogMessage("Finish: (" + myPosition.toString() + ")");
            myFire(ThreadLocalRandom.current().nextDouble(MIN_RAND, MAX_RAND));
        }
    }

    private void myFire(double dir) {
        //boolean friendBeetwen = friends.stream().anyMatch(friendPos -> isMyFriendBeetwen(dir, friendPos));

        //if (!friendBeetwen) {
            fire(dir);
        //} else move();
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
        double myX = myPosition.x;
        double myY = myPosition.y;
        double direction = myX - position.x < 0 ? Math.atan((myY - position.y) / (myX - position.x)) : Math.PI + Math.atan((myY - position.y) / (myX - position.x));
        return direction < 0 ? direction + 2 * Math.PI : direction;
    }

    private boolean isInPosition(Position position) {
        return calculDistanceFromPoint(position) < 10;
    }

    private boolean isEnemies(IRadarResult.Types type) {
        return type == IRadarResult.Types.OpponentMainBot || type == IRadarResult.Types.OpponentSecondaryBot;
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

    private double sqr(double value) {
        return value * value;
    }

    private Position getPostionfromDirectionAndDistance(double dir, double distance) {
        double x = myPosition.x + (distance * Math.cos(dir));
        double y = myPosition.y + (distance * Math.sin(dir));
        return new Position(x, y);
    }

    private double calculDistanceFromPoint(Position position) {
        return Math.sqrt(sqr(position.x - myPosition.x) + sqr(position.y - myPosition.y));
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(getHeading() - dir)) < HEADINGPRECISION;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        if (dir1 < 0) dir1 = dir1 + 2 * Math.PI;
        if (dir2 < 0) dir2 = dir2 + 2 * Math.PI;
        return Math.abs(dir1 - dir2) < ANGLEPRECISION;
    }

    private boolean isMyFriendBeetwen(double dir, Position friendPos) {
        return (Math.abs(getDirectionFromPoint(friendPos) - dir) < Math.PI / 12.0);
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
