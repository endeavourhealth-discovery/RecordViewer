<template>
    <Card v-if="accessMode === 'record-viewer'" aria-label="Search-Bar" style="margin:10px 0 10px 0; padding: 0px;">
      <template #content>
        <div class="searchbarData" style="display:flex; justify-content: center; align-items: center; padding: 0 5px 0 5px" v-show="expanded">
          <div v-if="selectedPatient!=null" style="display: grid;
            grid-template-columns: repeat(4, 1fr);
            grid-auto-rows: auto;
            gap: 10px;
            margin: 0;
            padding: 0">
            <p style="text-align:left; margin: 0;padding: 0"><b>{{selectedPatient.name}}</b></p>
            <p style="text-align:left; margin: 0;padding: 0">Gender: <b> {{selectedPatient.gender}}</b></p>
            <p style="text-align:left; margin: 0;padding: 0">NHS: <b> {{selectedPatient.nhsNumber}}</b></p>
            <p style="text-align:left; margin: 0;padding: 0">DOB: <b> {{selectedPatient.dob}}</b></p>
            <p style="text-align:left; margin: 0;padding: 0">{{selectedPatient.address}}</p>
            <p style="text-align:left; margin: 0;padding: 0">GP: <b> {{selectedPatient.usual_gp}}</b></p>
            <p style="text-align:left; margin: 0;padding: 0">Organisation: <b> {{selectedPatient.organisation}}</b></p>
            <p style="text-align:left; margin: 0;padding: 0">Registration status: <b> {{selectedPatient.registration}}</b></p>
          </div>
          <div style="display:flex; gap:5px; flex-wrap:wrap">
          <Button type="button" @click="openSearchDialog" :aria-controls="toggleOverlay ? 'dlg' : null" :aria-expanded="toggleOverlay ? true : false" enter style="margin: 0; padding:6px">Find patient</Button>
          <Button type="button" v-show="selectedPatient!=null" @click="clearPatient" severity="danger" style="padding:6px 2px 6px 2px">Clear patient</Button>
          </div>
        </div>
      </template>
    </Card>
  <Dialog header="Patient Search" role="dialog" id="dlg" v-model:visible="toggleOverlay" @afterHide="resetDialog" modal :style="{width: '80%', height: '74vh'}">

      <div class="dialogHeader">
        <div class="search-field">
          <i class="fa-solid fa-user" style="padding:14px 8px 14px 0; font-size:16px"></i>
          <InputText id="patientName" v-tooltip="nameTooltip" v-model="name" @keydown.enter="searchForPatients()" placeholder="Name" />
        </div>
        <div class="search-field">
          <i class="fa-solid fa-calendar-days" style="padding:14px 8px 14px 0; font-size:16px"></i>
          <InputText id="dob" v-tooltip="dobTooltip" v-model="dob" @keydown.enter="searchForPatients()" placeholder="Date of birth" autocomplete="off" />
        </div>
        <div class="search-field">
          <i class="fa-solid fa-id-card" style="padding:14px 8px 14px 0; font-size:16px"></i>
          <InputText id="NHSNumber" v-tooltip="nhsTooltip" v-model="nhsNumber" @keydown.enter="searchForPatients()" placeholder="NHS number" />
        </div>
        <ProgressSpinner v-show="searchingForPatient" style="width:42px; height:42px" strokeWidth="5"></ProgressSpinner>
      </div>
    <div style="height:45vh">
      <DataTable v-model:expandedRows="expandedPatients" v-model:selection="selectedPatientTemp" paginator :rows="10" @row-click="selectRowFromTable" dataKey="id" :value="tablePatients">
        <Column field="name" header="Name"></Column>
        <Column field="address" header="Address"></Column>
        <Column field="dob" header="DOB"></Column>
        <Column field="gender" header="Gender"></Column>
        <Column field="nhsNumber" header="NHS number"></Column>
        <Column expander />
        <template #expansion="slotProps">
          <div style="display:flex">
            <div style="display:inline; padding-right:10%">
              <p>ID: {{slotProps.data.id}}</p>
              <p>Mobile: {{slotProps.data.mobile}}</p>
            </div>
            <div style="display:inline; padding-right:10%">
              <p>GP: {{slotProps.data.usual_gp}}</p>
              <p>Organisation: {{slotProps.data.organisation}}</p>
            </div>
            <div style="display:inline; padding-right:10%">
              <p>Registration status: {{slotProps.data.registration}}</p>
              <p>Registration date: {{slotProps.data.start_date}}</p>
            </div>
          </div>
        </template>
      </DataTable>
    </div>
    <template #footer>
        <Button v-on:click="searchForPatients()" type="submit">Search</Button>
    </template>
  </Dialog>
</template>

<script>
import ApiPopulation from "@/API/ApiPopulation";

export default {
  name: "SearchBar",
  props: {
    accessMode: {default: "Viewer"}
  },
  data() {
    return {
      name: "",
      dob: "",
      nhsNumber: "",
      nameTooltip: "Format 'first_name last_name' for example: 'john jones'",
      dobTooltip: "Format 'DD/MM/YYYY' or 'DD-MM-YYYY', for example: '01/01/1990'",
      nhsTooltip: "Enter a valid 10 digit NHS number",
      toggleOverlay: false,
      expandedPatients: null,
      searchingForPatient: false,
      expanded: true,
      expandIcon: "fa-solid fa-chevron-up",
      patients: [],
      tablePatients: [],
      selectedPatient: null,
      selectedPatientTemp: null
    };
  },
  methods: {
    expandBar(){
      this.expanded = !this.expanded;
      if(this.expanded) {
        this.expandIcon = "fa-solid fa-chevron-up";
      } else {
        this.expandIcon = "fa-solid fa-chevron-down";
      }
    },
    openSearchDialog(){
      this.toggleOverlay = true;
      setTimeout(() => {
        const nameInput = document.getElementById('patientName');
        if (nameInput) {
          nameInput.focus();
        }
      }, 300);
    },
    selectRowFromTable(event) {
      this.clearPatient();
      this.selectedPatient = event.data;
      this.toggleOverlay = false;
      this.cachePatient();
    },
    cachePatient(){
      this.selectedPatientTemp = null;
      Object.assign(this.selectedPatient, {
        additionals: {
          concept: [],
          textValue: []
        }
      })
      for(let i = 0; i < this.patients.length; i++) {
        if(this.selectedPatient.id == this.patients[i].id) {
          if(this.patients[i].patientAdditionalConcept != null)
            this.selectedPatient.additionals.concept.push(this.patients[i].patientAdditionalConcept);
          if(this.patients[i].patientAdditionalTextValue != null)
            this.selectedPatient.additionals.textValue.push(this.patients[i].patientAdditionalTextValue);
        }
      }
      this.$emit("nhsNumber", this.selectedPatient.nhsNumber);
      this.$emit("patient", this.selectedPatient);
    },
    async findPatientWithNHSNumber(){
      this.patients = await ApiPopulation.searchForPatient(this.nhsNumber);
      if(Array.isArray(this.patients)) {
        this.selectedPatient = [...this.patients][0];
      } else {
        this.selectedPatient = this.patients;
      }
      Object.assign(this.selectedPatient, {
        additionals: {
          concept: [],
          textValue: []
        }
      })

      if(Array.isArray(this.patients)) {
        for (let i = 0; i < this.patients.length; i++) {
          if (this.selectedPatient.id == this.patients[i].id) {
            if (this.patients[i].patientAdditionalConcept != null)
              this.selectedPatient.additionals.concept.push(this.patients[i].patientAdditionalConcept);
            if (this.patients[i].patientAdditionalTextValue != null)
              this.selectedPatient.additionals.textValue.push(this.patients[i].patientAdditionalTextValue);
          }
        }
      } else {
        if (this.patients.patientAdditionalConcept != null)
          this.selectedPatient.additionals.concept.push(this.patients.patientAdditionalConcept);
        if (this.patients.patientAdditionalTextValue != null)
          this.selectedPatient.additionals.textValue.push(this.patients.patientAdditionalTextValue);
      }
      this.$emit("nhsNumber", this.selectedPatient.nhsNumber);
      this.$emit("patient", this.selectedPatient);
    },
    clearPatient(){
      const query = { ...this.$route.query }; // Get current query parameters
      delete query.nhsNum; // Remove the specific parameter
      this.$router.replace({ query });
      this.selectedPatient = null;
      this.tablePatients = [];
      this.expandedPatients = {};
      this.$emit("nhsNumber", null);
      this.$emit("patient", null);
    },
    resetDialog(){
      this.name="";
      this.dob="";
      this.nhsNumber="";
      this.tablePatients = [];
      this.expandedPatients = {};
    },
    normalizeDOB(raw) {
      if (raw instanceof Date && !isNaN(raw.getTime())) return raw;
      const str = String(raw).trim();
      if (!str) return null;
      const normalized = str.replace(/-/g, '/');
      const parts = normalized.split('/');
      if (parts.length !== 3) return null;
      const day = parseInt(parts[0], 10);
      const month = parseInt(parts[1], 10);
      let year = parseInt(parts[2], 10);
      if (isNaN(day) || isNaN(month) || isNaN(year)) return null;
      if (year < 100 && parts[2].length === 2) {
        year += year >= 50 ? 1900 : 2000;
      }
      const date = new Date(year, month - 1, day);
      if (isNaN(date.getTime())) return null;
      // Verify the parsed components match to catch invalid dates like 31/02/2024
      if (date.getDate() !== day || date.getMonth() !== month - 1 || date.getFullYear() !== year) {
        return null;
      }
      return date;
    },
    async searchForPatients() {
      if(this.name || this.dob || this.nhsNumber) {
        if(this.nhsNumber){
          if(this.nhsNumber.length != 10){
            this.$toast.add({
              severity: 'warn',
              summary: 'Invalid NHS Number',
              life: 8000,
              detail: 'Please enter a valid, 10 digit NHS number'
            });
            return;
          }
        }
        let date = '';
        if(this.dob){
          const normalizedDate = this.normalizeDOB(this.dob);
          if (!normalizedDate) {
            this.$toast.add({
              severity: 'warn',
              summary: 'Invalid DOB',
              life: 8000,
              detail: 'Please enter a valid date of birth (dd/mm/yyyy or dd-mm-yyyy)'
            });
            return;
          }
          const offset = normalizedDate.getTimezoneOffset()
          date = new Date(normalizedDate.getTime() - (offset*60*1000))
          date = date.toISOString().split('T')[0]
        }
        this.searchingForPatient = true;
        this.patients = await ApiPopulation.searchForPatients(this.name, this.nhsNumber, date, this.page);
        this.tablePatients = [...this.patients];
        for(let i = 0; i < this.patients.length; i++) {
          for (let j = i; j < this.tablePatients.length; j++) {
            if(i != j){
              if(this.tablePatients[i].id === this.tablePatients[j].id)
                this.tablePatients.splice(j, j+1);
            }
          }
        }
        for (let pat of this.tablePatients) {
          pat.patientName = "";
          for (let name of pat["name"]) {
            Object.keys(name).forEach(function (k) {
              pat.patientName += name[k] + " ";
            });
          }
        }
      } else {
        this.$toast.add({
          severity: 'warn',
          summary: 'Empty search',
          life: 8000,
          detail: 'To search for a patient, please enter a name, date of birth or valid NHS number'
        });
      }
      this.searchingForPatient = false;
    }
  },
  watch: {
    '$route.query.nhsNum': {
      immediate: true,
      handler(newNhsNum) {
        if (newNhsNum) {
          console.log("Found NHS Num in URL:", newNhsNum);
          this.nhsNumber = newNhsNum;
          this.findPatientWithNHSNumber();
        }
      }
    }
  },
};
</script>

<style scoped>
.p-card-content{
  padding:0px;
  margin:0px;
}

.dialogHeader{
  display:flex;
}

.search-field {
  display: flex;
  align-items: center;
  flex: 1;
  margin-right: 5px;
}

.search-field :deep(.p-inputtext) {
  width: 100%;
}

:deep(.p-datatable .p-selectable-row),
:deep(.p-datatable tbody tr) {
  cursor: pointer !important;
}

:deep(.p-datatable .p-selectable-row:hover),
:deep(.p-datatable tbody tr:hover) {
  cursor: pointer !important;
}

/* Media query for screens below a certain width */
@media (max-width: 768px) {
  .searchbarData {
    font-size: 12px;
  }
  .dialogHeader{
    flex-wrap: wrap;
  }
}
</style>
