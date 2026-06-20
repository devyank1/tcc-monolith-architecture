package com.yankdev.brtickets.venue.repository;

import com.yankdev.brtickets.venue.model.VenueModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<VenueModel, UUID> {
    String findAllByCity(String city);
    List<VenueModel> findAllByCityAndIsActiveTrue(String city);
}
