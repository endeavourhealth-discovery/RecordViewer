# record-viewer

## Developer Setup

### Backend Configuration
To run this project, you need a local backend environment set up:
- **Tomcat Server**: Start a local Tomcat server (expected on port 8080).
- **Database**: You need a local MySQL database, which should have been configured during onboarding.
    - This local DB contains a config table, with entries relating to 'record-viewer' which you should configure to be pointing to the **separate development database**.
    - The application uses this remote development database for testing and development.

### Frontend Setup
- **Node.js**: Ensure Node.js is installed on your system, this project is configured to run on V24+.
- **npm**: Install npm (Node Package Manager) if it's not already installed.

```
npm install
```

### Development
```
npm run dev
```

### Production
```
npm run build
```

### Unit Tests
```
npm run test:unit
```

### Lints and Fixes
```
npm run lint
```
