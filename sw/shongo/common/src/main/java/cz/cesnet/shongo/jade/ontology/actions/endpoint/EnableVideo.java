package cz.cesnet.shongo.jade.ontology.actions.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

/**
 * Command for an endpoint to enable video.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class EnableVideo extends ConnectorAgentAction
{
    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("Enabling video");
        getEndpoint(connector).enableVideo();
        return null;
    }

    public String toString()
    {
        return "EnableVideo agent action";
    }
}
