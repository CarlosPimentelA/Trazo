package com.trucks_logistics.Trucks.Logistics.loads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trucks_logistics.Trucks.Logistics.travels.Travel;
import com.trucks_logistics.Trucks.Logistics.travels.TravelRepository;
import com.trucks_logistics.Trucks.Logistics.travels.TravelStatus;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class LoadServiceTest {

    @Mock
    private LoadRepository loadRepository;
    @Mock
    private TravelRepository travelRepository;

    @InjectMocks
    private LoadService loadService;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Travel buildTravel() {
        Travel travel = new Travel();
        travel.setId(1L);
        travel.setDepartureDate(LocalDateTime.now().plusDays(1));
        travel.setTravelStatus(TravelStatus.PENDIENTE);
        travel.setEstimatedUsedFuel(50.0);
        travel.setCurrentFuelPrice(BigDecimal.valueOf(200.0));
        travel.setEstimatedTotalCost(BigDecimal.valueOf(10000.0));
        travel.setLoads(new ArrayList<>());
        return travel;
    }

    private Load buildLoad(Travel travel) {
        Load load = new Load();
        load.setId(1L);
        load.setTravel(travel);
        load.setLoadType(LoadTypes.GENERAL);
        load.setLoadWeight(100.0);
        load.setLoadDescription("Carga de prueba");
        return load;
    }

    private LoadRequest buildRequest() {
        LoadRequest request = new LoadRequest();
        request.setTravelId(1L);
        request.setLoadType(LoadTypes.GENERAL);
        request.setLoadWeight(100.0);
        request.setLoadDescription("Carga de prueba");
        return request;
    }

    // ─── createLoad ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createLoad")
    class CreateLoad {

        @Test
        @DisplayName("Debe crear una carga y asociarla al viaje exitosamente")
        void shouldCreateLoadSuccessfully() {
            Travel travel = buildTravel();
            LoadRequest request = buildRequest();
            Load load = buildLoad(travel);

            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));
            when(loadRepository.save(any(Load.class))).thenReturn(load);

            LoadResponse result = loadService.createLoad(request);

            assertThat(result).isNotNull();
            assertThat(travel.getLoads()).hasSize(1);
            verify(loadRepository).save(any(Load.class));
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el viaje no existe")
        void shouldThrowWhenTravelNotFound() {
            LoadRequest request = buildRequest();
            when(travelRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loadService.createLoad(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("No se puede crear la carga: El viaje con ID: 1 no existe.");
        }
    }

    // ─── getAllLoads ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllLoads")
    class GetAllLoads {

        @Test
        @DisplayName("Debe retornar lista de cargas")
        void shouldReturnAllLoads() {
            Travel travel = buildTravel();
            when(loadRepository.findAll()).thenReturn(List.of(buildLoad(travel)));

            List<LoadResponse> result = loadService.getAllLoads();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar lista vacia si no hay cargas")
        void shouldReturnEmptyList() {
            when(loadRepository.findAll()).thenReturn(List.of());

            List<LoadResponse> result = loadService.getAllLoads();

            assertThat(result).isEmpty();
        }
    }

    // ─── getLoadById ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLoadById")
    class GetLoadById {

        @Test
        @DisplayName("Debe retornar carga por id")
        void shouldReturnLoadById() {
            Travel travel = buildTravel();
            when(loadRepository.findById(1L)).thenReturn(Optional.of(buildLoad(travel)));

            LoadResponse result = loadService.getLoadById(1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si la carga no existe")
        void shouldThrowWhenLoadNotFound() {
            when(loadRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loadService.getLoadById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("La carga con ID: 99 no existe.");
        }
    }

    // ─── updateLoad ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateLoad")
    class UpdateLoad {

        @Test
        @DisplayName("Debe actualizar el tipo de carga exitosamente")
        void shouldUpdateLoadTypeSuccessfully() {
            Travel travel = buildTravel();
            Load load = buildLoad(travel);
            LoadUpdateRequest request = new LoadUpdateRequest();
            request.setLoadType(LoadTypes.PERECEDERO_FRIO);

            when(loadRepository.findById(1L)).thenReturn(Optional.of(load));
            when(loadRepository.save(any(Load.class))).thenReturn(load);

            LoadResponse result = loadService.updateLoad(1L, request);

            assertThat(result).isNotNull();
            assertThat(load.getLoadType()).isEqualTo(LoadTypes.PERECEDERO_FRIO);
        }

        @Test
        @DisplayName("Debe actualizar el peso exitosamente")
        void shouldUpdateLoadWeightSuccessfully() {
            Travel travel = buildTravel();
            Load load = buildLoad(travel);
            LoadUpdateRequest request = new LoadUpdateRequest();
            request.setLoadWeight(250.0);

            when(loadRepository.findById(1L)).thenReturn(Optional.of(load));
            when(loadRepository.save(any(Load.class))).thenReturn(load);

            loadService.updateLoad(1L, request);

            assertThat(load.getLoadWeight()).isEqualTo(250.0);
        }

        @Test
        @DisplayName("Debe actualizar la descripcion exitosamente")
        void shouldUpdateLoadDescriptionSuccessfully() {
            Travel travel = buildTravel();
            Load load = buildLoad(travel);
            LoadUpdateRequest request = new LoadUpdateRequest();
            request.setLoadDescription("Nueva descripcion");

            when(loadRepository.findById(1L)).thenReturn(Optional.of(load));
            when(loadRepository.save(any(Load.class))).thenReturn(load);

            loadService.updateLoad(1L, request);

            assertThat(load.getLoadDescription()).isEqualTo("Nueva descripcion");
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si la carga no existe")
        void shouldThrowWhenLoadNotFound() {
            when(loadRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loadService.updateLoad(99L, new LoadUpdateRequest()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("La carga con ID: 99 no existe.");
        }
    }

    // ─── deleteLoad ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteLoad")
    class DeleteLoad {

        @Test
        @DisplayName("Debe eliminar una carga y desasociarla del viaje")
        void shouldDeleteLoadAndRemoveFromTravel() {
            Travel travel = buildTravel();
            Load load = buildLoad(travel);
            travel.addLoad(load);

            when(loadRepository.findById(1L)).thenReturn(Optional.of(load));

            loadService.deleteLoad(1L);

            assertThat(travel.getLoads()).isEmpty();
            verify(loadRepository).delete(load);
        }

        @Test
        @DisplayName("Debe eliminar una carga sin viaje asociado")
        void shouldDeleteLoadWithoutTravel() {
            Load load = buildLoad(null);
            load.setTravel(null);

            when(loadRepository.findById(1L)).thenReturn(Optional.of(load));

            loadService.deleteLoad(1L);

            verify(loadRepository).delete(load);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si la carga no existe")
        void shouldThrowWhenLoadNotFound() {
            when(loadRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loadService.deleteLoad(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("La carga con ID: 99 no existe.");
        }
    }
}