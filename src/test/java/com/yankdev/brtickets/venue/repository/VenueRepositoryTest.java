package com.yankdev.brtickets.venue.repository;

import com.yankdev.brtickets.TestContainersConfiguration;
import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.model.enums.VenueEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestContainersConfiguration.class)
class VenueRepositoryTest {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.persistAndFlush(venue("Allianz Parque", "Sao Paulo", true));
        entityManager.persistAndFlush(venue("Ibirapuera", "Sao Paulo", true));
        entityManager.persistAndFlush(venue("Old Arena", "Sao Paulo", false));
        entityManager.persistAndFlush(venue("Maracana", "Rio de Janeiro", true));
    }

    private VenueModel venue(String name, String city, boolean active) {
        VenueModel venue = new VenueModel();
        venue.setName(name);
        venue.setDescription("Description of " + name);
        venue.setType(VenueEnum.ARENA);
        venue.setStreet("Some Street, 100");
        venue.setCity(city);
        venue.setState("SP");
        venue.setZipCode("05001-200");
        venue.setCountry("Brazil");
        venue.setCapacity(20000);
        venue.setActive(active);
        return venue;
    }

    @Test
    @DisplayName("returns only the active venues of the city")
    void findsActiveVenuesOfCity() {
        List<VenueModel> venues = venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("Sao Paulo");

        assertThat(venues)
                .hasSize(2)
                .extracting(VenueModel::getName)
                .containsExactlyInAnyOrder("Allianz Parque", "Ibirapuera");
    }

    @Test
    @DisplayName("ignores the case of the city name")
    void ignoresCityCase() {
        assertThat(venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("sAo PaUlO")).hasSize(2);
        assertThat(venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("SAO PAULO")).hasSize(2);
    }

    @Test
    @DisplayName("never returns an inactive venue")
    void excludesInactiveVenues() {
        List<VenueModel> venues = venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("Sao Paulo");

        assertThat(venues)
                .extracting(VenueModel::getName)
                .doesNotContain("Old Arena");
        assertThat(venues).allMatch(VenueModel::isActive);
    }

    @Test
    @DisplayName("does not mix venues of other cities")
    void doesNotMixCities() {
        List<VenueModel> venues = venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("Rio de Janeiro");

        assertThat(venues)
                .hasSize(1)
                .extracting(VenueModel::getName)
                .containsExactly("Maracana");
    }

    @Test
    @DisplayName("returns an empty list for a city with no venue")
    void returnsEmptyListForUnknownCity() {
        assertThat(venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("Curitiba")).isEmpty();
    }

    @Test
    @DisplayName("stores the venue type as a string")
    void persistsTypeAsString() {
        VenueModel stored = venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("Rio de Janeiro").get(0);

        assertThat(stored.getType()).isEqualTo(VenueEnum.ARENA);
        assertThat(entityManager.getEntityManager()
                .createNativeQuery("select type from venues where name = 'Maracana'")
                .getSingleResult())
                .isEqualTo("ARENA");
    }
}
