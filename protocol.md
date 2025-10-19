## 1. Projektübersicht und Technologie

### Implementierte Kernfunktionalität

* **User Management:** Registrierung und Login mit Token-Generierung und -Validierung.
* **Media CRUD:** Erstellung, Abruf, Aktualisierung und Löschen von Medieneinträgen.
* **HTTP Server:** Implementierung als Standalone-Anwendung in Java unter Verwendung der com.sun.net.httpserver Standardbibliothek

 

## 2. Architekturentscheidungen

### 2.1 Schichtentrennung

Die Anwendung ist in die folgenden Schichten unterteilt:

| Schicht | Package  | OOP-Prinzipien         | Verantwortung <br/>                                                |
| :--- |:---------|:-----------------------|:-----------------------------------------------------------------------------------|
| **API/Handler** | .api | Polymorphie, Kapselung | **HTTP-Verarbeitung:** Routing, Header-Management, JSON-Mapping und Delegation.    |
| **Service** | .service| Abstraktion, Kapselung | **Geschäftslogik:** Token-Validierung, Passwort-Hashing, und Autorisierungsregeln. |
| **DAO/Repository** | .dao | Abstraktion, Kapselung | **Datenhaltung:** Verwaltung der In-Memory HashMaps und Zugriffsmethoden.          |

### 2.2 Einhaltung der SOLID-Prinzipien


* Single Responsibility Principle (SRP): Wie oben beschrieben, hat jede Klasse nur einen Grund zur Änderung (z.B. der AuthService ändert sich nur, wenn sich die Token-Logik ändert, nicht wenn sich die Datenhaltung ändert).

* Open/Closed Principle (OCP): Die Klassen sind offen für Erweiterungen (z.B. Hinzufügen einer neuen Funktion zum MediaService), aber geschlossen für Modifikationen. Dies wird durch die Verwendung von Interfaces erreicht; das Hinzufügen einer neuen DAO-Implementierung (z.B. SQL) erfordert keine Änderung am AuthService.

* Liskov Substitution Principle (LSP): Die konkreten Repository-Implementierungen (z.B. UserRepository) erfüllen den Vertrag ihrer Interfaces (UserRepositoryIF) vollständig. Jede Klasse, die das Interface verwendet, kann jede Implementierung davon nutzen, ohne ihr Verhalten ändern zu müssen.

* Interface Segregation Principle (ISP): Es wurden feingranulare Interfaces für jede DAO-Klasse erstellt (z.B. TokenRepositoryIF, RatingRepositoryIF). Keine aufrufende Klasse ist gezwungen, von Methoden abhängig zu sein, die sie nicht benötigt.

* Dependency Inversion Principle (DIP): Dieses Prinzip ist das Fundament der Architektur. Die höherrangigen Schichten (Services) hängen von Abstraktionen (den Interfaces) ab und nicht von den konkreten, niedrigrangigen Implementierungen (new UserRepository()). Dies wird durch Dependency Injection im App.java-Container erreicht.

### 2.3 Autorisierung und Sicherheit

* Token-Validierung: Die AuthService-Klasse ist für die Validierung des Authorization: Bearer <Token>-Headers zuständig.

* Sicherheitskontrolle: Bei POST /api/media stellt der Handler sicher, dass die creatorId im Medieneintrag mit der ID des eingeloggten Benutzers übereinstimmt. Bei PUT/DELETE prüft der MediaService zusätzlich, ob der requesterId (vom Token) der creatorId des bestehenden Eintrags entspricht.

## 3. Curl Tests
   Die beigefügte Datei test_script.sh dient als Integrationstest und demonstriert den erfolgreichen End-to-End-Fluss:

1. Benutzerregistrierung und Login.

2. Speicherung des Tokens in einer Shell-Variable.

3. Verwendung des Tokens für alle nachfolgenden geschützten Aufrufe (CREATE, UPDATE, DELETE).

4. Nachweis der korrekten Autorisierung beim Löschen und Aktualisieren des Eintrags.

## 4. Build & Start
Das Projekt wird in einer Standard-Java-Umgebung kompiliert und gestartet.

1. Kompilieren: Java-Quellcode kompilieren.

2. Starten: Die Hauptklasse App.java ausführen.

3. Zugriff: Der Server ist nach dem Start unter http://localhost:8080 erreichbar.