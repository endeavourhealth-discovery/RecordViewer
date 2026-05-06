<template>

  <div v-if="currentPage !== 'Login'">
    <Sidebar title="Record Viewer" :items="currentSidebarItems" :logoSrc="logo" ref="sidebar" tabindex="0" role="region" aria-label="sidebar"  @pinned="pinSidebar" @summary="updateSummary"/>

    <NavBar @viewModeChanged="handleViewModeChange" @clinicianViewModeChanged="handleClinicianViewModeChange" @toggleSidebar="toggle"
            :headerItems="navbarItems"><RoleSelector @selectedProjectChanged="updateSelectedProject" :selectedProject="selectedProject"></RoleSelector></NavBar>

    <div v-if="UserManager.GetSelectedProject()" :style="{margin: '10px 10px 0 ' + contentOffset + 'px'}" style="min-height:91vh; max-height:200vh">
      <SearchBar ref="searchBar" @nhsNumber="setNHSNumber" @patient="setPatient" :accessMode="accessLevel"/>
      <router-view :selectedPatient="selectedPatient" :accessMode="accessLevel" :viewMode="viewMode" @selectedProjectChanged="updateSelectedProject"/>
    </div>
  </div>

  <Toast position="bottom-right"/>
</template>

<script setup>
import logo from '@/assets/logo.png';
</script>
<script>

import KeycloakService from "@/KeycloakService";
import SearchBar from "@/components/SearchBar.vue";
import Sidebar from "@/components/Sidebar.vue";
import NavBar from "@/components/NavBar.vue";
import RoleSelector from "@/components/RoleSelector.vue";
import AccessControl from "@/API/AccessManager";
import UserManager from "@/API/UserManager/UserManager";
import ApiPopulation from "@/API/ApiPopulation";
import { useAiReviewStore } from '@/stores/aiReview';
import { useEmergencyDataStore } from '@/stores/emergencyData';

export default {
  name: 'App',
  components: {
    SearchBar,
    Sidebar,
    RoleSelector,
    NavBar
  },
  data() {
    return {
      t: true,
      f: false,
      contentOffset: 270,
      visible: false,
      showProfile: false,
      themeChanged: false,
      currentNHSNumber: null,
      selectedPatient: null,
      selectedProject: null,
      accessLevel: 'Viewer',
      viewMode: 'Light',
      clinicianViewMode: 'gp',
      navbarItems: [
        { name: "Help", icon: "fa-regular fa-circle-question",
          command: () => {
            window.open("https://wiki.voror.co.uk/index.php?title=Record_Viewer", "_blank");
          }},
        { name: "Logout", icon: "fa-solid fa-right-from-bracket",
          command: () => {
            this.$router.push("/");
            KeycloakService.CallLogOut();
          }},
      ],
      sidebarItems: [
        {
          label: 'Patient demographics',
          icon: 'fa-solid fa-home-alt',
          command: () => {
            this.$router.push("/");
          }
        },
        {
          label: 'Care record summary',
          icon: 'fa-solid fa-notes-medical',
          command: () => {
            this.$emit('summary');
            this.$router.push("/patient-summary");
          }
        },
        {
          label: 'Conditions',
          icon: 'fa-solid fa-diagnoses',
          command: () => {
            this.$emit("conditions");
            this.$router.push("/condition-view");
          }
        },
        {
          label: 'Medication',
          icon: 'fa-solid fa-pills',
          command: () => {
            this.$emit('medication');
            this.$router.push("/medication-view");
          }
        },
        {
          label: 'Allergies',
          icon: 'fa-solid fa-allergies',
          command: () => {
            this.$emit('allergies');
            this.$router.push("/allergy-view");
          }
        },
        {
          label: 'Warnings/flags',
          icon: 'fa-solid fa-exclamation-triangle',
          command: () => {
            this.$emit('warnings');
            this.$router.push("/warning-and-flag-view");
          }
        },
        {
          label: 'Appointments',
          icon: 'fa-solid fa-calendar-alt',
          command: () => {
            this.$emit('appointments');
            this.$router.push("/appointment-view");
          }
        },
        {
          label: 'Encounters',
          icon: 'fa-solid fa-user-doctor',
          command: () => {
            this.$emit('encounters');
            this.$router.push("/encounter-view");
          }
        },
        {
          label: 'Observations',
          icon: 'fa-solid fa-heart-pulse',
          command: () => {
            this.$emit('observations');
            this.$router.push("/observation-view");
          }
        },
        {
          label: 'Diagnostic orders',
          icon: 'fa-solid fa-microscope',
          command: () => {
            this.$emit("diagnosticOrders");
            this.$router.push("/diagnostic-order-view");
          }
        },
        {
          label: 'Diagnostic reports',
          icon: 'fa-solid fa-x-ray',
          command: () => {
            this.$emit("diagnosticReports");
            this.$router.push("/diagnostic-report-view");
          }
        },
        {
          label: 'Procedure requests',
          icon: 'fa-solid fa-house-medical-flag',
          command: () => {
            this.$emit("procedureRequests");
            this.$router.push("/procedure-request-view");
          }
        },
        {
          label: 'Procedures',
          icon: 'fa-solid fa-procedures',
          command: () => {
            this.$emit("procedures");
            this.$router.push("/procedure-view");
          }
        },
        {
          label: 'Referral requests',
          icon: 'fa-solid fa-hospital-user',
          command: () => {
            this.$emit("referralRequests");
            this.$router.push("/referral-request-view");
          }
        },
        {
          label: 'Family history',
          icon: 'fa-solid fa-user-friends',
          command: () => {
            this.$emit("familyHistory");
            this.$router.push("/family-history-view");
          }
        },
        {
          label: 'Immunisations',
          icon: 'fa-solid fa-syringe',
          command: () => {
            this.$emit("immunisations");
            this.$router.push("/immunisation-view");
          }
        }
      ],
      emergencySidebarItems: [
        {
          label: 'Emergency Dashboard',
          icon: 'fa-solid fa-truck-medical',
          command: () => {
            this.$router.push("/emergency-dashboard");
          }
        },
        {
          label: 'AI Assisted Diagnosis',
          icon: 'fa-solid fa-robot',
          command: () => {
            this.$router.push("/ai-diagnosis");
          }
        },
        {
          label: 'Warnings & Flags',
          icon: 'fa-solid fa-exclamation-triangle',
          command: () => {
            this.$emit('warnings');
            this.$router.push("/emergency-warnings");
          }
        },
        {
          label: 'Recent Encounters',
          icon: 'fa-solid fa-user-doctor',
          command: () => {
            this.$emit('encounters');
            this.$router.push("/emergency-encounters");
          }
        }
      ]
    }
  },
  computed: {
    currentSidebarItems() {
      return this.clinicianViewMode === 'emergency' ? this.emergencySidebarItems : this.sidebarItems;
    },
    UserManager() {
      return UserManager
    },
    currentPage() {
      return this.$router.currentRoute.value.name;
    }
  },
  watch: {
    clinicianViewMode(val) {
      if (val === 'emergency' && this.selectedPatient !== null) {
        this.$router.push('/emergency-dashboard');
      }
    },
    selectedPatient(val) {
      if (this.clinicianViewMode === 'emergency' && val !== null) {
        this.$router.push('/emergency-dashboard');
      }
    }
  },
  methods: {
    toggle(){
      this.$refs.sidebar.OpenMenu();
    },
    pinSidebar(pinned) {
      this.contentOffset = pinned ? 270 : 70;
    },
    setNHSNumber: function(nhsNumber){
      this.currentNHSNumber = nhsNumber;
    },
    setPatient: async function(patient){
      const aiStore = useAiReviewStore();
      const emergencyStore = useEmergencyDataStore();
      
      if (this.selectedPatient?.personId) {
        try {
          await ApiPopulation.clearAiCache(this.selectedPatient.personId);
        } catch (e) {
          console.warn('Failed to clear AI cache for previous patient:', e.message);
        }
      }
      
      aiStore.clear();
      emergencyStore.clear();
      
      this.selectedPatient = patient;
    },
    updateSummary() {
      if(this.currentNHSNumber !== null) {
        this.$router.push({ path: 'patient-summary', query: { nhsNum: this.currentNHSNumber }});
      } else {
        this.$router.push('patient-summary');
      }
    },
    updateContentOffset() {
      const screenWidth = window.innerWidth;
      if (screenWidth <= 768) {
        this.contentOffset = 0;
      } else {
        this.contentOffset = this.$refs.sidebar && this.$refs.sidebar.pinned ? 270 : 70;
      }
    },
    async updateSelectedProject(){
      this.selectedProject = await UserManager.GetSelectedProject()
      this.accessLevel = await AccessControl.GetCurrentAccessLevel();
      if(this.$route.fullPath !== AccessControl.getUrl())
        this.$router.push(AccessControl.getCleanUrl())
      else {
        this.$router.push({
          path: this.$route.fullPath,
          query: { reload: Date.now() }
        });
      }
    },
    handleViewModeChange(val) {
      this.viewMode = val;
      const themeLink = document.getElementById('theme-link');
      if (themeLink) {
        if (val === 'Dark') {
          themeLink.href = '/themes/aura-dark-blue/theme.css';
        } else {
          themeLink.href = '/themes/aura-light-blue/theme.css';
        }
      }
    },
    handleClinicianViewModeChange(val) {
      const newMode = val.toLowerCase();
      if (newMode !== this.clinicianViewMode && this.selectedPatient !== null) {
        this.$refs.searchBar.clearPatient();
        this.currentNHSNumber = null;
        this.selectedPatient = null;
      }
      if (newMode === 'gp' && newMode !== this.clinicianViewMode) {
        this.$router.push('/');
      }
      this.clinicianViewMode = newMode;
    },
    handleResize() {
      this.updateContentOffset();
    }
  },
  async mounted() {
    window.addEventListener('resize', this.handleResize);
    this.updateContentOffset(); // Set initial offset based on the initial screen width
    await this.updateSelectedProject();
    this.accessLevel = await AccessControl.GetCurrentAccessLevel();
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
  }
}
</script>

<style scoped>

html {
  background-color: var(--surface-100);
}


:deep(.p-datatable-row-expansion){
  background-color: var(--surface-100);
}

p-sidebar-mask {
  width: 0px;
  height: 0px;
}

:deep(p-menu p-menuitem p-menu-list p-menuitem-content) {
  background-color: var(--teal-500);
}
</style>

