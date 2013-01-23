package cz.cesnet.shongo.controller.request;


import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.AliasReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a {@link Specification} for an {@link Alias}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AliasSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * Restricts {@link AliasType} for allocation of {@link Alias}.
     */
    private Set<AliasType> aliasTypes = new HashSet<AliasType>();

    /**
     * Restricts {@link Technology} for allocation of {@link Alias}.
     */
    private Set<Technology> aliasTechnologies = new HashSet<Technology>();

    /**
     * Requested {@link String} value for the {@link Alias}.
     */
    private String value;

    /**
     * {@link AliasProviderCapability} from which the {@link Alias} should be allocated.
     */
    private AliasProviderCapability aliasProviderCapability;

    /**
     * Constructor.
     */
    public AliasSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param aliasType  to be added to the {@link #aliasTypes}
     */
    public AliasSpecification(AliasType aliasType)
    {
        addAliasType(aliasType);
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     * @param aliasType  to be added to the {@link #aliasTypes}
     */
    public AliasSpecification(Technology technology, AliasType aliasType)
    {
        addAliasTechnology(technology);
        addAliasType(aliasType);
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public AliasSpecification(Technology technology, AliasProviderCapability aliasProviderCapability)
    {
        addAliasTechnology(technology);
        setAliasProviderCapability(aliasProviderCapability);
    }

    /**
     * Constructor.
     *
     * @param technology to be added to the {@link #technologies}
     */
    public AliasSpecification(Technology technology)
    {
        addAliasTechnology(technology);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<AliasType> getAliasTypes()
    {
        return Collections.unmodifiableSet(aliasTypes);
    }

    /**
     * @param aliasTypes sets the {@link #aliasTypes}
     */
    public void setAliasTypes(Set<AliasType> aliasTypes)
    {
        this.aliasTypes.clear();
        this.aliasTypes.addAll(aliasTypes);
    }

    /**
     * @param aliasType to be added to the {@link #aliasTypes}
     */
    public void addAliasType(AliasType aliasType)
    {
        aliasTypes.add(aliasType);
    }

    /**
     * @param aliasType to be removed from the {@link #aliasTypes}
     */
    public void removeAliasType(AliasType aliasType)
    {
        aliasTypes.remove(aliasType);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getAliasTechnologies()
    {
        return Collections.unmodifiableSet(aliasTechnologies);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setAliasTechnologies(Set<Technology> technologies)
    {
        this.aliasTechnologies.clear();
        this.aliasTechnologies.addAll(technologies);
    }

    /**
     * @param technology technology to be added to the set of technologies that the device support.
     */
    public void addAliasTechnology(Technology technology)
    {
        aliasTechnologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeAliasTechnology(Technology technology)
    {
        aliasTechnologies.remove(technology);
    }

    /**
     * @return {@link #value}
     */
    @Column
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return {@link #aliasProviderCapability}
     */
    @OneToOne
    public AliasProviderCapability getAliasProviderCapability()
    {
        return aliasProviderCapability;
    }

    /**
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public void setAliasProviderCapability(AliasProviderCapability aliasProviderCapability)
    {
        this.aliasProviderCapability = aliasProviderCapability;
    }

    @Override
    public void updateTechnologies()
    {
        clearTechnologies();
        addTechnologies(aliasTechnologies);
        for (AliasType aliasType : aliasTypes) {
            addTechnology(aliasType.getTechnology());
        }
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        AliasSpecification aliasSpecification = (AliasSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getAliasTechnologies(), aliasSpecification.getAliasTechnologies())
                || !ObjectUtils.equals(getAliasTypes(), aliasSpecification.getAliasTypes())
                || !ObjectUtils.equals(getValue(), aliasSpecification.getValue())
                || !ObjectUtils.equals(getAliasProviderCapability(), aliasSpecification.getAliasProviderCapability());

        setAliasTechnologies(aliasSpecification.getAliasTechnologies());
        setAliasTypes(aliasSpecification.getAliasTypes());
        setValue(aliasSpecification.getValue());
        setAliasProviderCapability(aliasSpecification.getAliasProviderCapability());

        return modified;
    }

    @Override
    public AliasSpecification clone()
    {
        AliasSpecification aliasSpecification = new AliasSpecification();
        aliasSpecification.setAliasTechnologies(getAliasTechnologies());
        aliasSpecification.setAliasTypes(getAliasTypes());
        aliasSpecification.setValue(getValue());
        aliasSpecification.setAliasProviderCapability(getAliasProviderCapability());
        aliasSpecification.updateTechnologies();
        return aliasSpecification;
    }

    @Override
    public AliasReservationTask createReservationTask(ReservationTask.Context context)
    {
        AliasReservationTask aliasReservationTask = new AliasReservationTask(context);
        for (Technology technology : getAliasTechnologies()) {
            aliasReservationTask.addTechnology(technology);
        }
        for (AliasType aliasType : getAliasTypes()) {
            aliasReservationTask.addAliasType(aliasType);
        }
        aliasReservationTask.setValue(value);
        if (aliasProviderCapability != null) {
            aliasReservationTask.addAliasProviderCapability(aliasProviderCapability);
        }
        return aliasReservationTask;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.AliasSpecification();
    }

    @Override
    public cz.cesnet.shongo.controller.api.AliasSpecification toApi()
    {
        return (cz.cesnet.shongo.controller.api.AliasSpecification) super.toApi();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSpecification) specificationApi;
        for (Technology technology : getAliasTechnologies()) {
            aliasSpecificationApi.addTechnology(technology);
        }
        for (AliasType aliasType : getAliasTypes()) {
            aliasSpecificationApi.addAliasType(aliasType);
        }
        aliasSpecificationApi.setValue(getValue());
        if (getAliasProviderCapability() != null) {
            aliasSpecificationApi.setResourceId(
                    Domain.getLocalDomain().formatId(getAliasProviderCapability().getResource()));
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi =
                (cz.cesnet.shongo.controller.api.AliasSpecification) specificationApi;
        if (aliasSpecificationApi.isPropertyFilled(aliasSpecificationApi.VALUE)) {
            setValue(aliasSpecificationApi.getValue());
        }
        if (aliasSpecificationApi.isPropertyFilled(aliasSpecificationApi.RESOURCE_ID)) {
            if (aliasSpecificationApi.getResourceId() == null) {
                setAliasProviderCapability(null);
            }
            else {
                Long resourceId = Domain.getLocalDomain().parseId(aliasSpecificationApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                Resource resource = resourceManager.get(resourceId);
                AliasProviderCapability aliasProviderCapability = resource.getCapability(AliasProviderCapability.class);
                if (aliasProviderCapability == null) {
                    throw new FaultException("Resource '%s' doesn't have %s.",
                            AliasProviderCapability.class.getSimpleName(), aliasSpecificationApi.getResourceId());
                }
                setAliasProviderCapability(aliasProviderCapability);
            }
        }

        // Create technologies
        for (Technology technology : aliasSpecificationApi.getTechnologies()) {
            if (aliasSpecificationApi.isPropertyItemMarkedAsNew(aliasSpecificationApi.TECHNOLOGIES, technology)) {
                addAliasTechnology(technology);
            }
        }
        // Delete technologies
        Set<Technology> technologiesToDelete =
                aliasSpecificationApi.getPropertyItemsMarkedAsDeleted(aliasSpecificationApi.TECHNOLOGIES);
        for (Technology technology : technologiesToDelete) {
            removeAliasTechnology(technology);
        }

        // Create alias types
        for (AliasType aliasType : aliasSpecificationApi.getAliasTypes()) {
            if (aliasSpecificationApi.isPropertyItemMarkedAsNew(aliasSpecificationApi.ALIAS_TYPES, aliasType)) {
                addAliasType(aliasType);
            }
        }
        // Delete alias types
        Set<AliasType> aliasTypesToDelete =
                aliasSpecificationApi.getPropertyItemsMarkedAsDeleted(aliasSpecificationApi.ALIAS_TYPES);
        for (AliasType aliasType : aliasTypesToDelete) {
            removeAliasType(aliasType);
        }

        super.fromApi(specificationApi, entityManager);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("aliasTypes", aliasTypes);
    }
}
