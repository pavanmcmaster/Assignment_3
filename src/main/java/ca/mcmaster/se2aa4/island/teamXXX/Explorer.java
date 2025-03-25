package ca.mcmaster.se2aa4.island.teamXXX;

import eu.ace_design.island.bot.IExplorerRaid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.StringReader;

/**
 * Orchestrator class implementing IExplorerRaid.
 * Delegates snippet-based logic to SnippetLogic.
 */
public class Explorer implements IExplorerRaid {

    private static final Logger logger = LogManager.getLogger(Explorer.class);

    private SnippetLogic snippetLogic;

    @Override
    public void initialize(String input) {
        logger.info("Initializing Explorer...");

        JSONObject initJson = new JSONObject(new JSONTokener(new StringReader(input)));
        logger.info("Initialization data:\n{}", initJson.toString(2));

        // Create the snippet logic component and run snippet-style initialization
        this.snippetLogic = new SnippetLogic();
        snippetLogic.initializeSnippet(initJson);

        logger.info("Explorer initialization complete.");
    }

    @Override
    public String takeDecision() {
        // Pass along to snippet logic
        return snippetLogic.snippetTakeDecision();
    }

    @Override
    public void acknowledgeResults(String results) {
        snippetLogic.snippetAcknowledgeResults(results);

        // If environment forcibly placed us out of range, we can stop.
        if (!snippetLogic.isInBounds()) {
            logger.warn("Detected out-of-range position from environment. Stopping mission.");
            snippetLogic.forceStop(); // So next decision is 'stop'
        }
    }

    @Override
    public String deliverFinalReport() {
        return snippetLogic.snippetDeliverFinalReport();
    }
}
