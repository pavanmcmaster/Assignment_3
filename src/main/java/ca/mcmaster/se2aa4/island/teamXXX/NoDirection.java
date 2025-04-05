package ca.mcmaster.se2aa4.island.teamXXX;

public class NoDirection implements MovementDirection {
    @Override
    public int moveX(int x) {
        return x;  //don't change x for unknown direction
    }

    @Override
    public int moveY(int y) {
        return y;  //don't change y for unknown direction
    }
}
