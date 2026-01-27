# 🚌 Damose - Rome Bus Tracker

A real-time bus tracking application for Rome's public transit system, built with Java Swing and GTFS-RT data.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?style=flat-square&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

<p align="center">
  <img src="src/main/resources/sprites/bus.png" alt="Bus Icon" width="120"/>
</p>

![Status](https://img.shields.io/badge/Status-In%20Review-yellow?style=for-the-badge)
![ETA](https://img.shields.io/badge/ETA-10%20Febbraio-blue?style=for-the-badge)

> Il progetto è attualmente in fase di revisione.  
> La release completa è prevista per il **10 Febbraio**.

## ✨ Features

- **Real-Time Bus Tracking** — Live bus positions on an interactive map using GTFS-RT feeds from Roma Mobilità
- **Arrival Predictions** — View upcoming arrivals at any stop with real-time delay/early status
- **Interactive Map** — Pan, zoom, and click on stops to see arrival information with smooth animations
- **Route Visualization** — Select a bus line to see its complete route highlighted on the map, showing only buses of that line
- **Stop & Line Search** — Spotlight-style search to find stops by name/ID or search for specific bus lines
- **Favorites System** — Mark stops and lines as favorites for quick access (star icon)
- **All Trips View** — View all scheduled trips passing through a stop for the entire day
- **User Accounts** — Optional login/registration with SQLite database
- **Online/Offline Mode** — Toggle between real-time and static data with connection status indicator
- **Modern Frameless UI** — Sleek borderless window with custom controls, dark theme powered by FlatLaf
- **Memory Management** — Built-in garbage collector and memory monitoring

## 📸 How It Works

1. **Login** — Optional login screen (skip to continue without account)
2. **Loading** — Animated loading screen shows GTFS data initialization and RT connection status
3. **Map View** — The main window displays an interactive fullscreen map of Rome
4. **Bus Icons** — Real-time bus positions are displayed as markers on the map
5. **Stop Markers** — Click on any bus stop to see upcoming arrivals
6. **Floating Panel** — Arrivals appear in a tooltip-style panel showing:
   - 🔴 Red dot = Bus is delayed
   - 🟢 Green dot = Bus is on time or early
   - ⚪ Gray dot = Static schedule (no real-time data)
   - ⭐ Star button = Add/remove from favorites
   - 📋 View all trips of the day
7. **Search** — Use the search button (🔍) to find stops or lines
   - **Stops tab** — Search by name or stop ID
   - **Lines tab** — Search bus routes
   - **Favorites tab** — Quick access to saved stops/lines
8. **Connection Toggle** — WiFi button (top-right) to switch between online/offline mode

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java 17** | Core language |
| **Swing** | GUI framework |
| **JXMapViewer2** | Interactive map rendering |
| **GTFS-RT Bindings** | Real-time transit data parsing |
| **Protocol Buffers** | Binary data serialization |
| **SQLite** | Local database for user accounts |
| **Apache Commons CSV** | GTFS static file parsing |
| **FlatLaf** | Modern look-and-feel theme |
| **Gson** | JSON processing |

## 📦 Installation

### Prerequisites

- Java 17 or higher
- Maven 3.x

### Build & Run

```bash
# Clone the repository
git clone https://github.com/yourusername/damose.git
cd damose

# Build the project
mvn clean compile

# Run the application
mvn exec:java
```

## 📁 Project Structure

```
src/main/java/damose/
├── app/
│   └── DamoseApp.java              # Application entry point
│
├── config/
│   └── AppConstants.java           # Centralized constants (colors, fonts, URLs)
│
├── controller/
│   └── MainController.java         # Main application controller
│
├── data/
│   ├── loader/                     # GTFS file loaders
│   │   ├── CalendarLoader.java
│   │   ├── CsvParser.java
│   │   ├── StopsLoader.java
│   │   ├── StopTimesLoader.java
│   │   └── TripsLoader.java
│   ├── mapper/                     # Data mapping utilities
│   │   ├── StopTripMapper.java
│   │   ├── TripIdUtils.java
│   │   └── TripMatcher.java
│   └── model/                      # Data models
│       ├── Stop.java
│       ├── StopTime.java
│       ├── Trip.java
│       ├── TripServiceCalendar.java
│       ├── TripUpdateRecord.java
│       └── VehiclePosition.java
│
├── database/
│   ├── DatabaseManager.java        # SQLite connection management
│   ├── SessionManager.java         # User session handling
│   ├── User.java                   # User model
│   └── UserService.java            # Authentication service
│
├── model/
│   ├── BusWaypoint.java            # Bus marker on map
│   ├── ConnectionMode.java         # Online/Offline enum
│   └── StopWaypoint.java           # Stop marker on map
│
├── service/                        # Business logic layer
│   ├── ArrivalService.java         # Arrival time calculations
│   ├── FavoritesService.java       # User favorites management
│   ├── GtfsParser.java             # GTFS-RT feed parsing
│   ├── MemoryManager.java          # Memory monitoring & GC
│   ├── RealtimeService.java        # RT feed fetching & caching
│   ├── RouteService.java           # Route/line operations
│   └── StaticSimulator.java        # Offline mode simulation
│
└── ui/
    ├── MainView.java               # Main application window
    ├── component/                  # Reusable UI components
    │   ├── ConnectionButton.java   # Online/Offline toggle
    │   ├── FloatingArrivalPanel.java
    │   └── SearchOverlay.java
    ├── dialog/                     # Modal dialogs
    │   ├── LoadingDialog.java
    │   └── LoginDialog.java
    ├── map/                        # Map utilities
    │   ├── GeoUtils.java
    │   ├── MapAnimator.java        # Smooth map transitions
    │   ├── MapFactory.java
    │   └── MapOverlayManager.java
    └── render/                     # Custom waypoint renderers
        ├── BusWaypointRenderer.java
        ├── RoutePainter.java
        └── StopWaypointRenderer.java

src/main/resources/
├── gtfs_static/                    # Static GTFS data files
│   ├── stops.txt
│   ├── trips.txt
│   ├── stop_times.txt
│   ├── calendar_dates.txt
│   └── ...
├── sprites/                        # UI icons
│   ├── bus.png
│   ├── stop.png
│   ├── star.png
│   ├── wifi.png
│   ├── nowifi.png
│   ├── connecting.gif
│   └── lente.png
└── data/
    └── favorites.txt               # User favorites storage
```

## 🌐 Data Sources

This application uses GTFS and GTFS-RT data from [Roma Mobilità](https://romamobilita.it/):

| Feed | URL |
|------|-----|
| Vehicle Positions | `https://romamobilita.it/.../rome_rtgtfs_vehicle_positions_feed.pb` |
| Trip Updates | `https://romamobilita.it/.../rome_rtgtfs_trip_updates_feed.pb` |

Static GTFS data is bundled in `src/main/resources/gtfs_static/`.

## ⚙️ Configuration

All configuration is centralized in `AppConstants.java`:

| Constant | Value | Description |
|----------|-------|-------------|
| `RT_UPDATE_INTERVAL_MS` | 30,000 | Real-time feed refresh interval |
| `RT_TIMEOUT_SECONDS` | 30 | Timeout for RT connection at startup |
| `HTTP_CONNECT_TIMEOUT_MS` | 30,000 | HTTP connection timeout |
| `ROME_LAT/LON` | 41.9028, 12.4964 | Default map center |

## 🎨 UI Theme

The application uses a custom **Midnight Dark** theme with a frameless window design:

- Background: `#111115` (dark)
- Panel Background: `#1C1C1E` (dark gray)
- Accent: `#58A6FF` (blue)
- Success: `#63D263` (green)
- Error: `#FF6363` (red)
- Text: `#E5E5EA` (light gray)

### Window Controls
- Custom minimize, maximize, restore, and close buttons
- Draggable window from any point on the map
- Rounded corners (when not maximized)

## 🔧 Architecture

The codebase follows a clean layered architecture:

```
┌─────────────────────────────────────────┐
│              UI Layer                    │
│  (MainView, Dialogs, Components)        │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│           Controller Layer              │
│         (MainController)                │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│           Service Layer                 │
│  (ArrivalService, RealtimeService,      │
│   RouteService, FavoritesService)       │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│            Data Layer                   │
│  (Loaders, Mappers, Models, Database)   │
└─────────────────────────────────────────┘
```

## 📄 License

This project is open source under the MIT License.

## 🤝 Contributing

Contributions are welcome! Feel free to:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 🙏 Acknowledgments

- [Roma Mobilità](https://romamobilita.it/) for providing open transit data
- [JXMapViewer2](https://github.com/msteiger/jxmapviewer2) for the excellent map library
- [FlatLaf](https://www.formdev.com/flatlaf/) for the modern Swing look-and-feel
- [MobilityData](https://gtfs.mobilitydata.org/) for GTFS-RT specifications

---

<p align="center">
  Made with ☕ and 🚌 in Rome<br>
  <b>Damose!</b> 🇮🇹
</p>
