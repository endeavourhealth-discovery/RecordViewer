# RecordViewer - Agent Development Guide

## Project Overview

RecordViewer is a healthcare patient record viewer application for Endeavour Health (NHS data service). It provides clinicians with a unified view of patient clinical data including demographics, medications, conditions, allergies, encounters, diagnostics, and more.

### Architecture

| Component | Location | Tech Stack | Status |
|-----------|----------|------------|--------|
| **Vue Frontend** | `VUE/record-viewer/` | Vue 3, PrimeVue 3.51, Vite 6, Keycloak-JS v24 | Active |
| **Java Backend API** | `API/` | JAX-RS (Jersey), Jackson, WAR packaging on Tomcat | Active |
| **Common Module** | `common/` | JDBC DAL with connection pooling, shared DTOs | Active |
| **Angular Frontend** | `FrontEndv8/` | Angular 9, Material, ngx-charts, Keycloak-Angular | Legacy (retiring) |

### Key Features

- Patient search and demographics display
- Clinical data views: medications, allergies, conditions, encounters, diagnostics
- Emergency Clinician View mode with prioritized critical data
- AI-powered record review with summary generation and conversational Q&A
- GP/Emergency view toggle in profile menu
- Theme switching (Light/Dark)

---

## Build Instructions

### Prerequisites

- **Java 17** (or Corretto 8 for CI/CD builds)
- **Maven 3.9+**
- **Node.js 18+** and npm
- **Apache Tomcat 9.x or 10.x** (for local testing)

### Backend Build (Java API + Common Modules)

```powershell
cd D:\OpenCode\RecordViewer-master
mvn clean install -DskipTests
```

Output: `API/target/api.war`

### Frontend Build (Vue App)

```powershell
cd VUE/record-viewer
npm install  # Only needed once or when dependencies change
npm run build
```

Output: `dist/` folder with static files

---

## Local Development Setup

### Backend (Tomcat)

1. Install Apache Tomcat 9.x
2. Copy WAR to webapps:
   ```powershell
   copy API\target\api.war "C:\path\to\tomcat\webapps\"
   ```
3. Start Tomcat:
   ```powershell
   cd C:\path\to\tomcat\bin
   .\startup.bat
   ```

### Frontend (Vite Dev Server)

```powershell
cd VUE/record-viewer
npm run dev
```

The frontend proxies API requests to `https://localhost:8443` via Vite config.

---

## Deployment

### Via CI/CD Pipeline (AWS CodeBuild + CodeDeploy)

Push changes to the repository and AWS CodeBuild will automatically build and deploy:

1. **Pre-build**: Downloads Maven settings from S3
2. **Build**: Runs `mvn -B package` (compiles Java backend + Vue frontend)
3. **Post-build**: Packages artifacts into zip files for deployment

Artifacts deployed to:
- Backend: `/opt/tomcat/webapps/record-viewer.war`
- Frontend: `/usr/share/nginx/html/record-viewer/`

### Manual Deployment

1. Build both frontend and backend (see above)
2. Transfer files to live server via SCP or similar
3. Deploy backend WAR to Tomcat webapps directory
4. Extract frontend dist files to Nginx HTML directory
5. Restart Tomcat: `sudo systemctl restart tomcat`

---

## Configuration

### AI Review Configuration (`record-viewer-ai`)

The AI review feature requires configuration via one of these methods (in priority order):

1. **ConfigManager** (preferred for production) - Add `"record-viewer-ai"` section to Endeavour's centralized config service
2. **Environment Variables**:
   ```powershell
   $env:AI_LLM_API_URL="http://ai.voror.co.uk:1234/v1/chat/completions"
   $env:AI_LLM_API_KEY="<your-api-key>"
   $env:AI_MODEL_NAME="gpt-4-turbo"
   ```
3. **Defaults** (if nothing configured): `http://ai.voror.co.uk:1234/v1/chat/completions` with model `gpt-4-turbo`

### Keycloak Configuration

Located at `VUE/record-viewer/public/keycloak.json`:
```json
{
  "realm": "endeavour",
  "auth-server-url": "https://demo.discoverydataservice.net:8424/auth",
  "resource": "eds-ui",
  "public-client": true
}
```

---

## Key Code Locations

### Frontend (Vue)

| File | Purpose |
|------|---------|
| `VUE/record-viewer/src/App.vue` | Main app component, view mode state management |
| `VUE/record-viewer/src/components/NavBar.vue` | Header with profile menu and theme/view toggles |
| `VUE/record-viewer/src/routers.ts` | Route definitions for all views |
| `VUE/record-viewer/src/API/ApiPopulation.ts` | API client functions for data fetching |
| `VUE/record-viewer/src/PatientViewer/Components/emergency/` | Emergency view components |

### Backend (Java)

| File | Purpose |
|------|---------|
| `API/src/main/java/org/endeavourhealth/recordviewer/api/endpoints/CareRecordEndpoint.java` | Main REST API endpoints |
| `API/src/main/java/org/endeavourhealth/recordviewer/api/endpoints/AiReviewEndpoint.java` | AI review endpoints (`/events/VUE/ai-review/*`) |
| `common/src/main/java/org/endeavourhealth/recordviewer/common/dal/VueRecordViewerJDBCDAL.java` | Vue-specific database access layer |
| `common/src/main/java/org/endeavourhealth/recordviewer/common/dal/AiRecordAggregator.java` | Aggregates patient data for AI review with caching |
| `common/src/main/java/org/endeavourhealth/recordviewer/common/dal/AiAuditLogger.java` | Audit logging for AI queries/responses |

---

## Emergency Clinician View

### Overview

The Emergency Clinician View provides a reorganized interface optimized for emergency settings, focusing on critical data like current medications, allergies, active conditions, and warnings. It includes an AI-powered record review feature.

### Key Components

1. **GP/Emergency Toggle** - Located in profile menu (user icon, top-right)
2. **Emergency Dashboard** - At-a-glance view with patient summary, critical alerts, medications, allergies, active problems, and warnings
3. **AI Record Review** - Generates comprehensive AI analysis of the complete patient record

### Emergency Dashboard Layout

- **Patient Summary**: Name, DOB, Age, Gender, NHS Number, Height, Weight, BMI (auto-calculated)
- **Critical Alerts**: Allergies, warnings, polypharmacy flags displayed as colored badges
- **Left Column Accordions**: Active conditions/problems, Allergies/intolerances, Warnings/flags
- **Right Column Accordions**: Current medication, AI Record Review section

### AI Review Features

- **Summary Generation**: Comprehensive analysis of complete patient record
- **Conversational Q&A**: Ask follow-up questions about the patient's record
- **Emergency Flags**: Highlights critical findings (allergies, warnings, drug interactions)
- **Caching**: 30-minute TTL cache for aggregated patient data (configurable via `cache_ttl_minutes`)

---

## Troubleshooting

### Common Issues

1. **Icons not displaying** - Font Awesome is bundled locally in `dist/assets/`. Ensure no CDN references remain.
2. **Theme toggle not working** - Check that `handleViewModeChange` updates the CSS link href correctly.
3. **OverlayPanel not appearing** - Use imperative API (`$refs.op.toggle()`) instead of reactive binding for PrimeVue 3.
4. **AI summary generation fails with NullPointerException** - Ensure `projectID` is passed as a request header, not just in JSON body.

### Debugging Tips

- Check browser console for JavaScript errors
- Verify Tomcat logs at `C:\tomcat\logs\catalina.out` (Windows) or `/opt/tomcat/logs/` (Linux)
- Use browser DevTools Network tab to verify API calls and font file loading
- Test AI endpoints directly with curl before testing in UI

---

## Recent Changes

### Emergency Clinician View + AI Review (Current Session)

1. Added `AiReviewEndpoint.java` with three REST endpoints: `/generate-summary`, `/chat`, `/cache/clear`
2. Created `AiRecordAggregator.java` for data aggregation and caching
3. Implemented `AiAuditLogger.java` for audit logging to file
4. Restructured Emergency Dashboard with Accordion-based layout matching PatientSummaryScreen
5. Fixed NavBar OverlayPanel visibility using imperative API
6. Added environment variable fallback for AI configuration
7. Fixed theme switching by updating CSS link href dynamically

---

## Contributing Guidelines

- Follow existing code patterns and naming conventions
- Use explicit imports instead of wildcards to avoid type conflicts (e.g., `javax.ws.rs.core.MediaType` vs `okhttp3.MediaType`)
- Test changes locally before committing
- Ensure both frontend and backend build successfully:
  ```powershell
  mvn clean install -DskipTests && cd VUE/record-viewer && npm run build
  ```
