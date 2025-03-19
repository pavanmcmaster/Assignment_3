package ca.mcmaster.se2aa4.island.teamXXX;

import static eu.ace_design.island.runner.Runner.run;

import java.io.File;

/**
 * A Runner that starts the exploration with specific parameters,
 * calls your Explorer, and saves the results in ./outputs.
 */
public class Runner {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java Runner <scenarioFile>");
            System.exit(1);
        }
        String filename = args[0];

        try {
            run(Explorer.class)
                    .exploring(new File(filename))
                    .withSeed(42L)
                    .startingAt(1, 1, "EAST")
                    .backBefore(7000)
                    .withCrew(5)
                    .collecting(1000, "WOOD")
                    .storingInto("./outputs")
                    .withName("Island")
                    .fire();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
