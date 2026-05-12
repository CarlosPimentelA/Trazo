package com.trucks_logistics.Trucks.Logistics.driver;

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

import com.trucks_logistics.Trucks.Logistics.drivers.Driver;
import com.trucks_logistics.Trucks.Logistics.drivers.DriverDTO;
import com.trucks_logistics.Trucks.Logistics.drivers.DriverDisponibility;
import com.trucks_logistics.Trucks.Logistics.drivers.DriverRepository;
import com.trucks_logistics.Trucks.Logistics.drivers.DriverService;
import com.trucks_logistics.Trucks.Logistics.drivers.DriverUpdateDTO;
import com.trucks_logistics.Trucks.Logistics.drivers.LicenseType;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverService driverService;

    // ─── Helpers ────────────────────────────────────────────────────────────────

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

    private DriverDTO buildDriverDTO() {
        DriverDTO dto = new DriverDTO();
        dto.setFirstName("Juan");
        dto.setLastName("Perez");
        dto.setDni("00112345678");
        dto.setLicenseType(LicenseType.CATEGORIA_3);
        return dto;
    }

    // ─── getDrivers ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDrivers")
    class GetDrivers {

        @Test
        @DisplayName("Debe retornar lista de conductores")
        void shouldReturnAllDrivers() {
            when(driverRepository.findAll()).thenReturn(List.of(buildDriver()));

            List<DriverDTO> result = driverService.getDrivers();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar lista vacia si no hay conductores")
        void shouldReturnEmptyList() {
            when(driverRepository.findAll()).thenReturn(List.of());

            List<DriverDTO> result = driverService.getDrivers();

            assertThat(result).isEmpty();
        }
    }

    // ─── getDriverById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDriverById")
    class GetDriverById {

        @Test
        @DisplayName("Debe retornar conductor por id")
        void shouldReturnDriverById() {
            when(driverRepository.findById(1L)).thenReturn(Optional.of(buildDriver()));

            DriverDTO result = driverService.getDriverById(1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el conductor no existe")
        void shouldThrowWhenDriverNotFound() {
            when(driverRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> driverService.getDriverById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Conductor no encontrado");
        }
    }

    // ─── addDrivers ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addDrivers")
    class AddDrivers {

        @Test
        @DisplayName("Debe agregar un conductor con disponibilidad DISPONIBLE por defecto")
        void shouldAddDriverWithDefaultAvailability() {
            DriverDTO dto = buildDriverDTO();
            when(driverRepository.save(any(Driver.class))).thenAnswer(i -> i.getArgument(0));

            driverService.addDrivers(dto);

            verify(driverRepository).save(any(Driver.class));
        }
    }

    // ─── deleteDriverById ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteDriverById")
    class DeleteDriverById {

        @Test
        @DisplayName("Debe eliminar un conductor exitosamente")
        void shouldDeleteDriverSuccessfully() {
            when(driverRepository.existsById(1L)).thenReturn(true);

            driverService.deleteDriverById(1L);

            verify(driverRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el conductor no existe")
        void shouldThrowWhenDriverNotFound() {
            when(driverRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> driverService.deleteDriverById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("No se puede eliminar: Conductor no encontrado");
        }
    }

    // ─── updateDriver ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateDriver")
    class UpdateDriver {

        @Test
        @DisplayName("Debe actualizar el nombre exitosamente")
        void shouldUpdateFirstNameSuccessfully() {
            Driver driver = buildDriver();
            DriverUpdateDTO request = new DriverUpdateDTO();
            request.setFirstName("Carlos");

            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            DriverDTO result = driverService.updateDriver(request, 1L);

            assertThat(result).isNotNull();
            assertThat(driver.getFirstName()).isEqualTo("Carlos");
        }

        @Test
        @DisplayName("Debe actualizar el apellido exitosamente")
        void shouldUpdateLastNameSuccessfully() {
            Driver driver = buildDriver();
            DriverUpdateDTO request = new DriverUpdateDTO();
            request.setLastName("Garcia");

            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            driverService.updateDriver(request, 1L);

            assertThat(driver.getLastName()).isEqualTo("Garcia");
        }

        @Test
        @DisplayName("Debe actualizar el DNI exitosamente")
        void shouldUpdateDNISuccessfully() {
            Driver driver = buildDriver();
            DriverUpdateDTO request = new DriverUpdateDTO();
            request.setDni("00198765432");

            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(driverRepository.existsByDNI("00198765432")).thenReturn(false);
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            driverService.updateDriver(request, 1L);

            assertThat(driver.getDNI()).isEqualTo("00198765432");
        }

        @Test
        @DisplayName("Debe lanzar IllegalArgumentException si el DNI ya existe")
        void shouldThrowWhenDNIAlreadyExists() {
            Driver driver = buildDriver();
            DriverUpdateDTO request = new DriverUpdateDTO();
            request.setDni("00198765432");

            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(driverRepository.existsByDNI("00198765432")).thenReturn(true);

            assertThatThrownBy(() -> driverService.updateDriver(request, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El DNI ya esta registrado con otro conductor");
        }

        @Test
        @DisplayName("Debe actualizar el tipo de licencia exitosamente")
        void shouldUpdateLicenseTypeSuccessfully() {
            Driver driver = buildDriver();
            DriverUpdateDTO request = new DriverUpdateDTO();
            request.setLicenseType(LicenseType.CATEGORIA_4);

            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
            when(driverRepository.save(any(Driver.class))).thenReturn(driver);

            driverService.updateDriver(request, 1L);

            assertThat(driver.getLicenseType()).isEqualTo(LicenseType.CATEGORIA_4);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el conductor no existe")
        void shouldThrowWhenDriverNotFound() {
            when(driverRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> driverService.updateDriver(new DriverUpdateDTO(), 99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Conductor no encontrado");
        }
    }

    // ─── getAvailableDrivers ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAvailableDrivers")
    class GetAvailableDrivers {

        @Test
        @DisplayName("Debe retornar solo conductores disponibles")
        void shouldReturnOnlyAvailableDrivers() {
            when(driverRepository.findByDriverDisponibility(DriverDisponibility.DISPONIBLE))
                    .thenReturn(List.of(buildDriver()));

            List<DriverDTO> result = driverService.getAvailableDrivers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDriverDisponibility())
                    .isEqualTo(DriverDisponibility.DISPONIBLE);
        }
    }

    // ─── updateDriverAvailability ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateDriverAvailability")
    class UpdateDriverAvailability {

        @Test
        @DisplayName("Debe actualizar la disponibilidad del conductor exitosamente")
        void shouldUpdateAvailabilitySuccessfully() {
            Driver driver = buildDriver();
            when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));

            driverService.updateDriverAvailability(1L, DriverDisponibility.OCUPADO);

            assertThat(driver.getDriverDisponibility())
                    .isEqualTo(DriverDisponibility.OCUPADO);
            verify(driverRepository).save(driver);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si el conductor no existe")
        void shouldThrowWhenDriverNotFound() {
            when(driverRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> driverService.updateDriverAvailability(99L, DriverDisponibility.DISPONIBLE))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Conductor no encontrado");
        }
    }
}