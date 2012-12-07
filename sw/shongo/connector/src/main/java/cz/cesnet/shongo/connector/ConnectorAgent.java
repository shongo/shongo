package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.connector.api.ontology.ConnectorOntology;
import cz.cesnet.shongo.jade.UnknownAgentActionException;
import cz.cesnet.shongo.jade.command.AgentActionResponderBehaviour;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;
import jade.content.AgentAction;
import jade.core.AID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Jade Agent for Device Connector
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConnectorAgent extends Agent
{
    private static Logger logger = LoggerFactory.getLogger(ConnectorAgent.class);

    private CommonService connector;

    @Override
    protected void setup()
    {
        addOntology(ConnectorOntology.getInstance());
        addBehaviour(new AgentActionResponderBehaviour(this));

        super.setup();

        registerService("connector", "Connector Service");
    }

    @Override
    protected void takeDown()
    {
        if (connector != null) {
            try {
                connector.disconnect();
            }
            catch (CommandException e) {
                // just suppress the exception, the agent is going not to be working anyway
            }
        }
        super.takeDown();
    }

    /**
     * Starts managing a device. Initializes a connector to the device.
     */
    public void manage(String connectorClass, String address, int port, String username, String password)
            throws ConnectorInitException, CommandException
    {
        try {
            Constructor co = Class.forName(connectorClass).getConstructor();
            connector = (CommonService) co.newInstance();
            if (connector == null) {
                throw new ConnectorInitException("Invalid connector class: " + connectorClass + " (must implement the CommonService interface)");
            }

            connector.connect(new Address(address, port), username, password);

            logger.info("Connector ready: {}", connector.getConnectorInfo());
        }
        catch (NoSuchMethodException e) {
            throw new ConnectorInitException(
                    "Invalid connector class: " + connectorClass + " (does not define an appropriate constructor)",
                    e
            );
        }
        catch (ClassNotFoundException e) {
            throw new ConnectorInitException("Connector class not found: " + connectorClass, e);
        }
        catch (InvocationTargetException e) {
            throw new ConnectorInitException("Connector class init failed", e);
        }
        catch (InstantiationException e) {
            throw new ConnectorInitException("Connector class init failed", e);
        }
        catch (IllegalAccessException e) {
            throw new ConnectorInitException("Connector class not accessible: " + connectorClass, e);
        }
    }

    @Override
    public Object handleAgentAction(AgentAction action, AID sender)
            throws UnknownAgentActionException, CommandException, CommandUnsupportedException
    {
        if (getArguments()[0].equals(Boolean.TRUE)) {
            // the connector is configured with <dump>true</dump>
            System.out.println(action.toString());
            // FIXME: return something (an exception is thrown by super.handleAgentAction())
        }
        else if (action instanceof ConnectorAgentAction) {
            return ((ConnectorAgentAction) action).exec(connector);
        }
        return super.handleAgentAction(action, sender);
    }
}
