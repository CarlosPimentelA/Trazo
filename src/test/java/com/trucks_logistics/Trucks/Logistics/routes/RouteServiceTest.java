package com.trucks_logistics.Trucks.Logistics.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
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

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteService routeService;

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private GeoLocation buildSantoDomingo() {
        return new GeoLocation("Santo Domingo", 18.4861, -69.9312);
    }

    private GeoLocation buildSantiago() {
        return new GeoLocation("Santiago", 19.4517, -70.6970);
    }

    private Route buildRoute() {
        Route route = new Route();
        route.setId(1L);
        route.setDeparturePoint(buildSantoDomingo());
        route.setDestination(buildSantiago());
        route.setDistanceKm(134.0);
        return route;
    }

    private RouteRequest buildRequest() {
        RouteRequest request = new RouteRequest();
        request.setDeparturePoint(buildSantoDomingo());
        request.setDestination(buildSantiago());
        return request;
    }

    // ─── addRoute ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addRoute")
    class AddRoute {

        @Test
        @DisplayName("Debe crear una ruta y calcular distancia con Haversine")
        void shouldAddRouteAndCalculateDistance() {
            RouteRequest request = buildRequest();
            Route saved = buildRoute();

            when(routeRepository.save(any(Route.class))).thenReturn(saved);

            RouteResponse result = routeService.addRoute(request);

            assertThat(result).isNotNull();
            // Distancia real entre Santo Domingo y Santiago ~134km
            assertThat(result.getDistanceKm()).isCloseTo(134.0, within(5.0));
            verify(routeRepository).save(any(Route.class));
        }

        @Test
        @DisplayName("Debe calcular distancia correctamente con Haversine")
        void shouldCalculateDistanceCorrectlyWithHaversine() {
            double distance = LocationUtils.locationDistance(
                    18.4861, -69.9312, // Santo Domingo
                    19.4517, -70.6970); // Santiago

            assertThat(distance).isCloseTo(134.0, within(5.0));
        }

        @Test
        @DisplayName("Debe calcular distancia cero para el mismo punto")
        void shouldReturnZeroForSamePoint() {
            double distance = LocationUtils.locationDistance(
                    18.4861, -69.9312,
                    18.4861, -69.9312);

            assertThat(distance).isCloseTo(0.0, within(0.001));
        }
    }

    // ─── getRoutes ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRoutes")
    class GetRoutes {

        @Test
        @DisplayName("Debe retornar lista de rutas")
        void shouldReturnAllRoutes() {
            when(routeRepository.findAll()).thenReturn(List.of(buildRoute()));

            List<RouteResponse> result = routeService.getRoutes();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Debe retornar lista vacia si no hay rutas")
        void shouldReturnEmptyList() {
            when(routeRepository.findAll()).thenReturn(List.of());

            List<RouteResponse> result = routeService.getRoutes();

            assertThat(result).isEmpty();
        }
    }

    // ─── getRouteById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRouteById")
    class GetRouteById {

        @Test
        @DisplayName("Debe retornar ruta por id")
        void shouldReturnRouteById() {
            when(routeRepository.findById(1L)).thenReturn(Optional.of(buildRoute()));

            RouteResponse result = routeService.getRouteById(1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si la ruta no existe")
        void shouldThrowWhenRouteNotFound() {
            when(routeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> routeService.getRouteById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Ruta no encontrada");
        }
    }

    // ─── deleteRouteById ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteRouteById")
    class DeleteRouteById {

        @Test
        @DisplayName("Debe eliminar una ruta exitosamente")
        void shouldDeleteRouteSuccessfully() {
            Route route = buildRoute();
            when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

            routeService.deleteRouteById(1L);

            verify(routeRepository).delete(route);
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si la ruta no existe")
        void shouldThrowWhenRouteNotFound() {
            when(routeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> routeService.deleteRouteById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Ruta no encontrada");
        }
    }

    // ─── updateRoute ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRoute")
    class UpdateRoute {

        @Test
        @DisplayName("Debe actualizar el punto de salida exitosamente")
        void shouldUpdateDeparturePoint() {
            Route route = buildRoute();
            RouteUpdateRequest request = new RouteUpdateRequest();
            request.setDeparturePoint(new GeoLocation("La Romana", 18.4273, -68.9728));

            when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

            RouteResponse result = routeService.updateRoute(1L, request);

            assertThat(result).isNotNull();
            assertThat(route.getDeparturePoint().name()).isEqualTo("La Romana");
        }

        @Test
        @DisplayName("Debe actualizar el destino exitosamente")
        void shouldUpdateDestination() {
            Route route = buildRoute();
            RouteUpdateRequest request = new RouteUpdateRequest();
            request.setDestination(new GeoLocation("Puerto Plata", 19.7930, -70.6890));

            when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

            RouteResponse result = routeService.updateRoute(1L, request);

            assertThat(result).isNotNull();
            assertThat(route.getDestination().name()).isEqualTo("Puerto Plata");
        }

        @Test
        @DisplayName("Debe recalcular distancia con Haversine al actualizar puntos")
        void shouldRecalculateDistanceWhenUpdatingPoints() {
            Route route = buildRoute();
            RouteUpdateRequest request = new RouteUpdateRequest();
            request.setDeparturePoint(new GeoLocation("Santo Domingo", 18.4861, -69.9312));
            request.setDestination(new GeoLocation("Santiago", 19.4517, -70.6970));

            when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

            RouteResponse result = routeService.updateRoute(1L, request);

            assertThat(result).isNotNull();
            assertThat(route.getDistanceKm()).isCloseTo(134.0, within(5.0));
        }

        @Test
        @DisplayName("Debe lanzar EntityNotFoundException si la ruta no existe")
        void shouldThrowWhenRouteNotFound() {
            when(routeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> routeService.updateRoute(99L, new RouteUpdateRequest()))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage("Ruta no encontrada");
        }
    }
}