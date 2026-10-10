package com.yankdev.brtickets.venue.service;

import com.yankdev.brtickets.shared.exception.IllegalVenueCapacityException;
import com.yankdev.brtickets.shared.exception.IllegalVenueInactiveException;
import com.yankdev.brtickets.shared.exception.VenueNotFoundException;
import com.yankdev.brtickets.venue.dto.VenueRequestDTO;
import com.yankdev.brtickets.venue.dto.VenueResponseDTO;
import com.yankdev.brtickets.venue.model.VenueModel;
import com.yankdev.brtickets.venue.model.enums.VenueEnum;
import com.yankdev.brtickets.venue.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private VenueService venueService;

    @Captor
    private ArgumentCaptor<VenueModel> venueCaptor;

    private VenueRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new VenueRequestDTO();
        request.setName("Allianz Parque");
        request.setDescription("Arena in Sao Paulo");
        request.setType(VenueEnum.ARENA);
        request.setStreet("Av. Francisco Matarazzo, 1705");
        request.setCity("Sao Paulo");
        request.setState("SP");
        request.setZipCode("05001-200");
        request.setCountry("Brazil");
        request.setCapacity(43000);
    }

    private VenueModel existingVenue() {
        VenueModel venue = new VenueModel();
        venue.setVenueId(UUID.randomUUID());
        venue.setName("Old Arena");
        venue.setDescription("Old description");
        venue.setType(VenueEnum.STADIUM);
        venue.setStreet("Old Street, 100");
        venue.setCity("Rio de Janeiro");
        venue.setState("RJ");
        venue.setZipCode("20000-000");
        venue.setCountry("Brazil");
        venue.setCapacity(10000);
        venue.setActive(true);
        return venue;
    }

    @Nested
    @DisplayName("createVenue")
    class CreateVenue {

        @Test
        @DisplayName("creates an active venue with the requested data")
        void createsVenue() {
            when(venueRepository.save(any(VenueModel.class))).thenAnswer(call -> call.getArgument(0));

            VenueResponseDTO response = venueService.createVenue(request);

            assertThat(response.getName()).isEqualTo("Allianz Parque");
            assertThat(response.getCity()).isEqualTo("Sao Paulo");
            assertThat(response.getType()).isEqualTo(VenueEnum.ARENA);
            assertThat(response.getCapacity()).isEqualTo(43000);
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("accepts the minimum capacity of 1 seat")
        void acceptsMinimumCapacity() {
            request.setCapacity(1);
            when(venueRepository.save(any(VenueModel.class))).thenAnswer(call -> call.getArgument(0));

            assertThat(venueService.createVenue(request).getCapacity()).isEqualTo(1);
        }

        @Test
        @DisplayName("rejects capacity below 1 and never saves")
        void rejectsZeroCapacity() {
            request.setCapacity(0);

            assertThatThrownBy(() -> venueService.createVenue(request))
                    .isInstanceOf(IllegalVenueCapacityException.class)
                    .hasMessage("Venue needs to have at least 1 seat.");

            verify(venueRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects negative capacity and never saves")
        void rejectsNegativeCapacity() {
            request.setCapacity(-50);

            assertThatThrownBy(() -> venueService.createVenue(request))
                    .isInstanceOf(IllegalVenueCapacityException.class);

            verify(venueRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findVenueById / findAllActiveVenues")
    class Queries {

        @Test
        @DisplayName("returns the venue found by id")
        void findsVenueById() {
            VenueModel venue = existingVenue();
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));

            VenueResponseDTO response = venueService.findVenueById(venue.getVenueId());

            assertThat(response.getVenueId()).isEqualTo(venue.getVenueId());
            assertThat(response.getName()).isEqualTo("Old Arena");
        }

        @Test
        @DisplayName("fails when the venue id does not exist")
        void failsWhenVenueIdDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(venueRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.findVenueById(unknownId))
                    .isInstanceOf(VenueNotFoundException.class)
                    .hasMessage("Venue not found.");
        }

        @Test
        @DisplayName("returns the active venues of a city")
        void findsActiveVenuesByCity() {
            VenueModel first = existingVenue();
            VenueModel second = existingVenue();
            second.setName("Second Arena");
            when(venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("Rio de Janeiro"))
                    .thenReturn(List.of(first, second));

            List<VenueResponseDTO> response = venueService.findAllActiveVenues("Rio de Janeiro");

            assertThat(response).hasSize(2)
                    .extracting(VenueResponseDTO::getName)
                    .containsExactly("Old Arena", "Second Arena");
        }

        @Test
        @DisplayName("returns an empty list when the city has no active venue")
        void findsNoVenueInCity() {
            when(venueRepository.findAllByCityIgnoreCaseAndIsActiveTrue("Curitiba"))
                    .thenReturn(List.of());

            assertThat(venueService.findAllActiveVenues("Curitiba")).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("updateVenue")
    class UpdateVenue {

        @Test
        @DisplayName("updates only the fields sent in the request")
        void updatesOnlyNonNullFields() {
            VenueModel venue = existingVenue();
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));
            when(venueRepository.save(any(VenueModel.class))).thenAnswer(call -> call.getArgument(0));

            VenueRequestDTO partial = new VenueRequestDTO();
            partial.setName("New Arena Name");
            partial.setCapacity(25000);

            VenueResponseDTO response = venueService.updateVenue(venue.getVenueId(), partial);

            assertThat(response.getName()).isEqualTo("New Arena Name");
            assertThat(response.getCapacity()).isEqualTo(25000);
            assertThat(response.getCity()).isEqualTo("Rio de Janeiro");
            assertThat(response.getType()).isEqualTo(VenueEnum.STADIUM);
            assertThat(response.isActive()).isTrue();
        }

        @Test
        @DisplayName("updates every field when all of them are sent")
        void updatesAllFields() {
            VenueModel venue = existingVenue();
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));
            when(venueRepository.save(any(VenueModel.class))).thenAnswer(call -> call.getArgument(0));

            VenueResponseDTO response = venueService.updateVenue(venue.getVenueId(), request);

            assertThat(response.getName()).isEqualTo("Allianz Parque");
            assertThat(response.getDescription()).isEqualTo("Arena in Sao Paulo");
            assertThat(response.getType()).isEqualTo(VenueEnum.ARENA);
            assertThat(response.getStreet()).isEqualTo("Av. Francisco Matarazzo, 1705");
            assertThat(response.getCity()).isEqualTo("Sao Paulo");
            assertThat(response.getState()).isEqualTo("SP");
            assertThat(response.getZipCode()).isEqualTo("05001-200");
            assertThat(response.getCountry()).isEqualTo("Brazil");
            assertThat(response.getCapacity()).isEqualTo(43000);
        }

        @Test
        @DisplayName("keeps name and capacity when the request omits them")
        void keepsNameAndCapacityWhenOmitted() {
            VenueModel venue = existingVenue();
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));
            when(venueRepository.save(any(VenueModel.class))).thenAnswer(call -> call.getArgument(0));

            VenueRequestDTO partial = new VenueRequestDTO();
            partial.setDescription("Only the description changes");
            partial.setStreet("New Street, 500");

            VenueResponseDTO response = venueService.updateVenue(venue.getVenueId(), partial);

            assertThat(response.getDescription()).isEqualTo("Only the description changes");
            assertThat(response.getStreet()).isEqualTo("New Street, 500");
            assertThat(response.getName()).isEqualTo("Old Arena");
            assertThat(response.getCapacity()).isEqualTo(10000);
        }

        @Test
        @DisplayName("refuses to update an inactive venue and never saves")
        void refusesInactiveVenue() {
            VenueModel venue = existingVenue();
            venue.setActive(false);
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));

            assertThatThrownBy(() -> venueService.updateVenue(venue.getVenueId(), request))
                    .isInstanceOf(IllegalVenueInactiveException.class)
                    .hasMessage("You cannot update an UNACTIVE venue");

            verify(venueRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the venue does not exist")
        void failsWhenVenueDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(venueRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.updateVenue(unknownId, request))
                    .isInstanceOf(VenueNotFoundException.class);

            verify(venueRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivateVenue")
    class DeactivateVenue {

        @Test
        @DisplayName("soft deletes the venue by setting active to false")
        void deactivatesVenue() {
            VenueModel venue = existingVenue();
            when(venueRepository.findById(venue.getVenueId())).thenReturn(Optional.of(venue));

            venueService.deactivateVenue(venue.getVenueId());

            verify(venueRepository).save(venueCaptor.capture());
            assertThat(venueCaptor.getValue().isActive()).isFalse();
        }

        @Test
        @DisplayName("fails when the venue does not exist")
        void failsWhenVenueDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(venueRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> venueService.deactivateVenue(unknownId))
                    .isInstanceOf(VenueNotFoundException.class);

            verify(venueRepository, never()).save(any());
        }
    }
}
