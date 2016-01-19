package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.RoomLayout;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.Alias;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Set;

/**
 * Get room's info.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class GetRoom extends AbstractDomainRoomAction
{

    @Override
    public ConnectorCommand toApi()
    {
        cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom getRoom;
        getRoom = new cz.cesnet.shongo.connector.api.jade.multipoint.GetRoom();

        return getRoom;
    }
}
