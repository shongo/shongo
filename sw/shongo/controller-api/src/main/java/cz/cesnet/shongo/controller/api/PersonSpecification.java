package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.oldapi.annotation.Required;

/**
 * {@link Specification} for {@link Person}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersonSpecification extends ParticipantSpecification
{
    /**
     * The requested person.
     */
    private Person person;

    /**
     * Constructor.
     */
    public PersonSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param name  sets the {@link OtherPerson#name} for the {@link #PERSON}
     * @param email sets the {@link OtherPerson#email} for the {@link #PERSON}
     */
    public PersonSpecification(String name, String email)
    {
        setPerson(new OtherPerson(name, email));
    }

    /**
     * @return {@link #PERSON}
     */
    public Person getPerson()
    {
        return person;
    }

    /**
     * @param person sets the {@link #PERSON}
     */
    public void setPerson(Person person)
    {
        this.person = person;
    }

    public static final String PERSON = "person";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PERSON, person);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        person = dataMap.getComplexTypeRequired(PERSON, Person.class);
    }
}
