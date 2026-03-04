package pt.ulusofona.cd.project.restaurant_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.ulusofona.cd.project.restaurant_service.entity.Restaurant;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @Query("SELECT r FROM Restaurant r WHERE r.active = true")
    List<Restaurant> findAllActive();

    @Query("SELECT r FROM Restaurant r WHERE r.id = :id AND r.active = true")
    Optional<Restaurant> findByIdAndActiveTrue(@Param("id") Long id);
}
