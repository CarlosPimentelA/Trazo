package com.trucks_logistics.Trucks.Logistics.trucks;

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
@RequestMapping("/api/trucks")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
public class TruckController {

    @Autowired
    TruckService truckService;

    @GetMapping
    public ResponseEntity<List<TruckResponse>> getTrucks() {
        return ResponseEntity.ok(truckService.getTrucks());
    }

    @GetMapping("/available")
    public ResponseEntity<List<TruckResponse>> getAvailableTrucks() {
        return ResponseEntity.ok(truckService.getAvailableTrucks());
    }

    @GetMapping("/in-use")
    public ResponseEntity<List<TruckResponse>> getInUseTrucks() {
        return ResponseEntity.ok(truckService.getInUseTrucks());
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<TruckResponse>> getAssignedTrucks() {
        return ResponseEntity.ok(truckService.getAssignedTrucks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TruckResponse> getTruckById(@PathVariable Long id) {
        return ResponseEntity.ok(truckService.getTruckById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteTruckById(@PathVariable Long id) {
        truckService.deleteTruck(id);
        return ResponseEntity.ok("Camion eliminado exitosamente");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> addTruck(@Valid @RequestBody TruckRequest request) {
        truckService.addTruck(request);
        return ResponseEntity.ok("Camion agregado exitosamente");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateTruck(@Valid @RequestBody TruckUpdateRequest truckUpdateRequest,
            @PathVariable Long id) {
        truckService.updateTruck(id, truckUpdateRequest);
        return ResponseEntity.ok("Camion actualizado exitosamente");
    }

    @PatchMapping("/{id}/{status}")
    public ResponseEntity<String> updateTruckAvailability(@PathVariable Long id,
            @PathVariable TruckStatus status) {
        truckService.updateTruckAvailability(id, status);
        return ResponseEntity.ok("Estado actualizado exitosamente");
    }
}
