# databroker-lrza-webservice

Een Spring Boot webservice die de LRZa (Landelijk Register Zorgaanbieders) functionaliteit implementeert voor de generieke functie Adressering binnen de Nederlandse zorg.

De implementatie is gebaseerd op de [Generieke Functies Care Services](https://build.fhir.org/ig/minvws/generiekefuncties-docs/branches/0.9.0-ballot/care-services.html)

---

## Inhoudsopgave

- [Functionaliteit](#functionaliteit)
- [Architectuur](#architectuur)
- [Tech Stack](#tech-stack)
- [Vereisten](#vereisten)
- [Lokaal draaien](#lokaal-draaien)
- [Configuratie](#configuratie)
- [API Overzicht](#api-overzicht)
- [Testen met Bruno](#testen-met-bruno)
- [Docker](#docker)
- [Trajecten en koppelingen](#trajecten-en-koppelingen)

---

## Functionaliteit

De webservice biedt twee hoofdfunctionaliteiten:

### Administratie (Publiceren naar LRZa)
Registreer en beheer FHIR-resources in het centrale LRZa:
- Aanmaken van **Zorgaanbieder** (Organization met URA-identifier)
- Aanmaken van **IT-leverancier** (Organization met KvK-nummer)
- Aanmaken van een **OrganizationAffiliation** (koppeling tussen leverancier en zorgaanbieder)
- Aanmaken van een **Applicatie/Device** (met applicatie-identifier)
- Aanmaken van een **Endpoint** (adresseringsendpoint met connectionType en payloadType)
- Aanmaken van een **HealthcareService** (zorgdienst met type en specialisatie)
- Koppelen van een **Endpoint aan een Organization**
- Koppelen van een **Endpoint aan een HealthcareService**

### Synchronisatie (Lokale Replica bijhouden)
Houd een lokale FHIR-replica up-to-date vanuit het centrale LRZa:
- **Initiële synchronisatie**: kopieer alle FHIR-resources (Endpoint, Location, HealthcareService, Organization, OrganizationAffiliation, PractitionerRole, Practitioner) naar de lokale replica
- **Periodieke synchronisatie**: haal wijzigingen op die hebben plaatsgevonden na een opgegeven tijdstip en verwerk upserts en deletes

---

## Architectuur

```
┌──────────────────────────────────────────────────────────────┐
│                   lrza-webservice (:8080)                    │
│                                                              │
│  AdministrationApi ──► OrganizationService                   │
│                   ──► EndpointService                        │
│                   ──► HealthcareServiceService               │
│                   ──► DeviceService                          │
│                                                              │
│  SynchronizationApi ──► InitialSyncService   ◄──┐           │
│                    ──► PeriodicSyncService   ◄──┤           │
└──────────────────────────────────────────────────────────────┘
         │  mTLS (UZI-certificaat)              │
         ▼                                      │
┌──────────────────┐               ┌────────────────────────┐
│  Centraal LRZa   │               │  Lokale Replica (:7050) │
│  (FHIR R4)       │               │  HAPI FHIR (FHIR R4)   │
└──────────────────┘               └────────────────────────┘
```

**Ondersteunende tools (voor lokaal testen):**

| Service              | Poort | Beschrijving                              |
|----------------------|-------|-------------------------------------------|
| Lokale Replica       | 7050  | Lokale HAPI FHIR R4 server                |
| Mock Central LRZa    | 7051  | Mock voor het centrale LRZa (FHIR R4)     |

---

## Tech Stack

| Technologie      | Versie   |
|------------------|----------|
| Kotlin           | 2.x      |
| Java             | 21       |
| Spring Boot      | 3.5.x    |
| HAPI FHIR        | 8.x      |
| Gradle           | 8.x      |
| Docker           | –        |
| OpenAPI Generator | 7.x     |

---

## Vereisten

- JDK 21
- Docker & Docker Compose
- [Bruno](https://www.usebruno.com/) (voor het uitvoeren van de API-collectie)

---

## Lokaal draaien

### 1. Start de ondersteunende services

Start de lokale FHIR-server (lokale replica):

```bash
docker compose -f tools/local-replica/docker-compose.yml up -d
```

Optioneel – start de mock van het centrale LRZa (als je niet met de echte proeftuin wilt werken):

```bash
docker compose -f tools/mock-central-lrza/docker-compose.yml up -d
```

### 2. Bouw en start de applicatie

Gebruik hiervoor IntelliJ of een andere IDE die Gradle ondersteunt.

## Configuratie

De configuratie bevindt zich in `lrza-webservice/src/main/resources/application.yaml`:

```yaml
application:
  local-replica:
    base-url: http://localhost:7050/fhir   # Lokale HAPI FHIR server
  lrza:
    base-url: https://adressering.proeftuin.gf.irealisatie.nl/poc/FHIR/fhir  # Centraal LRZa (proeftuin)
  mtls:
    keystore-file: classpath:mtls/your-mtls-certificate.pfx
    keystore-password: <wachtwoord>
```

> **Let op:** De webservice gebruikt **mTLS** met een UZI-certificaat voor communicatie met het centrale LRZa. Zorg dat het juiste `.pfx`-bestand aanwezig is onder `lrza-webservice/src/main/resources/mtls/`.

---

## API Overzicht

Alle endpoints draaien op poort `8080`.

### Administratie

| Methode | Pad                        | Beschrijving                                    |
|---------|----------------------------|-------------------------------------------------|
| POST    | `/Organization`            | Maak een Zorgaanbieder aan (URA-identifier)     |
| POST    | `/Organization/create-it-vendor` | Maak een IT-leverancier aan (KvK-nummer)  |
| POST    | `/OrganizationAffiliation` | Maak een koppeling vendor ↔ zorgaanbieder       |
| POST    | `/Application`             | Maak een Applicatie/Device aan                  |
| POST    | `/Endpoint`                | Maak een adresseringsendpoint aan               |
| POST    | `/HealthcareService`       | Maak een zorgdienst aan                         |
| PUT     | `/Organization`            | Koppel een Endpoint aan een Organization        |
| PUT     | `/HealthcareService`       | Koppel een Endpoint aan een HealthcareService   |

### Synchronisatie

| Methode | Pad                            | Beschrijving                                              |
|---------|--------------------------------|-----------------------------------------------------------|
| GET     | `/initial-sync`                | Voer een volledige initiële synchronisatie uit            |
| GET     | `/periodic-sync?since=<ISO8601>` | Synchroniseer wijzigingen sinds het opgegeven tijdstip  |

**Voorbeeld `since`-parameter:** `2026-05-01T00:00:00+02:00`

---

## Testen met Bruno

De API kan getest worden met [Bruno](https://www.usebruno.com/), een open-source API-client.

### Vereisten

- Installeer Bruno: https://www.usebruno.com/downloads

### Collectie openen

1. Open Bruno
2. Klik op **Open Collection**
3. Navigeer naar de map `.bruno/databroker-lrza-webservice/` in deze repository
4. De collectie bevat requests voor alle bovenstaande endpoints

### Aanbevolen testvolgorde (Administration)

Volg de onderstaande volgorde wanneer je voor het eerst resources aanmaakt, omdat latere stappen afhankelijk zijn van eerder aangemaakte IDs:

1. **POST** `/Organization` → sla de teruggegeven `id` op als `zorgaanbiederId`
2. **POST** `/Organization/create-it-vendor` → sla de teruggegeven `id` op als `vendorId`
3. **POST** `/OrganizationAffiliation` → gebruik `vendorId` en `zorgaanbiederId`
4. **POST** `/Application` → gebruik `vendorId`
5. **POST** `/Endpoint` → gebruik `vendorId`; sla de teruggegeven `id` op als `endpointId`
6. **POST** `/HealthcareService` → gebruik `zorgaanbiederId`; sla de teruggegeven `id` op als `serviceId`
7. **PUT** `/Organization` → koppel `endpointId` aan `zorgaanbiederId`
8. **PUT** `/HealthcareService` → koppel `endpointId` aan `serviceId`

### Synchronisatie testen

- **GET** `/initial-sync` – kopieer alle resources van het (centrale/mock) LRZa naar de lokale replica
- **GET** `/periodic-sync?since=2026-01-01T00:00:00Z` – synchroniseer wijzigingen

> **Tip:** Stel in Bruno een omgevingsvariabele `baseUrl` in op `http://localhost:8080` zodat alle requests automatisch de juiste poort gebruiken.

---

## Laatst bijgewerkt

15 mei 2026
