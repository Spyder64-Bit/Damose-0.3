# Damose
<p align="center">
  <img src="src/main/resources/sprites/icon.png" alt="Damose icon" width="120" />
</p>

## Come Installare?

### Opzione 1: EXE (Windows)
1. Genera i pacchetti:
```bash
mvn clean package
```
2. Crea l'installer `.exe` con `jpackage` (JDK 17 + WiX Toolset 3.x richiesti):
```bash
jpackage --type exe --name Damose --input target --main-jar damose-bus-tracker-1.0.0.jar --main-class damose.app.DamoseApp --dest target/installer
```
3. Avvia:
`target/installer/Damose-1.0.exe`

### Opzione 2: JAR (cross-platform)
1. Download diretto del jar:
[damose-bus-tracker-1.0.0.jar](docs/damose-bus-tracker-1.0.0.jar)

2. Avvia:
```bash
java -jar damose-bus-tracker-1.0.0.jar
```

3. In alternativa, genera il jar eseguibile localmente:
```bash
mvn clean package
```
4. Avvia:
```bash
java -jar target/damose-bus-tracker-1.0.0.jar
```

Nota: il file `.exe` e il `.jar` vengono prodotti in locale nella cartella `target/`.

Applicazione desktop Java per il monitoraggio del trasporto pubblico di Roma, basata su:
- dati GTFS statici (`stops`, `trips`, `stop_times`, `calendar_dates`);
- feed GTFS Realtime (`TripUpdates`, `VehiclePositions`);
- interfaccia Swing con mappa interattiva.

## Stack Tecnologico
- Java 17
- Maven 3
- Swing + FlatLaf
- JXMapViewer2
- Protocol Buffers + GTFS-RT bindings
- SQLite

## Funzionalita Principali
- Visualizzazione mezzi in tempo reale su mappa.
- Arrivi fermata con integrazione realtime/statico.
- Ricerca fermate e linee.
- Preferiti (fermate/linee).
- Modalita online/offline.
- Vista "tutte le corse del giorno" nel pannello fermata.
- Overlay info integrato con link cliccabili.
- Filtro route/vehicle migliorato per ridurre mismatch realtime.
- Rendering mappa migliorato: meno sovrapposizioni waypoint e fit linea piu stabile.

## Architettura
Il progetto segue una struttura a livelli:
- `view`: UI e componenti mappa.
- `controller`: orchestrazione flussi applicativi.
- `service`: logica di dominio e integrazione realtime.
- `data.loader` / `data.mapper`: caricamento e matching GTFS.
- `model`: entita dominio.
- `database`: persistenza utente/sessione/preferiti.

### UML Class Diagram (Detailed, post-refactor)
```mermaid
classDiagram
    direction LR

    class DamoseApp {
      +main(String[] args)
    }

    class MainController {
      -ControllerDataLoader dataLoader
      -RealtimeUpdateScheduler realtimeScheduler
      -RouteViewportNavigator routeViewport
      -RouteVehicleMarkerBuilder routeVehicleMarkerBuilder
      -VehiclePanelInfoBuilder vehiclePanelInfoBuilder
      -RoutePanelFlow routePanelFlow
      -StopPanelFlow stopPanelFlow
      -VehicleFollowFlow vehicleFollowFlow
      -MainView view
      +start()
      -setupViewCallbacks()
      -setupSearchPanel()
      -handleStopSelection(stop, fromSearch)
      -handleLineSelection(fakeLine)
      -onRouteDirectionSelected(directionId)
      -onRouteStopSelected(stop)
      -refreshMapOverlay()
      -startRealtimeUpdates()
    }

    class ControllerDataLoader {
      +load() ControllerDataContext
    }

    class ControllerDataContext {
      -List~Stop~ stops
      -List~Trip~ trips
      -TripMatcher tripMatcher
      -StopTripMapper stopTripMapper
      -RouteService routeService
      -ArrivalService arrivalService
      +getStops()
      +getTrips()
      +getTripMatcher()
      +getStopTripMapper()
      +getRouteService()
      +getArrivalService()
    }

    class RealtimeUpdateScheduler {
      -Timer timer
      +start(view, trips, stopTripMapper, arrivalService, modeSupplier, feedTsConsumer, positionsConsumer)
      +stop()
      +refreshMapOverlay(view, trips, mode, positionsConsumer)
      -runCycle(view, trips, stopTripMapper, arrivalService, modeSupplier, feedTsConsumer, positionsConsumer)
    }

    class RoutePanelFlow {
      -MainView view
      -ControllerDataContext dataContext
      -RouteViewportNavigator routeViewport
      -RoutePanelState routePanelState
      +chooseInitialDirection(directions)
      +closeRoutePanelOverlay(hideFloatingPanel)
      +resetRoutePanelUiState()
      +applyRouteSelectionStateAndView(routeId, routeName, routeStops, routeShape, directions, selectedDirection, hideFloatingPanel)
    }

    class StopPanelFlow {
      -MainView view
      -ControllerDataContext dataContext
      +refreshFloatingPanelIfVisible(mode, currentFeedTs)
      +showFloatingArrivals(stop, mode, currentFeedTs)
      -findStopById(stopId)
      -showPanel(stop, arrivi, isFavorite)
    }

    class VehicleFollowFlow {
      -MainView view
      -RouteViewportNavigator routeViewport
      -VehicleTrackingResolver vehicleTrackingResolver
      -RoutePanelState routePanelState
      -FollowedVehicleState followedVehicleState
      -VehiclePanelInfoBuilder vehiclePanelInfoBuilder
      +onRouteVehicleSelected(marker)
      +onVehiclePositionsUpdated(positions, routeVehicleMarkerBuilder)
      +clearFollowedVehicle()
      +clearFollowedVehicle(hidePanel)
      -updateFollowedVehicleTracking(positions, animate)
    }

    class RoutePanelState {
      -String routeId
      -List~Stop~ routeStops
      -Integer direction
      -String routeName
      -boolean circular
      +apply(routeId, routeName, routeStops, direction, circular)
      +reset()
      +routeId()
      +routeStops()
      +direction()
      +routeName()
      +isCircular()
      +hasRoute()
    }

    class FollowedVehicleState {
      -String markerId
      -int missCount
      +follow(markerId)
      +clear()
      +markerId()
      +resetMisses()
      +incrementMissAndReached(threshold)
    }

    class VehicleTrackingResolver {
      +findByMarkerId(markerId, positions, routeFilter, directionFilter, tripMatcher)
    }

    class VehiclePanelInfoBuilder {
      -ControllerDataContext dataContext
      +build(vehiclePosition) VehiclePanelInfo
    }

    class MainView {
      -SearchOverlaySection searchOverlaySection
      -FloatingPanelSection floatingPanelSection
      -RoutePanelSection routePanelSection
      +init()
      +showSearchOverlay()
      +setSearchData(stops, lines)
      +setOnSearchSelect(callback)
      +showInfoOverlay()
      +showFloatingPanel(stopName, stopId, arrivi, isFavorite, pos, anchorGeo)
      +showVehicleFloatingPanel(title, rows, anchorGeo)
      +refreshVehicleFloatingPanel(title, rows, anchorGeo)
      +refreshFloatingPanel(stopName, stopId, arrivi, isFavorite)
      +showAllTripsInPanel(allTrips)
      +showRouteSidePanel(routeName, routeStops)
      +setRouteSidePanelDirections(directions, selectedDirection)
      +updateRouteSidePanelVehicles(markers)
      +hideRouteSidePanel()
      +hideFloatingPanel()
      +showBottomNotice(message)
    }

    class SearchOverlaySection {
      -SearchOverlay searchOverlay
      -InfoOverlay infoOverlay
      +initialize(hostPane, width, height)
      +updateBounds(width, height)
      +showSearchOverlay()
      +showInfoOverlay()
      +setSearchData(stops, lines)
      +setOnSearchSelect(callback)
      +setOnFavoritesLoginRequired(callback)
      +showFavorites(favorites)
    }

    class FloatingPanelSection {
      -FloatingArrivalPanel floatingPanel
      -FloatingPanelCoordinator coordinator
      +setOnClose(callback)
      +setPreferredMaxRows(maxRows)
      +setOnFavoriteToggle(callback)
      +setOnViewAllTrips(callback)
      +showStopPanel(stopName, stopId, arrivals, isFavorite, position, anchorGeo)
      +refreshStopPanel(stopName, stopId, arrivals, isFavorite)
      +showVehiclePanel(title, rows, anchorGeo)
      +refreshVehiclePanel(title, rows, anchorGeo)
      +showAllTrips(allTrips)
      +updatePosition()
      +hide()
      +isVisible()
      +getCurrentStopId()
    }

    class RoutePanelSection {
      -RouteSidePanelCoordinator coordinator
      +setOnClose(callback)
      +setOnDirectionSelected(callback)
      +setOnVehicleMarkerSelected(callback)
      +setOnStopSelected(callback)
      +updateBounds(hostWidth, hostHeight)
      +showRoute(routeName, routeStops)
      +setDirections(directions, selectedDirection)
      +setVehicleMarkers(markers)
      +hide()
    }

    class FloatingPanelCoordinator {
      -GeoPosition floatingAnchorGeo
      +showStopPanel(stopName, stopId, arrivals, isFavorite, pos, anchorGeo)
      +refreshStopPanel(stopName, stopId, arrivals, isFavorite)
      +showVehiclePanel(panelTitle, rows, anchorGeo)
      +refreshVehiclePanel(panelTitle, rows, anchorGeo)
      +showAllTrips(allTrips)
      +updatePosition()
      +hide()
      +isVisible()
      +getCurrentStopId()
      +clearAnchor()
    }

    class RouteSidePanelCoordinator {
      -RouteSidePanel panel
      +setOnClose(callback)
      +setOnDirectionSelected(callback)
      +setOnVehicleMarkerSelected(callback)
      +setOnStopSelected(callback)
      +updateBounds(hostWidth, hostHeight)
      +showRoute(routeName, routeStops)
      +setDirections(directions, selectedDirection)
      +setVehicleMarkers(markers)
      +hide()
    }

    class ArrivalService {
      -Map realtimeArrivals
      -Map realtimeArrivalsByRoute
      -TripMatcher matcher
      -StopTripMapper stopTripMapper
      -TripServiceCalendar tripServiceCalendar
      -RouteFallbackPredictionAssigner routeFallbackPredictionAssigner
      +updateRealtimeArrivals(updates)
      +updateRealtimeArrivals(updates, referenceEpochSeconds)
      +computeArrivalsForStop(stopId, mode, currentFeedTs)
      +getAllTripsForStopToday(stopId, mode, currentFeedTs)
      -lookupRealtimeArrivalEpochByTripAndStop(stopTime, stopId)
    }

    class RouteFallbackPredictionAssigner {
      +lookupRouteFallbackArrivalEpoch(stopId, routeId, scheduledEpoch)
      +assignRouteFallbackPredictions(stopId, allTrips)
    }

    class ArrivalFormattingSupport {
      +formatTripInfo(info)
      +formatArrivalInfo(info, nowEpochSeconds)
    }

    class RouteService {
      +findRepresentativeTrip(routeId, headsign)
      +findTripsByRouteId(routeId)
      +getStopsForTrip(tripId)
      +getStopsForRoute(routeId)
      +getStopsForRouteAndDirection(routeId, directionId)
      +getShapeForRoute(routeId)
      +getShapeForRouteAndDirection(routeId, directionId)
      +getHeadsignsForRoute(routeId)
      +getDirectionsForRoute(routeId)
      +getRepresentativeHeadsignForRouteAndDirection(routeId, directionId)
    }

    class MapOverlayManager {
      +updateMap(mapViewer, highlightedStops, vehicles, trips)
      +setRoute(routeStops, routeShape)
      +clearRoute()
      +setBusRouteFilter(routeId)
      +setBusDirectionFilter(direction)
      +clearBusRouteFilter()
      +clearBusDirectionFilter()
      +setSelectedVehicleMarkerId(markerId)
      +clearSelectedVehicleMarkerId()
    }

    class RealtimeService {
      +startPolling()
      +stopPolling()
      +getLatestTripUpdates()
      +getLatestVehiclePositions()
      +hasRealTimeData()
    }

    class StopTripMapper {
      +getStopTimesForStop(stopId)
      +getStopTimesForTrip(tripId)
    }

    class TripMatcher {
      +matchByTripId(tripId)
      +matchByTripIdAndRoute(tripId, routeId, directionId)
    }

    class SearchOverlay
    class InfoOverlay
    class FloatingArrivalPanel
    class RouteSidePanel

    DamoseApp --> MainController

    MainController --> ControllerDataLoader
    MainController --> ControllerDataContext
    MainController --> RealtimeUpdateScheduler
    MainController --> RouteViewportNavigator
    MainController --> RouteVehicleMarkerBuilder
    MainController --> VehiclePanelInfoBuilder
    MainController --> RoutePanelFlow
    MainController --> StopPanelFlow
    MainController --> VehicleFollowFlow
    MainController --> VehicleTrackingResolver
    MainController --> RoutePanelState
    MainController --> MainView
    MainController --> RealtimeService

    ControllerDataLoader --> ControllerDataContext
    ControllerDataContext --> StopTripMapper
    ControllerDataContext --> RouteService
    ControllerDataContext --> ArrivalService
    ControllerDataContext --> TripMatcher

    RealtimeUpdateScheduler --> RealtimeService
    RealtimeUpdateScheduler --> ArrivalService
    RealtimeUpdateScheduler --> MapOverlayManager

    RoutePanelFlow --> MainView
    RoutePanelFlow --> ControllerDataContext
    RoutePanelFlow --> RouteViewportNavigator
    RoutePanelFlow --> RoutePanelState
    RoutePanelFlow --> MapOverlayManager

    StopPanelFlow --> MainView
    StopPanelFlow --> ControllerDataContext

    VehicleFollowFlow --> MainView
    VehicleFollowFlow --> RouteViewportNavigator
    VehicleFollowFlow --> VehicleTrackingResolver
    VehicleFollowFlow --> RoutePanelState
    VehicleFollowFlow --> FollowedVehicleState
    VehicleFollowFlow --> VehiclePanelInfoBuilder
    VehicleFollowFlow --> MapOverlayManager

    VehicleTrackingResolver --> TripMatcher
    VehiclePanelInfoBuilder --> ControllerDataContext

    MainView --> SearchOverlaySection
    MainView --> FloatingPanelSection
    MainView --> RoutePanelSection

    SearchOverlaySection --> SearchOverlay
    SearchOverlaySection --> InfoOverlay

    FloatingPanelSection --> FloatingPanelCoordinator
    FloatingPanelSection --> FloatingArrivalPanel

    RoutePanelSection --> RouteSidePanelCoordinator
    RouteSidePanelCoordinator --> RouteSidePanel

    FloatingPanelCoordinator --> FloatingArrivalPanel

    ArrivalService --> RouteFallbackPredictionAssigner
    ArrivalService --> ArrivalFormattingSupport
    ArrivalService --> StopTripMapper
    ArrivalService --> TripMatcher

    RouteService --> TripMatcher
```

## Struttura Progetto (sintesi)
```text
src/main/java/damose/
  app/
  controller/
  service/
  data/loader/
  data/mapper/
  model/
  view/
  database/
```

## Avvio Locale
```bash
mvn clean compile
mvn exec:java
```

## Dati
- Feed RT:
  - `https://romamobilita.it/sites/default/files/rome_rtgtfs_vehicle_positions_feed.pb`
  - `https://romamobilita.it/sites/default/files/rome_rtgtfs_trip_updates_feed.pb`
- GTFS statico locale in `src/main/resources/gtfs_static/`.

## Documentazione Aggiuntiva
- `docs/javadoc/index.html`: documentazione API generata.
- `docs/ClassDiagram_Structured.pdf`: unico diagramma UML mantenuto.
