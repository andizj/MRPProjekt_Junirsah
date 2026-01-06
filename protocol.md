# Protokoll - Media Ratings Platform (MRP)

**Autor:** Andi Junirsah
**Datum:** 6. Januar 2026
**Git Repository:** https://github.com/andizj/MRPProjekt_Junirsah
**Branch:** main

---

## 1. Design-Entscheidungen

### Architektur

Ich habe mich für eine klare **Schichten-Architektur (Layered Architecture)** entschieden, um eine saubere Trennung von Verantwortlichkeiten (Separation of Concerns) zu gewährleisten. Dies erleichtert die Wartbarkeit und Testbarkeit erheblich.

**Die Schichten:**
- **App (Entry Point):** Initialisiert die Datenbankverbindung, Repositories, Services und startet den HTTP-Server.
- **API (Presentation Layer):**
    - **Dispatcher:** Der `UserRequestDispatcher` routet komplexe Pfade an spezifische Controller weiter.
    - **Handler/Controller:** Verarbeiten HTTP-Anfragen, parsen JSON und delegieren an die Services (z. B. `MediaHandler`, `UserProfileController`).
- **Service (Business Layer):** Enthält die Geschäftslogik (z. B. Algorithmus für Empfehlungen, Validierungen). Hängt nur von Repository-Interfaces ab (Dependency Inversion).
- **DAO (Data Access Layer):**
    - **Interfaces:** Definieren den Vertrag für den Datenzugriff.
    - **Implementierungen:** Enthalten den konkreten JDBC/SQL-Code.
- **Database (Infrastructure):** Verwaltet die JDBC-Verbindung.

### Verwendete Technologien

- **Java HTTP Server:** Nutzung des eingebauten `com.sun.net.httpserver` für eine leichtgewichtige Lösung ohne große Frameworks wie Spring.
- **Jackson:** `ObjectMapper` für die JSON-Serialisierung und Deserialisierung.
- **PostgreSQL & JDBC:** Direkter Datenbankzugriff mittels SQL und `PreparedStatement` zur Vermeidung von SQL-Injection.
- **Lombok:** Reduzierung von Boilerplate-Code (Getter, Setter, Konstruktoren) in den Model-Klassen durch Annotationen wie `@Data`.
- **BCrypt:** Sicheres Hashen von Passwörtern.
- **JUnit 5 & Mockito:** Für Unit-Tests und Mocking von Abhängigkeiten.

### Authentifizierung

Die Authentifizierung erfolgt Token-basiert:
- **Token:** UUID-String, der beim Login generiert und in der Datenbank (`tokens` Tabelle) gespeichert wird.
- **Übertragung:** Client sendet den Token im HTTP-Header `Authorization: Bearer <token>`.
- **Validierung:** Der `BaseHandler` prüft den Token bei jedem geschützten Request und lädt den zugehörigen User.

---

## 2. Projektstruktur


```

src/main/java/at/technikum_wien/mrp/
├── App.java                      # Main-Klasse
├── api/                          # HTTP Handler & Dispatcher
│   ├── BaseHandler.java
│   ├── UserRequestDispatcher.java
│   ├── UserRequestHelper.java
│   ├── MediaHandler.java
│   ├── ... (weitere Handler)
├── controller/                   # Spezifische Controller für Sub-Ressourcen
│   ├── UserProfileController.java
│   ├── UserMediaController.java
│   └── UserRatingController.java
├── service/                      # Geschäftslogik
│   ├── UserService.java
│   ├── MediaService.java
│   ├── RatingService.java
│   └── AuthService.java
├── dao/                          # Data Access Objects
│     interfaces/
│     ├── UserRepositoryIF.java     # Interfaces
│     ├── ... (weitere Interfaces)
│     impl/
│     ├── UserRepository.java       # Implementierung
│     ├── ... (weitere Repos)
├── model/                        # Datenmodelle (POJOs mit Lombok)
│   ├── User.java
│   ├── MediaEntry.java
│   ├── Rating.java
│   ├── Token.java
│   └── UserProfileStats.java
└── database/                     # DB Infrastruktur
├── DatabaseConnectionIF.java
└── DatabaseConnection.java

```

---

## 3. Datenbankschema

Das Schema basiert auf PostgreSQL.

**users**
```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    favorite_genre VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

```

**media**

```sql
CREATE TABLE media (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    media_type VARCHAR(50) NOT NULL,
    release_year INT,
    genres TEXT[] DEFAULT '{}',
    age_restriction INT DEFAULT 0,
    creator_id INT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

```

**ratings**

```sql
CREATE TABLE ratings (
    id SERIAL PRIMARY KEY,
    media_id INT REFERENCES media(id),
    user_id INT REFERENCES users(id),
    stars INT,
    comment TEXT,
    visible BOOLEAN DEFAULT FALSE, -- Muss bestätigt werden
    created_at TIMESTAMP DEFAULT NOW()
);

```

**favorites** (m:n Beziehung)

```sql
CREATE TABLE favorites (
    user_id INT REFERENCES users(id),
    media_id INT REFERENCES media(id),
    PRIMARY KEY (user_id, media_id)
);

```

---

## 4. API Endpoints

### Authentifizierung & User

| Method | Endpoint | Beschreibung |
| --- | --- | --- |
| POST | `/api/users/register` | Neuen User registrieren |
| POST | `/api/users/login` | Login (erhält Token) |
| GET | `/api/users/{id}/profile` | Profilstatistiken abrufen |
| PUT | `/api/users/{id}/profile` | Profil aktualisieren (Self) |
| GET | `/api/users/{id}/recommendations` | Personalisierte Empfehlungen |
| GET | `/api/users/{id}/favorites` | Liste der favorisierten Medien |
| GET | `/api/users/{id}/ratings` | Liste der eigenen Bewertungen |

### Media (Filme/Serien/Games)

| Method | Endpoint | Beschreibung |
| --- | --- | --- |
| GET | `/api/media` | Alle Medien + Filter (search, type, genre...) |
| GET | `/api/media/{id}` | Detailansicht eines Mediums |
| POST | `/api/media` | Neues Medium anlegen |
| PUT | `/api/media/{id}` | Medium bearbeiten (nur Creator) |
| DELETE | `/api/media/{id}` | Medium löschen (nur Creator) |
| POST | `/api/media/{id}/rate` | Medium bewerten |

### Ratings & Actions

| Method | Endpoint | Beschreibung |
| --- | --- | --- |
| GET | `/api/ratings/average/{mediaId}` | Durchschnittsbewertung |
| PUT | `/api/ratings/{id}/confirm` | Bewertung sichtbar schalten (Owner) |
| POST | `/api/ratings/{id}/like` | Ein Rating liken |
| DELETE | `/api/ratings/{id}/like` | Like entfernen |
| GET | `/api/leaderboard` | Top 10 aktivste User |

---

## 5. Business Logic Highlights

### Dynamische Empfehlungen (Recommendation Engine)

Der Algorithmus im `MediaService` funktioniert wie folgt:

1. Lade alle Ratings des Users.
2. Identifiziere Genres von Medien, die der User mit **≥ 4 Sternen** bewertet hat.
3. Suche in der Datenbank nach anderen Medien, die diese Genres enthalten.
4. Filtere Medien heraus, die der User bereits kennt (bewertet hat).
5. Gib die Liste der Kandidaten zurück.

### Sichtbarkeit von Ratings

Neu erstellte Ratings sind standardmäßig **unsichtbar** (`visible = false`). Der User muss das Rating explizit über den Endpunkt `/confirm` bestätigen, damit es öffentlich sichtbar wird und im Durchschnitt sowie im Leaderboard zählt.

### Lieblings-Genre Berechnung

Das "Favorite Genre" im Userprofil wird dynamisch berechnet, indem die Häufigkeit der Genres in den vom User bewerteten Medien gezählt wird. Das häufigste Genre gewinnt.

---

## 6. Testing

Es wurden Unit-Tests mit **JUnit 5** und **Mockito** erstellt, um die Service-Schicht isoliert von der Datenbank zu testen.

| Testklasse | Anzahl Tests | Fokus |
| --- | --- | --- |
| `UserServiceTest` | 2 | Profil-Statistiken, Lieblings-Genre Logik |
| `AuthServiceTest` | 4 | Login (Success/Fail), Register (Success/Fail) |
| `MediaServiceTest` | 5 | CRUD, Validierung, Security (Owner-Check), Empfehlungslogik |
| `RatingServiceTest` | 6 | Default Visibility, Confirm-Logik, Durchschnittsberechnung, Leaderboard |
| `BaseHandlerTest` | 4 | Hilfsmethoden (ID-Extraktion, Query-Parsing) |
| `RatingActionHandlerTest` | 1 | Dispatching von Like-Actions |

**Gesamt:** ~22 Tests, die kritische Geschäftslogik und Security-Constraints abdecken.

---

## 7. SOLID Prinzipien Analyse

Mein Design legt besonderen Wert auf Clean Code Prinzipien:

1. **Single Responsibility Principle (SRP):**
* Die Aufteilung der User-Route in `UserRequestDispatcher` und spezialisierte Controller (`UserProfileController`, `UserMediaController`) verhindert monolithische Klassen.
* Services kümmern sich rein um Logik, DAOs rein um SQL.


2. **Open/Closed Principle (OCP):**
* Die `MediaRepository`-Suche ist erweiterbar.
* Services hängen von Interfaces ab, was Erweiterungen (z.B. neue Speicherarten) ermöglicht, ohne den Service-Code zu ändern.


3. **Dependency Inversion Principle (DIP):**
* Die Services (`UserService`, `MediaService`...) instanziieren keine Repositories, sondern erhalten Interfaces (`UserRepositoryIF`) über den Konstruktor injiziert.



---

## 8. Lessons Learned

* **Dispatcher Pattern:** Das Refactoring von einem riesigen `UserProfileHandler` hin zu einem Dispatcher mit kleinen Controllern hat den Code deutlich lesbarer und wartbarer gemacht.
* **JDBC vs. ORM:** Die Arbeit mit reinem JDBC und `ResultSet` Mapping ist zwar schreibintensiver als Hibernate, bietet aber volle Kontrolle über die SQL-Queries und Performance.
* **Testing:** Das Mocking der Repository-Interfaces war essenziell, um die Geschäftslogik (z. B. Empfehlungen) zu testen, ohne eine echte Datenbank hochfahren zu müssen.

---

## 9. Zeiterfassung

| Aufgabe | Zeitaufwand (ca.) |
| --- | --- |
| Datenbank-Design & Setup | 5h |
| Implementierung DAOs & Models | 8h |
| Services & Business Logic | 10h |
| API Layer (Handler & Dispatcher) | 12h |
| Refactoring (SOLID Optimierung) | 6h |
| Unit Tests & Bugfixing | 6h |
| Dokumentation | 3h |
| **Gesamt** | **~50h** |

---

## 10. Setup & Start

### Voraussetzungen

* Java 17+ SDK
* Docker

### Ausführung

1. Projekt kompilieren und starten:
```bash
# Datenbank starten
docker-compose up -d

# Aus dem Root-Verzeichnis
javac -d out -cp "lib/*" src/main/java/at/technikum_wien/mrp/App.java
java -cp "out:lib/*" at.technikum_wien.mrp.App

```

3. Server läuft auf `http://localhost:8080`.
