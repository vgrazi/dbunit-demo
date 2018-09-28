package com.vgrazi.demo.dbdemo;

import org.dbunit.DBTestCase;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.mssql.InsertIdentityOperation;
import org.dbunit.operation.DatabaseOperation;
import static org.dbunit.Assertion.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;

import static org.dbunit.Assertion.assertEqualsIgnoreCols;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DbUnitIT extends DBTestCase
{
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger logger = LoggerFactory.getLogger(DbUnitIT.class);

    public DbUnitIT(String name)
    {
        super(name);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS, "net.sourceforge.jtds.jdbc.Driver");
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL, "xxx");
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME, "xxx");
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD, "xxx");
    }

    protected IDataSet getDataSet() throws Exception
    {
        FlatXmlDataSet seats = new FlatXmlDataSetBuilder().build(
                new FileInputStream("seats.xml"));
        FlatXmlDataSet passenger = new FlatXmlDataSetBuilder().build(
                new FileInputStream("passenger.xml"));
        FlatXmlDataSet reservations = new FlatXmlDataSetBuilder().build(
                new FileInputStream("reservations.xml"));
        CompositeDataSet compositeDataSet = new CompositeDataSet(
                new IDataSet[]{seats,passenger, reservations});
        return compositeDataSet;
    }

    protected DatabaseOperation getSetUpOperation() throws Exception
    {
        return DatabaseOperation.REFRESH;
    }

    protected DatabaseOperation getTearDownOperation() throws Exception
    {
        return DatabaseOperation.NONE;
    }

    @Test
    public void testReservationViaRestCall()
    {
        String result = restTemplate.getForObject("http://localhost:8082/reservations/reservation/Victor/First", String.class);
        assertThat(result, is("Booked First for Victor"));
    }

    @Test
    public void testReservationSql() throws Exception
    {
        // Make the REST call
        String result = restTemplate.getForObject(
                "http://localhost:8082/reservations/reservation/George/Business", String.class);

        // Grab the database state
        QueryDataSet dataSet = new QueryDataSet(getConnection());
//        dataSet.addTable("RESERVATION", "select * from RESERVATION");
        dataSet.addTable("RESERVATION");

        // Get the expected data
        FlatXmlDataSet reservations = new FlatXmlDataSetBuilder().build(
                new FileInputStream("reservations-verify.xml"));

        // Assert equal
        org.dbunit.Assertion.assertEquals(reservations, dataSet);
    }
}
