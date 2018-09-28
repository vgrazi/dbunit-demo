package com.vgrazi.demo.dbunit;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.sql.Types;
import java.util.List;

@RestController
@RequestMapping("/reservations")
public class ReservationsController
{
    private static final Logger logger = LoggerFactory.getLogger(ReservationsController.class);

    private final JdbcTemplate jdbcTemplate;

    @Inject
    public ReservationsController(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @RequestMapping("/seats")
    public List<String> findSeats()
    {
        return jdbcTemplate.queryForList("" +
                        " SELECT SEAT_NUM from SEAT " +
                        " WHERE SEAT_NUM not in " +
                        " (SELECT SEAT_NUM from RESERVATION)",
                String.class);
    }

    @RequestMapping("/passengers")
    public List<String> getPassengers()
    {
        return jdbcTemplate.queryForList("" +
                        "select PASSENGER_NAME from PASSENGER",
                String.class);
    }

    @RequestMapping("/reservation/{passengerName}/{seatType}")
    public String createReservation(@PathVariable("passengerName") String passengerName, @PathVariable("seatType") String seatType)
    {
        // find a seat
        String seat = findSeat(seatType);
        if (seat == null)
        {
            throw new IllegalArgumentException("No seats for " + seatType);
        }

        // if the passenger does not exist, add them
        String passenger = findPassenger(passengerName);
        if (passenger == null)
        {
            createPassenger(passengerName);
        }
        else        {
            String bookedSeat = findBooking(passengerName);
            if (bookedSeat != null)
            {
                throw new IllegalArgumentException("Already booked seat for " + passengerName);
            }
        }


        // create a reservation record
        bookReservation(passenger, seat);
        return "Booked " + seatType + " for " + passengerName;
    }


    @RequestMapping("/cancel/{passengerName}")
    public String cancelReservation(@PathVariable("passengerName") String passengerName)
    {
        jdbcTemplate.update("" +
                        "delete from RESERVATION where PASSENGER_ID in (select PASSENGER_ID from PASSENGER where PASSENGER_NAME=?)",
                passengerName);
        return "Canceled reservation for " + passengerName;


    }

    private String findBooking(String passengerName)
    {
        String seat = null;
        List<String> seats = jdbcTemplate.queryForList("" +
                "SELECT SEAT_NUM + ' ' + PASSENGER_NAME " +
                "FROM RESERVATION R, PASSENGER P " +
                "WHERE R.PASSENGER_ID = P.PASSENGER_ID " +
                "AND P.PASSENGER_NAME = ?", new String[]{passengerName}, String.class);
        if (!seats.isEmpty())
        {
            seat = seats.get(0);
        }
        return seat;
    }

    private void bookReservation(String passenger, String seat)
    {
        Integer reservationId = jdbcTemplate.queryForObject("select coalesce(max(RESERVATION_ID), 0) + 1 from RESERVATION", int.class);
        jdbcTemplate.update("" +
                "insert into RESERVATION(RESERVATION_ID, PASSENGER_ID, SEAT_NUM)\n" +
                " select ?, PASSENGER_ID, ? from PASSENGER where PASSENGER_NAME = ?", reservationId, seat, passenger);
    }

    @RequestMapping("/bookings")
    public List<String> bookings()
    {

        List<String> seats = jdbcTemplate.queryForList("" +
                "select SEAT_NUM + ' ' + CONVERT(varchar(32), PASSENGER_ID) from RESERVATION", String.class);
        return seats;
    }


    private void createPassenger(String passengerName)
    {
        Integer passengerId = jdbcTemplate.queryForObject("select coalesce(max(PASSENGER_ID), 0) + 1 from PASSENGER", int.class);
        jdbcTemplate.update("INSERT INTO PASSENGER (PASSENGER_ID, PASSENGER_NAME) VALUES (?,?)", passengerId, passengerName);
    }

    public String findSeat(String seatType)
    {
        String seat = null;
        List<String> seats = jdbcTemplate.queryForList("" +
                        "select top 1 SEAT_NUM from SEAT " +
                        "where SEAT_TYPE=? " +
                        "and SEAT_NUM not in " +
                        "(select SEAT_NUM from RESERVATION)", new String[]{seatType}, new int[]{Types.VARCHAR},
                String.class);
        if (!seats.isEmpty())
        {
            seat = seats.get(0);
        }
        return seat;
    }


    public String findPassenger(String passengerName)
    {
        String passenger = null;
        List<String> passengers = jdbcTemplate.queryForList("select PASSENGER_NAME from PASSENGER where PASSENGER_NAME =?",
                new String[]{passengerName}, new int[]{Types.VARCHAR},
                String.class);
        if (!passengers.isEmpty())
        {
            passenger = passengers.get(0);
        }
        return passenger;

    }

}
