# Damose 0.3

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

## Architettura
Il progetto segue una struttura a livelli:
- `view`: UI e componenti mappa.
- `controller`: orchestrazione flussi applicativi.
- `service`: logica di dominio e integrazione realtime.
- `data.loader` / `data.mapper`: caricamento e matching GTFS.
- `model`: entita dominio.
- `database`: persistenza utente/sessione/preferiti.

### UML Class Diagram
```mermaid
classDiagram
    class DamoseApp {
      +main(String[] args)
    }

    class MainController {
      -ControllerDataLoader dataLoader
      -ControllerDataContext dataContext
      -RealtimeUpdateScheduler realtimeScheduler
      -MainView view
      -ConnectionMode mode
      +start()
    }

    class ControllerDataLoader {
      +load() ControllerDataContext
    }

    class ControllerDataContext {
      +getStops()
      +getTrips()
      +getStopTripMapper()
      +getRouteService()
      +getArrivalService()
    }

    class RealtimeUpdateScheduler {
      +start(...)
      +stop()
      +refreshMapOverlay(...)
    }

    class MainView
    class ArrivalService
    class RouteService
    class RealtimeService
    class GtfsParser
    class MapOverlayManager
    class StopTripMapper

    DamoseApp --> MainController
    MainController --> ControllerDataLoader
    MainController --> ControllerDataContext
    MainController --> RealtimeUpdateScheduler
    MainController --> MainView
    MainController --> RealtimeService
    RealtimeUpdateScheduler --> RealtimeService
    RealtimeUpdateScheduler --> GtfsParser
    RealtimeUpdateScheduler --> ArrivalService
    RealtimeUpdateScheduler --> MapOverlayManager
    ControllerDataLoader --> StopTripMapper
    ControllerDataLoader --> RouteService
    ControllerDataLoader --> ArrivalService
```

### UML Sequence Diagram (Avvio e primo ciclo realtime)
```mermaid
sequenceDiagram
    participant A as DamoseApp
    participant C as MainController
    participant L as ControllerDataLoader
    participant V as MainView
    participant R as RealtimeService
    participant S as RealtimeUpdateScheduler
    participant AS as ArrivalService

    A->>C: start()
    C->>L: load()
    L-->>C: ControllerDataContext
    C->>V: init() e setup listener
    C->>R: startPolling()
    C->>S: start(...)

    loop ogni 30s
      S->>R: getLatestTripUpdates()
      S->>R: getLatestVehiclePositions()
      S->>AS: updateRealtimeArrivals(...)
      S->>V: update mappa (invokeLater)
    end
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
- `DOCUMENTAZIONE_PROGETTO_IT.md`: descrizione estesa del progetto (italiano + UML).
