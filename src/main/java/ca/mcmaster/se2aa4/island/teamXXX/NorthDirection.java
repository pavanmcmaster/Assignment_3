package ca.mcmaster.se2aa4.island.teamXXX;

public class NorthDirection implements MovementDirection {
    @Override
    public int moveX(int x) { //don't change x value for north
    	return x;
    }

    @Override
    public int moveY(int y) {
        return y + 1;  // moving one step north is y + 1
    }
}
