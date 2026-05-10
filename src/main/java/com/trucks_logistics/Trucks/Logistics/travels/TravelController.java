package com.trucks_logistics.Trucks.Logistics.travels;

import java.util.List;

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
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/travels")
@PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
@AllArgsConstructor
public class TravelController {
    private final TravelService travelService;

    @GetMapping
    public ResponseEntity<List<TravelResponse>> findAll() {
        return ResponseEntity.ok(travelService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TravelResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(travelService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TravelResponse> create(@Valid @RequestBody TravelRequest request) {
        return ResponseEntity.ok(travelService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TravelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TravelUpdateRequest request) {
        return ResponseEntity.ok(travelService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        travelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER', 'DISPATCHER')")
    public ResponseEntity<List<TravelResponse>> findByDriver(@PathVariable Long driverId) {
        return ResponseEntity.ok(travelService.findByDriver(driverId));
    }

    @GetMapping("/truck/{truckId}")
    public ResponseEntity<List<TravelResponse>> findByTruck(@PathVariable Long truckId) {
        return ResponseEntity.ok(travelService.findByTruck(truckId));
    }

    @PatchMapping("/status/{travelId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    public ResponseEntity<String> updateCompleteStatus(@PathVariable Long travelId) {
        travelService.completeTravelStatus(travelId);
        return ResponseEntity.ok("Actualizado perfectamente!");
    }

    @PatchMapping("/status/{travelId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    public ResponseEntity<String> updatePendingStatus(@PathVariable Long travelId) {
        travelService.pendingTravelStatus(travelId);
        return ResponseEntity.ok("Actualizado perfectamente");
    }

    @PatchMapping("/status/{travelId}/loading")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    public ResponseEntity<String> updateLoadingStatus(@PathVariable Long travelId) {
        travelService.loadingTravelStatus(travelId);
        return ResponseEntity.ok("Actualizado perfectamente");
    }

    @PatchMapping("/status/{travelId}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    public ResponseEntity<String> updateStartStatus(@PathVariable Long travelId) {
        travelService.startTravelStatus(travelId);
        return ResponseEntity.ok("Actualizado perfectamente");
    }

    @PatchMapping("/status/{travelId}/cancel")
    public ResponseEntity<String> updateCancelStatus(@PathVariable Long travelId) {
        travelService.cancelTravelStatus(travelId);
        return ResponseEntity.ok("Actualizado perfectamente");
    }

    @PatchMapping("/status/{travelId}/stop")
    public ResponseEntity<String> updateStopStatus(@PathVariable Long travelId) {
        travelService.stopTravelStatus(travelId);
        return ResponseEntity.ok("Actualizado perfectamente");
    }

    @PatchMapping("/status/{travelId}/unloading")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")

    public ResponseEntity<String> updateUnloadingStatus(@PathVariable Long travelId) {
        travelService.unloadingTravelStatus(travelId);
        return ResponseEntity.ok("Actualizado perfectamente");
    }

    @GetMapping("/active")
    public ResponseEntity<List<TravelResponse>> findActiveTravels() {
        return ResponseEntity.ok(travelService.findActiveTravels());
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<TravelResponse>> findTravelInDateRange(
            @Valid @RequestBody TravelDateRangeRequest request) {
        return ResponseEntity.ok(travelService.findByDateRange(request.starDateTime(), request.endDateTime()));
    }

    @GetMapping("/current-weight/{id}")
    public ResponseEntity<Double> getTravelCurrentWeight(@PathVariable Long id) {
        return ResponseEntity.ok(travelService.getCurrentTotalWeight(id));
    }

    @PatchMapping("/estimated-cost/{id}")
    public ResponseEntity<TravelResponse> updateTotalCost(@PathVariable Long id) {
        return ResponseEntity.ok(travelService.refreshEstimatedCost(id));
    }
}