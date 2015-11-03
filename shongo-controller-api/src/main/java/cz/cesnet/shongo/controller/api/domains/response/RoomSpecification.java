package cz.cesnet.shongo.controller.api.domains.response;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;

import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableState;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a room specification for foreign {@link Reservation}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RoomSpecification extends ForeignSpecification
{
    @JsonProperty("licenseCount")
    private Integer licenseCount;

    @JsonProperty("meetingName")
    private String meetingName;

    @JsonProperty("technologies")
    private Set<Technology> technologies;

    @JsonProperty("aliases")
    private List<Alias> aliases;

    @JsonProperty("state")
    private RoomState state;

    @JsonCreator
    public RoomSpecification(@JsonProperty("licenseCount") Integer licenseCount,
                             @JsonProperty("meetingName") String meetingName,
                             @JsonProperty("technologies") Set<Technology> technologies,
                             @JsonProperty("aliases") List<Alias> aliases)
    {
        this.licenseCount = licenseCount;
        this.meetingName = meetingName;
        this.technologies = technologies;
        this.aliases = aliases;
    }

    public RoomSpecification()
    {
    }

    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    public void addTechnology(Technology technology)
    {
        if (technologies == null) {
            technologies = new HashSet<>();
        }
        this.technologies.add(technology);
    }

    public List<Alias> getAliases()
    {
        return aliases;
    }

    public void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    public void addAlias(AliasType aliasType, String value)
    {
        if (aliases == null) {
            aliases = new ArrayList<>();
        }
        this.aliases.add(new Alias(aliasType, value));
    }

    public String getMeetingName()
    {
        return meetingName;
    }

    public void setMeetingName(String meetingName)
    {
        this.meetingName = meetingName;
    }

    public Integer getLicenseCount()
    {
        return licenseCount;
    }

    public void setLicenseCount(Integer licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    public RoomState getState()
    {
        return state;
    }

    public void setState(RoomState state)
    {
        this.state = state;
    }

    /**
     * Room state (mapped to {@link ExecutableState})
     */
    public enum RoomState
    {
        /**
         * Room has not been started yet.
         */
        NOT_STARTED,

        /**
         * Room is already started.
         */
        STARTED,

        /**
         * Room failed to start.
         */
        STARTING_FAILED,

        /**
         * Room has been already stopped.
         */
        STOPPED,

        /**
         * Room failed to stop.
         */
        STOPPING_FAILED;

        /**
         * @return converted to {@link cz.cesnet.shongo.controller.api.ExecutableState}
         */
        public ExecutableState toApi()
        {
            switch (this) {
                case NOT_STARTED:
                    return ExecutableState.NOT_STARTED;
                case STARTED:
                    return ExecutableState.STARTED;
                case STARTING_FAILED:
                    return ExecutableState.STARTING_FAILED;
                case STOPPED:
                    return ExecutableState.STOPPED;
                case STOPPING_FAILED:
                    return ExecutableState.STOPPING_FAILED;
                default:
                    throw new RuntimeException("Cannot convert " + this.toString() + " to API.");
            }
        }

        /**
         * @return created from {@link cz.cesnet.shongo.controller.api.ExecutableState}
         */
        public static RoomState fromApi(ExecutableState api)
        {
            switch (api) {
                case NOT_STARTED:
                    return RoomState.NOT_STARTED;
                case STARTED:
                    return RoomState.STARTED;
                case STARTING_FAILED:
                    return RoomState.STARTING_FAILED;
                case STOPPED:
                    return RoomState.STOPPED;
                case STOPPING_FAILED:
                    return RoomState.STOPPING_FAILED;
                default:
                    throw new RuntimeException("Cannot create " + api.toString() + " from API.");
            }
        }
    }
}
