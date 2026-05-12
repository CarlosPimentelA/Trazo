package com.trucks_logistics.Trucks.Logistics.catalogs;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trucks_logistics.Trucks.Logistics.catalogs.truck.TruckTypeDTO;
import com.trucks_logistics.Trucks.Logistics.catalogs.truck.TruckTypeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/catalogs")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogController {

    @Autowired
    TruckTypeService truckTypeService;

    @PostMapping("/truck-type")
    public ResponseEntity<TruckTypeDTO> addTruckType(@Valid @RequestBody TruckTypeDTO dto) {
        return ResponseEntity.ok(truckTypeService.addTruckType(dto));
    }

    @GetMapping("/truck-type")
    public ResponseEntity<List<TruckTypeDTO>> getTruckTypes() {
        return ResponseEntity.ok(truckTypeService.getTruckTypes());
    }
}
