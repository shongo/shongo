package cz.cesnet.shongo.connector.api.ontology.actions.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * Command to switch an endpoint to the standby mode.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class StandBy extends ConnectorAgentAction
{
    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getEndpoint(connector).standBy();
        return null;
    }

    public String toString()
    {
        return "Standby agent action";
    }
}