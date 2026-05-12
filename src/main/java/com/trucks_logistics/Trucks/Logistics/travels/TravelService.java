package com.trucks_logistics.Trucks.Logistics.travels;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.trucks_logistics.Trucks.Logistics.drivers.DriverRepository;
import com.trucks_logistics.Trucks.Logistics.exceptions.OverCapacityException;
import com.trucks_logistics.Trucks.Logistics.loads.Load;
import com.trucks_logistics.Trucks.Logistics.drivers.Driver;
import com.trucks_logistics.Trucks.Logistics.routes.RouteRepository;
import com.trucks_logistics.Trucks.Logistics.trucks.Truck;
import com.trucks_logistics.Trucks.Logistics.trucks.TruckRepository;
import com.trucks_logistics.Trucks.Logistics.routes.Route;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class TravelService implements ITravelService {

        private final TravelRepository travelRepository;
        private final TruckRepository truckRepository;
        private final DriverRepository driverRepository;
        private final RouteRepository routeRepository;

        public TravelService(TravelRepository travelRepository, TruckRepository truckRepository,
                        DriverRepository driverRepository, RouteRepository routeRepository) {
                this.travelRepository = travelRepository;
                this.truckRepository = truckRepository;
                this.driverRepository = driverRepository;
                this.routeRepository = routeRepository;
        }

        // Empezamos mvp
        @Override
        public List<TravelResponse> findAll() {
                List<Travel> travels = travelRepository.findAll();
                return TravelMapper.toDTOList(travels);
        }

        @Override
        public TravelResponse findById(Long id) {
                TravelDataBundle travelData = getEntities(id);

                return TravelMapper.toDTO(
                                travelData.travel(),
                                travelData.loads(),
                                travelData.driver(),
                                travelData.truck(),
                                travelData.route());
        }

        @Override
        public TravelResponse create(TravelRequest request) {
                Driver driver = driverRepository.findById(request.getDriverId())
                                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

                Truck truck = truckRepository.findById(request.getTruckId())
                                .orElseThrow(() -> new RuntimeException("Camion no encontrado"));

                Route route = routeRepository.findById(request.getRouteId())
                                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));

                Travel travel = TravelMapper.toEntity(request, driver, truck, route);

                // MVP rule: estado controlado por backend
                travel.setTravelStatus(TravelStatus.PENDIENTE);

                // no se utiliza de momento
                validateCapacity(travel);

                Travel saved = travelRepository.save(travel);

                TravelDataBundle travelData = getEntities(saved.getId());

                return TravelMapper.toDTO(
                                travelData.travel(),
                                travelData.loads(),
                                travelData.driver(),
                                travelData.truck(),
                                travelData.route());
        }

        // TODO: Crear metodo para agregar cargas a un viaje, con validacion de
        // capacidad

        @Override
        public TravelResponse update(Long id, TravelUpdateRequest request) {
                Travel travel = travelRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

                Driver driver = driverRepository.findById(request.getDriverId())
                                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

                Truck truck = truckRepository.findById(request.getTruckId())
                                .orElseThrow(() -> new RuntimeException("Camion no encontrado"));

                Route route = routeRepository.findById(request.getRouteId())
                                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));

                travel.setDriver(driver);
                travel.setTruck(truck);
                travel.setRoute(route);
                travel.setArriveDate(request.getArrivalDate());

                validateCapacity(travel);

                Travel updated = travelRepository.save(travel);

                TravelDataBundle travelData = getEntities(updated.getId());

                return TravelMapper.toDTO(
                                travelData.travel(),
                                travelData.loads(),
                                travelData.driver(),
                                travelData.truck(),
                                travelData.route());
        }

        @Override
        public void delete(Long id) {
                Travel travel = travelRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));
                travelRepository.delete(travel);
        }

        @Override
        public List<TravelResponse> findByDriver(Long driverId) {
                Driver driver = driverRepository.findById(driverId)
                                .orElseThrow(() -> new RuntimeException("Conductor no encontrado"));

                List<Travel> travels = travelRepository.findByDriver(driver);

                return TravelMapper.toDTOList(travels);
        }

        @Override
        public List<TravelResponse> findByTruck(Long truckId) {
                Truck truck = truckRepository.findById(truckId)
                                .orElseThrow(() -> new RuntimeException("Camión no encontrado"));

                List<Travel> travels = travelRepository.findByTruck(truck);

                return TravelMapper.toDTOList(travels);
        }

        @Override
        public List<TravelResponse> findActiveTravels() {
                List<Travel> activeTravels = travelRepository.findAll().stream()
                                .filter(t -> t.getTravelStatus() == TravelStatus.EN_RUTA).toList();
                return TravelMapper.toDTOList(activeTravels);
        }

        private void updateStatus(Long id, TravelStatus status) {
                Travel travel = travelRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));
                travel.setTravelStatus(status);
        }

        @Override
        @Transactional
        public void startTravelStatus(Long id) {
                updateStatus(id, TravelStatus.EN_RUTA);
        }

        @Override
        @Transactional
        public void pendingTravelStatus(Long id) {
                updateStatus(id, TravelStatus.PENDIENTE);
        }

        @Override
        @Transactional
        public void completeTravelStatus(Long id) {
                updateStatus(id, TravelStatus.COMPLETADO);
        }

        @Override
        @Transactional
        public void cancelTravelStatus(Long id) {
                updateStatus(id, TravelStatus.CANCELADO);
        }

        @Override
        @Transactional
        public void loadingTravelStatus(Long id) {
                updateStatus(id, TravelStatus.CARGANDO);
        }

        @Override
        @Transactional
        public void unloadingTravelStatus(Long id) {
                updateStatus(id, TravelStatus.DESCARGANDO);
        }

        @Override
        @Transactional
        public void stopTravelStatus(Long id) {
                updateStatus(id, TravelStatus.DETENIDO);
        }

        @Override
        public List<TravelResponse> findByDateRange(LocalDateTime start, LocalDateTime end) {
                List<Travel> travels = travelRepository.findByDepartureDateBetween(start, end);
                return TravelMapper.toDTOList(travels);
        }

        private double sumTravelLoadWeight(List<Load> loads) {
                return loads.stream()
                                .collect(Collectors.summingDouble(Load::getLoadWeight));
        }

        private void validateCapacity(Travel travel) {
                int maxCapacity = travel.getTruck().getTruckType().getCapacityMax();

                double currentWeight = sumTravelLoadWeight(travel.getLoads());

                if (currentWeight > maxCapacity) {
                        throw new OverCapacityException("Truck capacity (" + maxCapacity + ") "
                                        + "excedeed, current weight: " + currentWeight);
                }
        }

        @Override
        public Double getCurrentTotalWeight(Long id) {
                TravelDataBundle travelData = getEntities(id);
                double loadWeight = sumTravelLoadWeight(travelData.loads());
                return loadWeight;
        }

        @Override
        public TravelResponse refreshEstimatedCost(Long id) {
                TravelDataBundle travelData = getEntities(id);
                TravelStatus travelStatus = travelData.travel().getTravelStatus();
                if (travelStatus == TravelStatus.COMPLETADO || travelStatus == TravelStatus.EN_RUTA
                                || travelStatus == TravelStatus.DESCARGANDO) {
                        throw new IllegalStateException(
                                        "No se puede refrescar el costo de un viaje finalizado o facturado");
                }
                double fuelPrice = travelData.travel().getCurrentFuelPrice().doubleValue();
                double usedFuel = travelData.travel().getEstimatedUsedFuel();
                BigDecimal total = BigDecimal.valueOf(fuelPrice * usedFuel);
                travelData.travel().setEstimatedTotalCost(total);
                return TravelMapper.toDTO(travelData.travel(), travelData.loads(), travelData.driver(),
                                travelData.truck(), travelData.route());
        }

        private TravelDataBundle getEntities(Long id) {
                Travel travel = travelRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Viaje no encontrado"));

                return new TravelDataBundle(
                                travel,
                                travel.getLoads(),
                                travel.getDriver(),
                                travel.getTruck(),
                                travel.getRoute());
        }

}
