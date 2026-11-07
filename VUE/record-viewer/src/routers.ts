import { createRouter, createWebHashHistory } from "vue-router";
import PatientSearch from "@/PatientViewer/Components/patient/PatientDemographics.vue";
//import PatientView from "@/PatientViewer/Components/patient/PatientView.vue";
import MedicationView from "@/PatientViewer/Components/medication/MedicationView.vue";
import AppointmentView from "@/PatientViewer/Components/appointments/AppointmentView.vue";
import AllergyView from "@/PatientViewer/Components/allergies/AllergyView.vue";
import DiagnosticReportView from "@/PatientViewer/Components/diagnostics/DiagnosticReportView.vue";
import PatientSummaryScreen from "@/PatientViewer/Components/patient/PatientSummaryScreen.vue";
import EncounterView from "@/PatientViewer/Components/encounters/EncounterView.vue";
import WarningView from "@/PatientViewer/Components/warnings/WarningView.vue";
import ConditionView from "@/PatientViewer/Components/conditions/ConditionView.vue";
import ObservationView from "@/PatientViewer/Components/observations/ObservationView.vue";
import DiagnosticOrderView from "@/PatientViewer/Components/diagnostics/DiagnosticOrderView.vue";
import ProcedureView from "@/PatientViewer/Components/procedures/ProcedureView.vue";
import ProcedureRequestView from "@/PatientViewer/Components/procedures/ProcedureRequestView.vue";
import ReferralRequestView from "@/PatientViewer/Components/referrals/ReferralRequestView.vue";
import FamilyHistoryView from "@/PatientViewer/Components/family/FamilyHistoryView.vue";
import ImmunisationView from "@/PatientViewer/Components/immunisations/ImmunisationView.vue";
import PatientDemographics from "@/PatientViewer/Components/patient/PatientDemographics.vue";
import FrontPage from "@/PatientViewer/Components/FrontPage.vue";
import AccessControl from "@/API/AccessManager";
import AllowAccess from "@/components/AllowAccess.vue";
import EmergencyDashboard from "@/PatientViewer/Components/emergency/EmergencyDashboard.vue";
import AiDiagnosisView from "@/PatientViewer/Components/emergency/AiDiagnosisView.vue";
import EmergencyWarningsView from "@/PatientViewer/Components/emergency/EmergencyWarningsView.vue";
import EmergencyEncountersView from "@/PatientViewer/Components/emergency/EmergencyEncountersView.vue";
import AiPatientSummaryView from "@/PatientViewer/Components/patient/AiPatientSummaryView.vue";
import AiAdminView from "@/PatientViewer/Components/admin/AiAdminView.vue";

const routes = [
  {
    name: "Unauthorised",
    component: AllowAccess,
    path: "/unauthorised",
    accessRoles: [],
  },
  {
    name: "PatientDemographics",
    path: "/",
    component: PatientDemographics,
    props: true
  },
  {
    path: "/patient-demographics/",
    redirect: "/:nhsNum?",
  },
  // {
  //   name: "PatientSummaryScreen",
  //   component: PatientSummaryScreen,
  //   path: "/patient-summary/:nhsNum",
  //   props: true,
  // },
  {
    name: "PatientSummaryScreen",
    component: PatientSummaryScreen,
    path: "/patient-summary",
    props: true
  },
  {
    name: "MedicationView",
    component: MedicationView,
    path: "/medication-view",
    props: true
  },
  {
    name: "AppointmentView",
    component: AppointmentView,
    path: "/appointment-view",
    props: true
  },
  {
    name: "AllergyView",
    component: AllergyView,
    path: "/allergy-view",
    props: true
  },
  {
    name: "DiagnosticReportView",
    component: DiagnosticReportView,
    path: "/diagnostic-report-view",
    props: true
  },
  {
    name: "EncounterView",
    component: EncounterView,
    path: "/encounter-view",
    props: true
  },
  {
    name: "WarningView",
    component: WarningView,
    path: "/warning-and-flag-view",
    props: true
  },
  {
    name: "ConditionView",
    component: ConditionView,
    path: "/condition-view",
    props: true
  },
  {
    name: "ObservationView",
    component: ObservationView,
    path: "/observation-view",
    props: true
  },
  {
    name: "DiagnosticOrderView",
    component: DiagnosticOrderView,
    path: "/diagnostic-order-view",
    props: true
  },
  {
    name: "ProcedureView",
    component: ProcedureView,
    path: "/procedure-view",
    props: true
  },
  {
    name: "ProcedureRequestView",
    component: ProcedureRequestView,
    path: "/procedure-request-view",
    props: true
  },
  {
    name: "ReferralRequestView",
    component: ReferralRequestView,
    path: "/referral-request-view",
    props: true
  },
  {
    name: "FamilyHistoryView",
    component: FamilyHistoryView,
    path: "/family-history-view",
    props: true
  },
  {
    name: "ImmunisationView",
    component: ImmunisationView,
    path: "/immunisation-view",
    props: true
  },
  {
    name: "EmergencyDashboard",
    component: EmergencyDashboard,
    path: "/emergency-dashboard"
  },
  {
    name: "AiDiagnosis",
    component: AiDiagnosisView,
    path: "/ai-diagnosis"
  },
  {
    name: "EmergencyWarnings",
    component: EmergencyWarningsView,
    path: "/emergency-warnings"
  },
  {
    name: "EmergencyEncounters",
    component: EmergencyEncountersView,
    path: "/emergency-encounters"
  },
  {
    name: "AiPatientSummary",
    component: AiPatientSummaryView,
    path: "/ai-patient-summary",
    props: true
  },
  {
    name: "AiAdmin",
    component: AiAdminView,
    path: "/ai-admin",
    props: true
  }
];
const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach(async (to, from, next) => {
  try {
    // Case 1: user tries to access /unauthorised
    if (to.path === "/unauthorised") {
      const hasRVAccess = await AccessControl.CheckRoleAccess("record-viewer");

      if (hasRVAccess) {
        return next({ name: "PatientDemographics" });
      }

      return next(); // allow access to /unauthorised
    }

    //to use this app the user must have 'record-viewer' level access.
    const allowed = await AccessControl.isAccessAllowed(["record-viewer"], to);
    if (allowed) {
      next();
    } else {
      next({ path: "/unauthorised" });
    }
  } catch {
    next({ path: "/unauthorised" });
  }
});
export default router;
