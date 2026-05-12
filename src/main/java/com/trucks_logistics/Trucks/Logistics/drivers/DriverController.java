package com.trucks_logistics.Trucks.Logistics.drivers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    DriverService driverService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<DriverDTO>> getDrivers() {
        return ResponseEntity.ok(driverService.getDrivers());
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<DriverDTO>> getAvailableDrivers() {
        return ResponseEntity.ok(driverService.getAvailableDrivers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DriverDTO> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<String> deleteDriverById(@PathVariable Long id) {
        driverService.deleteDriverById(id);
        return ResponseEntity.ok("Conductor eliminado exitosamente");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> addDriver(@Valid @RequestBody DriverDTO driverDTO) {
        driverService.addDrivers(driverDTO);
        return ResponseEntity.ok("Conductor agregado exitosamente");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateDriver(@Valid @RequestBody DriverUpdateDTO driverUpdateDTO,
            @PathVariable Long id) {
        driverService.updateDriver(driverUpdateDTO, id);
        return ResponseEntity.ok("Conductor actualizado exitosamente");
    }

    @PatchMapping("/available/{id}/{disponibility}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<String> updateDriverAvailability(@PathVariable Long id,
            @PathVariable DriverDisponibility disponibility) {
        driverService.updateDriverAvailability(id, disponibility);
        return ResponseEntity.ok("Estado actualizado exitosamente");
    }

    // TODO: Crear un ENDPOINT protegido que permita actualizar el DNI.
}
