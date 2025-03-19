package ca.mcmaster.se2aa4.island.teamXXX;

import static eu.ace_design.island.runner.Runner.run;

import java.io.File;

public class Runner {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Runner <mapFile>");
            System.exit(1);
        }
        String file = args[0];

        try {
            run(Explorer.class)
                .exploring(new File(file))
                .withSeed(42L)
                .startingAt(1, 1, "EAST")
                .backBefore(7000)
                .withCrew(5)
                .collecting(1000, "WOOD")
                .storingInto("./outputs")
                .withName("Island")
                .fire();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
