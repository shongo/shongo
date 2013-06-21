package cz.cesnet.shongo.connector.jade;

import cz.cesnet.shongo.connector.ConnectorAgent;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.connector.api.ConnectorOptions;
import cz.cesnet.shongo.jade.Agent;
import cz.cesnet.shongo.jade.LocalCommand;
import cz.cesnet.shongo.jade.LocalCommandException;

/**
 * {@link LocalCommand} for starting managing a device by a connector agent.
 * <p/>
 * Initializes the agent to manage a device and connects to it.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ManageLocalCommand extends LocalCommand
{
    /**
     * Connector class to be used to manage the device.
     */
    private String connectorClass;

    /**
     * Address of the device to manage.
     */
    private String deviceAddress;

    /**
     * Port of the device to connect to.
     */
    private int devicePort;

    /**
     * Username for managing the device.
     */
    private String authUsername;

    /**
     * Password for managing the device.
     */
    private String authPassword;

    /**
     * Options for managing the device.
     */
    private ConnectorOptions options;


    public ManageLocalCommand(String connectorClass, String deviceAddress, int devicePort, String authUsername,
            String authPassword, ConnectorOptions options)
    {
        this.connectorClass = connectorClass;
        this.deviceAddress = deviceAddress;
        this.devicePort = devicePort;
        this.authUsername = authUsername;
        this.authPassword = authPassword;
        this.options = options;
    }

    public String getAuthPassword()
    {
        return authPassword;
    }

    public void setAuthPassword(String authPassword)
    {
        this.authPassword = authPassword;
    }

    public String getAuthUsername()
    {
        return authUsername;
    }

    public void setAuthUsername(String authUsername)
    {
        this.authUsername = authUsername;
    }

    public String getConnectorClass()
    {
        return connectorClass;
    }

    public void setConnectorClass(String connectorClass)
    {
        this.connectorClass = connectorClass;
    }

    public String getDeviceAddress()
    {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress)
    {
        this.deviceAddress = deviceAddress;
    }

    public int getDevicePort()
    {
        return devicePort;
    }

    public void setDevicePort(int devicePort)
    {
        this.devicePort = devicePort;
    }

    public ConnectorOptions getOptions()
    {
        return options;
    }

    public void setOptions(ConnectorOptions options)
    {
        this.options = options;
    }

    @Override
    public void process(Agent localAgent) throws LocalCommandException
    {
        if (!(localAgent instanceof ConnectorAgent)) {
            throw new IllegalArgumentException("Manage command works only with instances of " + ConnectorAgent.class);
        }
        ConnectorAgent connectorAgent = (ConnectorAgent) localAgent;

        try {
            connectorAgent.manage(connectorClass, deviceAddress, devicePort, authUsername, authPassword, options);
        }
        catch (ConnectorInitException exception) {
            throw new LocalCommandException("Error initializing the connector", exception);
        }
    }
}
