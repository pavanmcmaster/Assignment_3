package ca.mcmaster.se2aa4.island.teamXXX;

public class EastDirection implements MovementDirection {
    @Override
    public int moveX(int x) { //moving one step east is x+1
        return x + 1;
    }

    @Override
    public int moveY(int y) { //don't change y for east
        return y;
    }
}
