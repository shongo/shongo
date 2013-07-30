package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.rpc.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.Allocation;
import cz.cesnet.shongo.jade.Container;
import cz.cesnet.shongo.util.Timer;
import org.apache.log4j.Level;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DatabasePerformanceTest
{
    private static Logger logger = LoggerFactory.getLogger(DatabasePerformanceTest.class);

    public EntityManagerFactory createEntityManagerFactory(String driver, String url, String username, String password)
            throws Exception
    {
        // Enable log4jdbc
        //driver = "net.sf.log4jdbc.DriverSpy";
        //url = url.replace("jdbc:", "jdbc:log4jdbc:");

        // For testing purposes use only in-memory database
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("hibernate.connection.driver_class", driver);
        properties.put("hibernate.connection.url", url);
        properties.put("hibernate.connection.username", username);
        properties.put("hibernate.connection.password", password);

        Timer timer = new Timer();
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("controller", properties);
        logger.info("Entity manager factory created in {} ms.", timer.stop());

        return entityManagerFactory;
    }

    @Test
    public void test() throws Exception
    {
        /*EntityManagerFactory entityManagerFactoryHsqldb = createEntityManagerFactory(
                "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test;", "sa", "");
        test(entityManagerFactoryHsqldb);
        entityManagerFactoryHsqldb.close();

        EntityManagerFactory entityManagerFactoryPostgres = createEntityManagerFactory(
                "org.postgresql.Driver", "jdbc:postgresql://127.0.0.1/test", "shongo", "shongo");
        test(entityManagerFactoryPostgres);

        EntityManager entityManager = entityManagerFactoryPostgres.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.createNativeQuery("DROP SCHEMA public CASCADE;").executeUpdate();
        entityManager.createNativeQuery("CREATE SCHEMA public;").executeUpdate();
        entityManager.getTransaction().commit();
        entityManager.close();

        entityManagerFactoryPostgres.close();*/
    }

    private void test(EntityManagerFactory entityManagerFactory) throws Exception
    {
        AbstractControllerTest.setupSystemProperties();
        Reporter.setThrowInternalErrorsForTesting(true);

        cz.cesnet.shongo.controller.Controller controller = new cz.cesnet.shongo.controller.Controller();
        controller.setDomain("cz.cesnet", "CESNET, z.s.p.o.");
        controller.setEntityManagerFactory(entityManagerFactory);

        Authorization authorization = DummyAuthorization.createInstance(controller.getConfiguration());
        controller.setAuthorization(authorization);

        Cache cache = new Cache();
        cache.setEntityManagerFactory(entityManagerFactory);
        cache.init(controller.getConfiguration());

        Preprocessor preprocessor = new Preprocessor();
        preprocessor.setCache(cache);
        preprocessor.setAuthorization(authorization);
        preprocessor.init();

        Scheduler scheduler = new Scheduler();
        scheduler.setCache(cache);
        scheduler.setAuthorization(authorization);
        scheduler.setNotificationManager(controller.getNotificationManager());
        scheduler.init();

        controller.addRpcService(new AuthorizationServiceImpl());
        controller.addRpcService(new ResourceServiceImpl(cache));
        controller.addRpcService(new ReservationServiceImpl());

        controller.start();
        controller.startRpc();

        ControllerClient controllerClient = new ControllerClient(controller.getRpcHost(), controller.getRpcPort());
        ReservationService reservationService = controllerClient.getService(ReservationService.class);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSlot("2013-01-01T12:00", "PT2H");
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));
        String reservationRequestId = reservationService.createReservationRequest(
                AbstractControllerTest.SECURITY_TOKEN, reservationRequest);

        for (int index = 0; index < 3; index++) {
            logger.debug("Attempt #{}", index + 1);

            reservationService.getReservationRequest(AbstractControllerTest.SECURITY_TOKEN, reservationRequestId);

            EntityManager entityManager = entityManagerFactory.createEntityManager();

            cz.cesnet.shongo.controller.request.ReservationRequest persistentReservationRequest =
                    entityManager.find(cz.cesnet.shongo.controller.request.ReservationRequest.class,
                            EntityIdentifier.parseId(cz.cesnet.shongo.controller.request.ReservationRequest.class,
                                    reservationRequestId));

            enableSqlLogger(true);
            Timer timer = new Timer();
            persistentReservationRequest.getAllocation().getReservations().size();
            timer.stopAndPrint();
            enableSqlLogger(false);

            entityManager.close();
        }

        controller.stop();
        controller.destroy();
        preprocessor.destroy();
        scheduler.destroy();
        Reporter.setThrowInternalErrorsForTesting(false);
    }

    private void enableSqlLogger(boolean enable)
    {
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("org.hibernate.SQL");
        logger.setLevel(enable ? Level.ALL : Level.OFF);
    }
}
