package pt.ulusofona.cd.project.restaurant_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.ulusofona.cd.project.restaurant_service.entity.AvailabilitySlot;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.restaurant.id = :restaurantId " +
           "AND a.date = :date " +
           "ORDER BY a.startTime")
    List<AvailabilitySlot> findByRestaurantAndDate(
            @Param("restaurantId") Long restaurantId,
            @Param("date") LocalDate date
    );

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.restaurant.id = :restaurantId " +
           "ORDER BY a.date, a.startTime")
    List<AvailabilitySlot> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Modifying
    @Query(value = "UPDATE availability_slots SET seats_available = seats_available - :count " +
                   "WHERE id = :id AND (seats_available - :count) >= 0",
           nativeQuery = true)
    int bookSeats(@Param("id") Long id, @Param("count") Integer count);

    @Modifying
    @Query(value = "UPDATE availability_slots SET seats_available = CASE " +
                   "WHEN seats_available + :count > capacity THEN capacity " +
                   "ELSE seats_available + :count END " +
                   "WHERE id = :id",
           nativeQuery = true)
    int releaseSeats(@Param("id") Long id, @Param("count") Integer count);
}
