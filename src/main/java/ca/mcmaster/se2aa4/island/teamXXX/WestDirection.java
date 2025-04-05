package ca.mcmaster.se2aa4.island.teamXXX;

public class WestDirection implements MovementDirection {
    @Override
    public int moveX(int x) {
        return x - 1;  //moving one step west is x-1
    }

    @Override
    public int moveY(int y) {
        return y;  //there's no change in y for west
    }
}
