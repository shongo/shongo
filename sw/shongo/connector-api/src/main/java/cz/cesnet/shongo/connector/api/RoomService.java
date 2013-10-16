package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface RoomService
{
    /**
     * Lists all rooms at the device.
     *
     * @return array of rooms
     */
    Collection<RoomSummary> getRoomList() throws CommandException, CommandUnsupportedException;

    /**
     * Gets info about an existing room.
     *
     * @param roomId id of the room to get info about
     * @return information about a room with roomId
     */
    Room getRoom(String roomId) throws CommandException, CommandUnsupportedException;

    /**
     * Create a new virtual room on a multipoint device that is managed by this connector.
     *
     * @param room room settings
     * @return identifier of the created room, unique within the device, to be used for further identification of the
     *         room as the roomId parameter
     */
    String createRoom(Room room) throws CommandException, CommandUnsupportedException;

    /**
     * Modifies a virtual room.
     * <p/>
     * The attributes may name any of Room attributes (see constants in the Room class).
     *
     * @param room room to be modified
     * @return new room identifier (shall be the same for most connectors, but may change due to changes in some
     *         attributes)
     */
    String modifyRoom(Room room)
            throws CommandException, CommandUnsupportedException;

    /**
     * Deletes a virtual room.
     *
     * @param roomId room identifier
     */
    void deleteRoom(String roomId) throws CommandException, CommandUnsupportedException;

    /**
     * Gets current settings of a room exported to XML.
     *
     * @param roomId room identifier
     * @return room settings in XML
     */
    String exportRoomSettings(String roomId) throws CommandException, CommandUnsupportedException;

    /**
     * Sets up a room according to given settings previously exported by the <code>exportRoomSettings</code> method.
     *
     * @param roomId   room identifier
     * @param settings room settings in XML, previously returned by the exportRoomSettings method
     */
    void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException;

    /**
     * Lists all participants present in a virtual room.
     *
     * @param roomId room identifier
     * @return collection of room participants
     */
    Collection<RoomParticipant> listRoomParticipants(String roomId)
            throws CommandException, CommandUnsupportedException;

    /**
     * Gets participant information and settings in a room.
     *
     * @param roomId            room identifier
     * @param roomParticipantId identifier of the user within the given room
     * @return description of the user
     */
    RoomParticipant getRoomParticipant(String roomId, String roomParticipantId)
            throws CommandException, CommandUnsupportedException;

    /**
     * Gets a snapshots of the participants in a room.
     *
     * @param roomId             room identifier where the user resides
     * @param roomParticipantIds identifiers of the participants within the room
     * @return map of image data for each participant identifier
     */
    Map<String, MediaData> getRoomParticipantSnapshots(String roomId, Set<String> roomParticipantIds)
            throws CommandException, CommandUnsupportedException;

    /**
     * Modifies participant settings in the room.
     * <p/>
     * Suitable for setting microphone/playback level, muting/unmuting, user layout, ...
     *
     * @param roomParticipant room participant
     */
    void modifyRoomParticipant(RoomParticipant roomParticipant)
            throws CommandException, CommandUnsupportedException;

    /**
     * Dials a user by an alias and adds him/her to the room.
     *
     * @param roomId identifier of room to which to add the user
     * @param alias  alias under which the user is callable
     * @return identifier assigned to the user within the given room (generated by the connector)
     */
    String dialRoomParticipant(String roomId, Alias alias) throws CommandException, CommandUnsupportedException;

    /**
     * Disconnects a user from a room.
     *
     * @param roomId            room identifier
     * @param roomParticipantId identifier of the user within the given room
     */
    void disconnectRoomParticipant(String roomId, String roomParticipantId)
            throws CommandException, CommandUnsupportedException;
}
