package ca.mcmaster.se2aa4.island.teamXXX;

public class SouthDirection implements MovementDirection {
    @Override
    public int moveX(int x) { //no change in x for South
        return x;
    }

    @Override
    public int moveY(int y) {
        return y - 1;  //moving one step south is y - 1
    }
}
