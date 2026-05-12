package com.trucks_logistics.Trucks.Logistics.travel;

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

import com.trucks_logistics.Trucks.Logistics.catalogs.truck.TruckType;
import com.trucks_logistics.Trucks.Logistics.catalogs.truck.TruckTypes;
import com.trucks_logistics.Trucks.Logistics.drivers.Driver;
import com.trucks_logistics.Trucks.Logistics.drivers.DriverDisponibility;
import com.trucks_logistics.Trucks.Logistics.drivers.DriverRepository;
import com.trucks_logistics.Trucks.Logistics.drivers.LicenseType;
import com.trucks_logistics.Trucks.Logistics.exceptions.OverCapacityException;
import com.trucks_logistics.Trucks.Logistics.loads.Load;
import com.trucks_logistics.Trucks.Logistics.loads.LoadTypes;
import com.trucks_logistics.Trucks.Logistics.routes.GeoLocation;
import com.trucks_logistics.Trucks.Logistics.routes.Route;
import com.trucks_logistics.Trucks.Logistics.routes.RouteRepository;
import com.trucks_logistics.Trucks.Logistics.travels.TravelStatus;
import com.trucks_logistics.Trucks.Logistics.travels.Travel;
import com.trucks_logistics.Trucks.Logistics.travels.TravelRepository;
import com.trucks_logistics.Trucks.Logistics.travels.TravelRequest;
import com.trucks_logistics.Trucks.Logistics.travels.TravelResponse;
import com.trucks_logistics.Trucks.Logistics.travels.TravelService;
import com.trucks_logistics.Trucks.Logistics.trucks.Truck;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckRepository;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckStatus;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    @Mock
    private TravelRepository travelRepository;
    @Mock
    private TruckRepository truckRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private TravelService travelService;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private TruckType buildTruckType(int capacityMax) {
        TruckType truckType = new TruckType();
        truckType.setId(1L);
        truckType.setCategory(TruckTypes.PLATAFORMA);
        truckType.setDescription("Camion de plataforma");
        truckType.setCapacityMax(capacityMax);
        truckType.setEstimatedConsumption(10.5f);
        return truckType;
    }

    private Truck buildTruck(int capacityMax) {
        Truck truck = new Truck();
        truck.setId(1L);
        truck.setLicensePlate("ABC123");
        truck.setTruckStatus(TruckStatus.LIBRE);
        truck.setTruckType(buildTruckType(capacityMax));
        return truck;
    }

    private Driver buildDriver() {
        Driver driver = new Driver();
        driver.setId(1L);
        driver.setFirstName("Juan");
        driver.setLastName("Perez");
        driver.setDNI("00112345678");
        driver.setLicenseType(LicenseType.CATEGORIA_3);
        driver.setDriverDisponibility(DriverDisponibility.DISPONIBLE);
        return driver;
    }

    private Route buildRoute() {
        Route route = new Route();
        route.setId(1L);
        route.setDeparturePoint(new GeoLocation("Santo Domingo", 18.4861, -69.9312));
        route.setDestination(new GeoLocation("Santiago", 19.4517, -70.6970));
        route.setDistanceKm(155.0);
        return route;
    }

    private Load buildLoad(Travel travel, double weight) {
        Load load = new Load();
        load.setId(1L);
        load.setTravel(travel);
        load.setLoadType(LoadTypes.GENERAL);
        load.setLoadWeight(weight);
        load.setLoadDescription("Carga de prueba");
        return load;
    }

    private Travel buildTravel(TravelStatus status, List<Load> loads) {
        Travel travel = new Travel();
        travel.setId(1L);
        travel.setDepartureDate(LocalDateTime.now().plusDays(1));
        travel.setTravelStatus(status);
        travel.setEstimatedUsedFuel(50.0);
        travel.setCurrentFuelPrice(BigDecimal.valueOf(200.0));
        travel.setEstimatedTotalCost(BigDecimal.valueOf(10000.0));
        travel.setDriver(buildDriver());
        travel.setTruck(buildTruck(1000));
        travel.setRoute(buildRoute());
        travel.setLoads(loads != null ? loads : new ArrayList<>());
        return travel;
    }

    private TravelRequest buildRequest() {
        TravelRequest request = new TravelRequest();
        request.setDepartureDate(LocalDateTime.now().plusDays(1));
        request.setEstimatedUsedFuel(50.0);
        request.setCurrentFuelPrice(BigDecimal.valueOf(200.0));
        request.setEstimatedTotalCost(BigDecimal.valueOf(10000.0));
        request.setTruckId(1L);
        request.setDriverId(1L);
        request.setRouteId(1L);
        return request;
    }

    // ─── findAll ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Debe retornar lista de viajes")
        void shouldReturnAllTravels() {
            Travel travel = buildTravel(TravelStatus.PENDIENTE, new ArrayList<>());
            when(travelRepository.findAll()).thenReturn(List.of(travel));

            List<TravelResponse> result = travelService.findAll();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar lista vacia si no hay viajes")
        void shouldReturnEmptyListWhenNoTravels() {
            when(travelRepository.findAll()).thenReturn(List.of());

            List<TravelResponse> result = travelService.findAll();

            assertThat(result).isEmpty();
        }
    }

    // ─── findById ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Debe retornar viaje por id")
        void shouldReturnTravelById() {
            Travel travel = buildTravel(TravelStatus.PENDIENTE, new ArrayList<>());
            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            TravelResponse result = travelService.findById(1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el viaje no existe")
        void shouldThrowEntityNotFoundWhenTravelNotFound() {
            when(travelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> travelService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Viaje no encontrado");
        }
    }

    // ─── create ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Debe crear un viaje exitosamente")
        void shouldCreateTravelSuccessfully() {
            TravelRequest request = buildRequest();
            Driver driver = buildDriver();
            Truck truck = buildTruck(1000);
            Route route = buildRoute();
            Load heavyLoad = new Load();
            heavyLoad.setLoadWeight(500.0); // 500 > 10

            List<Load> loads = List.of(heavyLoad);
            Travel saved = buildTravel(TravelStatus.PENDIENTE, loads);
            saved.setId(1L);

            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
            when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
            when(travelRepository.save(any(Travel.class))).thenReturn(saved);
            when(travelRepository.findById(1L)).thenReturn(Optional.of(saved));

            TravelResponse result = travelService.create(request);

            assertThat(result).isNotNull();
            verify(travelRepository).save(any(Travel.class));
        }

        @Test
        @DisplayName("Debe lanzar RuntimeException si el conductor no existe")
        void shouldThrowWhenDriverNotFound() {
            TravelRequest request = buildRequest();
            when(driverRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> travelService.create(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Conductor no encontrado");
        }

        @Test
        @DisplayName("Debe lanzar RuntimeException si el camion no existe")
        void shouldThrowWhenTruckNotFound() {
            TravelRequest request = buildRequest();
            when(driverRepository.findById(1L)).thenReturn(Optional.of(buildDriver()));
            when(truckRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> travelService.create(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Camion no encontrado");
        }

        @Test
        @DisplayName("Debe lanzar RuntimeException si la ruta no existe")
        void shouldThrowWhenRouteNotFound() {
            TravelRequest request = buildRequest();
            when(driverRepository.findById(1L)).thenReturn(Optional.of(buildDriver()));
            when(truckRepository.findById(1L)).thenReturn(Optional.of(buildTruck(1000)));
            when(routeRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> travelService.create(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Ruta no encontrada");
        }

        @Test
        @DisplayName("Debe lanzar OverCapacityException si el peso excede la capacidad")
        void shouldThrowOverCapacityWhenWeightExceedsCapacity() {
            TravelRequest request = buildRequest();
            Driver driver = buildDriver();
            Truck truck = buildTruck(10); // capacidad muy baja
            Route route = buildRoute();

            Load heavyLoad = new Load();
            heavyLoad.setLoadWeight(500.0); // 500 > 10

            List<Load> loads = List.of(heavyLoad);
            Travel saved = buildTravel(TravelStatus.PENDIENTE, loads);
            saved.setId(1L);

            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
            when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
            when(travelRepository.save(any(Travel.class))).thenReturn(saved);
            when(travelRepository.findById(1L)).thenReturn(Optional.of(saved));

            assertThatThrownBy(() -> travelService.create(request))
                    .isInstanceOf(OverCapacityException.class);
        }
    }

    // ─── delete ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Debe eliminar un viaje exitosamente")
        void shouldDeleteTravelSuccessfully() {
            Travel travel = buildTravel(TravelStatus.PENDIENTE, new ArrayList<>());
            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            travelService.delete(1L);

            verify(travelRepository).delete(travel);
        }

        @Test
        @DisplayName("Debe lanzar RuntimeException si el viaje no existe")
        void shouldThrowWhenTravelNotFound() {
            when(travelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> travelService.delete(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Viaje no encontrado");
        }
    }

    // ─── estados ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("estados del viaje")
    class TravelStatusTest {

        @Test
        @DisplayName("Debe cambiar estado a EN_RUTA")
        void shouldUpdateStatusToEnRuta() {
            Travel travel = buildTravel(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.PENDIENTE,
                    new ArrayList<>());
            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            travelService.startTravelStatus(1L);

            assertThat(travel.getTravelStatus())
                    .isEqualTo(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.EN_RUTA);
        }

        @Test
        @DisplayName("Debe cambiar estado a COMPLETADO")
        void shouldUpdateStatusToCompletado() {
            Travel travel = buildTravel(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.EN_RUTA,
                    new ArrayList<>());
            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            travelService.completeTravelStatus(1L);

            assertThat(travel.getTravelStatus())
                    .isEqualTo(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.COMPLETADO);
        }

        @Test
        @DisplayName("Debe cambiar estado a CANCELADO")
        void shouldUpdateStatusToCancelado() {
            Travel travel = buildTravel(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.PENDIENTE,
                    new ArrayList<>());
            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            travelService.cancelTravelStatus(1L);

            assertThat(travel.getTravelStatus())
                    .isEqualTo(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.CANCELADO);
        }

        @Test
        @DisplayName("Debe cambiar estado a CARGANDO")
        void shouldUpdateStatusToCargando() {
            Travel travel = buildTravel(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.PENDIENTE,
                    new ArrayList<>());
            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            travelService.loadingTravelStatus(1L);

            assertThat(travel.getTravelStatus())
                    .isEqualTo(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.CARGANDO);
        }

        @Test
        @DisplayName("Debe cambiar estado a DESCARGANDO")
        void shouldUpdateStatusToDescargando() {
            Travel travel = buildTravel(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.EN_RUTA,
                    new ArrayList<>());
            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            travelService.unloadingTravelStatus(1L);

            assertThat(travel.getTravelStatus())
                    .isEqualTo(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.DESCARGANDO);
        }

        @Test
        @DisplayName("Debe cambiar estado a DETENIDO")
        void shouldUpdateStatusToDetenido() {
            Travel travel = buildTravel(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.EN_RUTA,
                    new ArrayList<>());
            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            travelService.stopTravelStatus(1L);

            assertThat(travel.getTravelStatus())
                    .isEqualTo(com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.DETENIDO);
        }

        @Test
        @DisplayName("Debe lanzar RuntimeException si el viaje no existe al cambiar estado")
        void shouldThrowWhenTravelNotFoundOnStatusChange() {
            when(travelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> travelService.startTravelStatus(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Viaje no encontrado");
        }
    }

    // ─── findActiveTravels ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("findActiveTravels")
    class FindActiveTravels {

        @Test
        @DisplayName("Debe retornar solo viajes EN_RUTA")
        void shouldReturnOnlyActiveTravels() {
            Travel active = buildTravel(
                    com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.EN_RUTA,
                    new ArrayList<>());
            Travel pending = buildTravel(
                    com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.PENDIENTE,
                    new ArrayList<>());

            when(travelRepository.findAll()).thenReturn(List.of(active, pending));

            List<TravelResponse> result = travelService.findActiveTravels();

            assertThat(result).hasSize(1);
        }
    }

    // ─── getCurrentTotalWeight ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getCurrentTotalWeight")
    class GetCurrentTotalWeight {

        @Test
        @DisplayName("Debe retornar el peso total de las cargas")
        void shouldReturnTotalWeight() {
            Travel travel = buildTravel(
                    com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.PENDIENTE,
                    new ArrayList<>());
            Load load1 = buildLoad(travel, 100.0);
            Load load2 = buildLoad(travel, 200.0);
            travel.setLoads(List.of(load1, load2));

            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            Double result = travelService.getCurrentTotalWeight(1L);

            assertThat(result).isEqualTo(300.0);
        }

        @Test
        @DisplayName("Debe retornar 0 si no hay cargas")
        void shouldReturnZeroWhenNoLoads() {
            Travel travel = buildTravel(
                    com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.PENDIENTE,
                    new ArrayList<>());

            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            Double result = travelService.getCurrentTotalWeight(1L);

            assertThat(result).isEqualTo(0.0);
        }
    }

    // ─── refreshEstimatedCost ────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshEstimatedCost")
    class RefreshEstimatedCost {

        @Test
        @DisplayName("Debe calcular el costo estimado correctamente")
        void shouldRefreshEstimatedCostSuccessfully() {
            Travel travel = buildTravel(
                    com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.PENDIENTE,
                    new ArrayList<>());
            travel.setCurrentFuelPrice(BigDecimal.valueOf(200.0));
            travel.setEstimatedUsedFuel(50.0);

            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            TravelResponse result = travelService.refreshEstimatedCost(1L);

            assertThat(result).isNotNull();
            assertThat(travel.getEstimatedTotalCost())
                    .isEqualByComparingTo(BigDecimal.valueOf(10000.0));
        }

        @Test
        @DisplayName("Debe lanzar IllegalStateException si el viaje está EN_RUTA")
        void shouldThrowWhenTravelIsEnRuta() {
            Travel travel = buildTravel(
                    com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.EN_RUTA,
                    new ArrayList<>());

            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            assertThatThrownBy(() -> travelService.refreshEstimatedCost(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Debe lanzar IllegalStateException si el viaje está COMPLETADO")
        void shouldThrowWhenTravelIsCompletado() {
            Travel travel = buildTravel(
                    com.trucks_logistics.Trucks.Logistics.travels.TravelStatus.COMPLETADO,
                    new ArrayList<>());

            when(travelRepository.findById(1L)).thenReturn(Optional.of(travel));

            assertThatThrownBy(() -> travelService.refreshEstimatedCost(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}