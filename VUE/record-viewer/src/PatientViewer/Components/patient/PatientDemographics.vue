<template>
  <Card role="main" aria-label="Patient-Demographics-Page" aria-description="use up and down arrow keys to navigate data" v-if="selectedPatient!=null" style="margin-top:10px">
    <template #header>
      <div style="display:flex">
      <PageHeader :searching="false" icon="fa-solid fa-house" title="Patient demographics"
                  subtitle="Full demographic information for the selected patient."/>
      <Button v-if="patientHasMother == true" @click="openMothersPage" style="margin: 16px 16px 0 30%;
        height: 40px;">View Mother</Button>
      </div>
    </template>
    <template #content>

      <div class="data">
        <FieldSet style="margin:0 10px 0 10px" legend="Name"><p>{{selectedPatient.name}}</p> <p v-if="selectedPatient.name === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="Gender"><p>{{selectedPatient.gender}}</p><p v-if="selectedPatient.gender === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="DOB (age)"><p>{{selectedPatient.dob}} ({{selectedPatient.age}})</p><p v-if="selectedPatient.dob === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="NHS number"><p>{{selectedPatient.nhsNumber}}</p><p v-if="selectedPatient.nhsNumber === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="Address"><p>{{selectedPatient.address}}</p><p v-if="selectedPatient.address === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="DOD"><p>{{selectedPatient.dod}}</p><p v-if="selectedPatient.dod === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="Mobile"><p>{{selectedPatient.mobile}}</p><p v-if="selectedPatient.mobile === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="Usual GP"><p>{{selectedPatient.usual_gp}}</p><p v-if="selectedPatient.usual_gp === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="Organisation"><p>{{selectedPatient.organisation}}</p><p v-if="selectedPatient.organisation === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="Registration date"><p>{{selectedPatient.start_date}}</p><p v-if="selectedPatient.start_date === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="Registration status"><p>{{selectedPatient.registration}}</p><p v-if="selectedPatient.registration === ''">N/A</p></FieldSet>
        <FieldSet style="margin:0 10px 0 10px" legend="Additional">
          <template v-for="(item, index) in selectedPatient.additionals.concept" :key="index">
            <Button text class="hyperlink" v-if="conceptBecomesButton(item, index) && accessMode==='record-viewer'" @click="conceptButtonClick(item, index)" >{{item}}</Button>
            <p v-else-if="conceptBecomesButton(item, index)">{{item}} in database</p>
            <p v-else>{{item}}: {{selectedPatient.additionals.textValue[index]}}</p>
          </template>
          <p v-if="selectedPatient.id === ''">N/A</p></FieldSet>
      </div>
    </template>
  </Card>
</template>
<script>
//import PatientEntryStructure from "@/PatientViewer/Components/patient/PatientEntryStructure";
import KeycloakService from "@/KeycloakService";
import PageHeader from "@/components/PageHeader.vue";
import ApiPopulation from "@/API/ApiPopulation";

export default {
  name: "PatientDemographics",
  components: {PageHeader},
  //components: {
    //PatientEntryStructure,
  //},
  props: {
    accessMode: {default: "Viewer"},
    selectedPatient: null
  },
  data() {
    return {
      isMounted: false,
      motherSnomedId: "Natural mother (person)",
      patientHasMother: false,
      mother: null
    }
  },
  methods: {
    conceptBecomesButton(concept, index){
      if(this.motherSnomedId === concept){
        this.populateMother(index)
        return true;
      }
      return false;
    },
    conceptButtonClick(concept, index){
      if(concept === this.motherSnomedId){
        this.openMothersPage(index);
      }
    },
    async populateMother(index){
      let m = await ApiPopulation.searchForSinglePatientWithId(this.selectedPatient.additionals.textValue[index]);
      this.mother = m[0];
    },
    logOut() {
      return KeycloakService.CallLogOut();
    },
    async openMothersPage() {
      const routeData = this.$router.resolve({
        path: '/',
        query: { nhsNum: this.mother.nhsNumber }
      });
      window.open(routeData.href, '_blank').focus();
    },
  },
  mounted() {
    this.isMounted=true;
  },
  unmounted() {
    this.isMounted=false;
  }
};
</script>
<style scoped>
.patientData{
  width:33%;
}
.hyperlink{
  color: var(--blue-400);
}
.hyperlink:hover{
  cursor:pointer;
  background-color:var(--surface-0);
  color: var(--blue-500);
}
.data{
  display: grid;
  grid-template-columns: repeat(4, 1fr); /* Three columns */
  grid-auto-rows: auto;
  gap: 10px; /* Adjust as needed */
}
/* Media query for screens below a certain width */
@media (max-width: 1000px) {
  .data {
    display: grid;
    grid-template-columns: repeat(3, 1fr); /* Three columns */
    grid-auto-rows: auto;
    gap: 10px; /* Adjust as needed */
  }
}
/* Media query for screens below a certain width */
@media (max-width: 768px) {
  .data {
    display: grid;
    grid-template-columns: repeat(2, 1fr); /* Three columns */
    grid-auto-rows: auto;
    gap: 10px; /* Adjust as needed */
  }
}
</style>
