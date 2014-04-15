package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.util.ObjectHelper;

import javax.persistence.*;

/**
 * Represents a {@link AbstractParticipant} defined by a {@link AbstractPerson}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PersonParticipant extends AbstractParticipant implements ObjectHelper.SameCheckable
{
    /**
     * @see AbstractPerson
     */
    private AbstractPerson person;

    /**
     * Each {@link AbstractParticipant} acts in a meeting in a {@link cz.cesnet.shongo.ParticipantRole}.
     */
    private ParticipantRole role;

    /**
     * @return {@link #person}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public AbstractPerson getPerson()
    {
        return person;
    }

    /**
     * @param person sets the {@link #person}
     */
    public void setPerson(AbstractPerson person)
    {
        this.person = person;
    }

    /**
     * @return {@link #role}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public ParticipantRole getRole()
    {
        return role;
    }

    /**
     * @param participantRole sets the {@link #role}
     */
    public void setRole(ParticipantRole participantRole)
    {
        this.role = participantRole;
    }

    /**
     * @return {@link AbstractPerson#getInformation()}
     */
    @Transient
    public PersonInformation getPersonInformation()
    {
        if (person == null) {
            return null;
        }
        return person.getInformation();
    }

    @Override
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        PersonParticipant personParticipant = (PersonParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);
        modified |= !ObjectHelper.isSamePersistent(getPerson(), personParticipant.getPerson());
        modified |= !ObjectHelper.isSame(getRole(), personParticipant.getRole());

        setPerson(personParticipant.getPerson().clone());
        setRole(personParticipant.getRole());

        return modified;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractParticipant createApi()
    {
        return new cz.cesnet.shongo.controller.api.PersonParticipant();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi)
    {
        cz.cesnet.shongo.controller.api.PersonParticipant personParticipantApi =
                (cz.cesnet.shongo.controller.api.PersonParticipant) participantApi;
        personParticipantApi.setPerson(getPerson().toApi());
        personParticipantApi.setRole(getRole());
        super.toApi(participantApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.PersonParticipant personParticipantApi =
                (cz.cesnet.shongo.controller.api.PersonParticipant) participantApi;

        cz.cesnet.shongo.controller.api.AbstractPerson personApi = personParticipantApi.getPerson();
        if (personApi == null) {
            throw new IllegalArgumentException("Person must not be null.");
        }
        else if (getPerson() != null && personApi.getId() != null && getPerson().getId().equals(personApi.notNullIdAsLong())) {
            getPerson().fromApi(personApi);
        }
        else {
            AbstractPerson person = AbstractPerson.createFromApi(personApi);
            setPerson(person);
        }
        setRole(personParticipantApi.getRole());

        super.fromApi(participantApi, entityManager);
    }

    @Override
    public boolean isSame(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        PersonParticipant that = (PersonParticipant) object;

        if (person != null ? !ObjectHelper.isSame(person, that.person) : that.person != null) {
            return false;
        }
        if (role != that.role) {
            return false;
        }

        return true;
    }
}