package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.controller.resource.Resource;

/**
 * Information about a connector.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ConnectorInfo
{
    /**
     * State of connection between a connector and a device it manages.
     */
    public static enum ConnectionState
    {
        /**
         * The connection is established.
         */
        CONNECTED,
        /**
         * The connection is not established.
         */
        DISCONNECTED,
    }

    private String name;
    private Resource device;
    private ConnectionState connectionState;
    private DeviceState deviceState;


    /**
     * @return connection state to the device
     */
    public ConnectionState getConnectionState()
    {
        return connectionState;
    }

    /**
     * @param connectionState   connection state to the device
     */
    public void setConnectionState(ConnectionState connectionState)
    {
        this.connectionState = connectionState;
    }

    /**
     * @return the device managed by this connector
     */
    public Resource getDevice()
    {
        return device;
    }

    /**
     * @param device    the device managed by this connector (must be a resource of type ManagedDevice)
     */
    public void setDevice(Resource device)
    {
        if (device.getType() != Resource.Type.DEVICE) {
            throw new IllegalArgumentException("Invalid type of resource, a device is expected.");
        }
        this.device = device;
    }

    /**
     * @return state of the device, maintained by the connector for performance reasons
     */
    public DeviceState getDeviceState()
    {
        return deviceState;
    }

    /**
     * @param deviceState    state of the device, maintained by the connector for performance reasons
     */
    public void setDeviceState(DeviceState deviceState)
    {
        this.deviceState = deviceState;
    }

    /**
     * @return the connector name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name    the connector name
     */
    public void setName(String name)
    {
        this.name = name;
    }
}
