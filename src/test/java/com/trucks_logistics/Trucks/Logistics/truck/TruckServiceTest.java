package com.trucks_logistics.Trucks.Logistics.truck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.trucks_logistics.Trucks.Logistics.catalogs.truck.TruckTypeRepository;
import com.trucks_logistics.Trucks.Logistics.catalogs.truck.TruckTypes;
import com.trucks_logistics.Trucks.Logistics.trucks.Truck;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckRepository;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckRequest;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckResponse;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckService;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckStatus;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckUpdateRequest;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TruckServiceTest {

    @Mock
    private TruckRepository truckRepository;
    @Mock
    private TruckTypeRepository truckTypeRepository;

    @InjectMocks
    private TruckService truckService;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private TruckType buildTruckType() {
        TruckType truckType = new TruckType();
        truckType.setId(1L);
        truckType.setCategory(TruckTypes.PLATAFORMA);
        truckType.setDescription("Camion de plataforma");
        truckType.setCapacityMax(1000);
        truckType.setEstimatedConsumption(10.5f);
        return truckType;
    }

    private Truck buildTruck() {
        Truck truck = new Truck();
        truck.setId(1L);
        truck.setLicensePlate("ABC123");
        truck.setTruckStatus(TruckStatus.LIBRE);
        truck.setTruckType(buildTruckType());
        return truck;
    }

    private TruckRequest buildRequest() {
        TruckRequest request = new TruckRequest();
        request.setLicensePlate("ABC123");
        request.setTruckTypeId(1L);
        return request;
    }

    // ─── addTruck ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addTruck")
    class AddTruck {

        @Test
        @DisplayName("Debe agregar un camion exitosamente")
        void shouldAddTruckSuccessfully() {
            TruckRequest request = buildRequest();
            Truck truck = buildTruck();

            when(truckTypeRepository.findById(1L)).thenReturn(Optional.of(buildTruckType()));
            when(truckRepository.save(any(Truck.class))).thenReturn(truck);

            TruckResponse result = truckService.addTruck(request);

            assertThat(result).isNotNull();
            assertThat(result.getLicensePlate()).isEqualTo("ABC123");
            verify(truckRepository).save(any(Truck.class));
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el tipo de camion no existe")
        void shouldThrowWhenTruckTypeNotFound() {
            TruckRequest request = buildRequest();
            when(truckTypeRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> truckService.addTruck(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Tipo de camion inexistente");
        }
    }

    // ─── getTrucks ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTrucks")
    class GetTrucks {

        @Test
        @DisplayName("Debe retornar lista de camiones")
        void shouldReturnAllTrucks() {
            when(truckRepository.findAll()).thenReturn(List.of(buildTruck()));

            List<TruckResponse> result = truckService.getTrucks();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar lista vacia si no hay camiones")
        void shouldReturnEmptyList() {
            when(truckRepository.findAll()).thenReturn(List.of());

            List<TruckResponse> result = truckService.getTrucks();

            assertThat(result).isEmpty();
        }
    }

    // ─── getTruckById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTruckById")
    class GetTruckById {

        @Test
        @DisplayName("Debe retornar camion por id")
        void shouldReturnTruckById() {
            when(truckRepository.findById(1L)).thenReturn(Optional.of(buildTruck()));

            TruckResponse result = truckService.getTruckById(1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el camion no existe")
        void shouldThrowWhenTruckNotFound() {
            when(truckRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> truckService.getTruckById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Camion no encontrado");
        }
    }

    // ─── updateTruck ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTruck")
    class UpdateTruck {

        @Test
        @DisplayName("Debe actualizar la placa exitosamente")
        void shouldUpdateLicensePlateSuccessfully() {
            Truck truck = buildTruck();
            TruckUpdateRequest request = new TruckUpdateRequest();
            request.setLicensePlate("XYZ999");

            when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
            when(truckRepository.existsByLicensePlate("XYZ999")).thenReturn(false);
            when(truckRepository.save(any(Truck.class))).thenReturn(truck);

            TruckResponse result = truckService.updateTruck(1L, request);

            assertThat(result).isNotNull();
            assertThat(truck.getLicensePlate()).isEqualTo("XYZ999");
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException si la placa ya existe")
        void shouldThrowWhenLicensePlateAlreadyExists() {
            Truck truck = buildTruck();
            TruckUpdateRequest request = new TruckUpdateRequest();
            request.setLicensePlate("XYZ999");

            when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
            when(truckRepository.existsByLicensePlate("XYZ999")).thenReturn(true);

            assertThatThrownBy(() -> truckService.updateTruck(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La placa ya esta registrada en otro camion");
        }

        @Test
        @DisplayName("Debe actualizar el tipo de camion exitosamente")
        void shouldUpdateTruckTypeSuccessfully() {
            Truck truck = buildTruck();
            TruckType newTruckType = buildTruckType();
            newTruckType.setId(2L);

            TruckUpdateRequest request = new TruckUpdateRequest();
            request.setTruckTypeId(2L);

            when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));
            when(truckTypeRepository.findById(2L)).thenReturn(Optional.of(newTruckType));
            when(truckRepository.save(any(Truck.class))).thenReturn(truck);

            TruckResponse result = truckService.updateTruck(1L, request);

            assertThat(result).isNotNull();
            assertThat(truck.getTruckType().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el camion no existe")
        void shouldThrowWhenTruckNotFound() {
            when(truckRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> truckService.updateTruck(99L, new TruckUpdateRequest()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Camion no encontrado");
        }
    }

    // ─── deleteTruck ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTruck")
    class DeleteTruck {

        @Test
        @DisplayName("Debe eliminar un camion exitosamente")
        void shouldDeleteTruckSuccessfully() {
            Truck truck = buildTruck();
            when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));

            truckService.deleteTruck(1L);

            verify(truckRepository).delete(truck);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el camion no existe")
        void shouldThrowWhenTruckNotFound() {
            when(truckRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> truckService.deleteTruck(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("No se puede eliminar: Camion no encontrado");
        }
    }

    // ─── filtros por estado ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("filtros por estado")
    class FilterByStatus {

        @Test
        @DisplayName("Debe retornar camiones disponibles")
        void shouldReturnAvailableTrucks() {
            when(truckRepository.findByTruckStatus(TruckStatus.LIBRE))
                    .thenReturn(List.of(buildTruck()));

            List<TruckResponse> result = truckService.getAvailableTrucks();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar camiones en uso")
        void shouldReturnInUseTrucks() {
            Truck truck = buildTruck();
            truck.setTruckStatus(TruckStatus.EN_USO);
            when(truckRepository.findByTruckStatus(TruckStatus.EN_USO))
                    .thenReturn(List.of(truck));

            List<TruckResponse> result = truckService.getInUseTrucks();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar camiones asignados")
        void shouldReturnAssignedTrucks() {
            Truck truck = buildTruck();
            truck.setTruckStatus(TruckStatus.ASIGNADO);
            when(truckRepository.findByTruckStatus(TruckStatus.ASIGNADO))
                    .thenReturn(List.of(truck));

            List<TruckResponse> result = truckService.getAssignedTrucks();

            assertThat(result).hasSize(1);
        }
    }

    // ─── updateTruckAvailability ─────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTruckAvailability")
    class UpdateTruckAvailability {

        @Test
        @DisplayName("Debe actualizar el estado del camion exitosamente")
        void shouldUpdateTruckStatusSuccessfully() {
            Truck truck = buildTruck();
            when(truckRepository.findById(1L)).thenReturn(Optional.of(truck));

            truckService.updateTruckAvailability(1L, TruckStatus.EN_USO);

            assertThat(truck.getTruckStatus()).isEqualTo(TruckStatus.EN_USO);
            verify(truckRepository).save(truck);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el camion no existe")
        void shouldThrowWhenTruckNotFound() {
            when(truckRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> truckService.updateTruckAvailability(99L, TruckStatus.EN_USO))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Camion no encontrado");
        }
    }
}