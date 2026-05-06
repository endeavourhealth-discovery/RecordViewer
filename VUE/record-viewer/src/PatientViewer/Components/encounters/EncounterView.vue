<template>
  <div>
    <Card style="margin-top:10px">
      <template #header>
        <PageHeader :searching="searchingForData" icon="fa-solid fa-user-doctor" title="Encounters"
                    subtitle="An interaction between the patient and healthcare provider(s) for the purpose of providing healthcare service(s) or assessing the health status of the selected patient."/>
        <div style="text-align:left; align-items:start; justify-content:left" v-if="encounters !== null">
          <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px" />
          <EncounterTimeline :show-input="true" :encounters="encounters" buttonStyle="margin-bottom: 4px"/>
        </div>
        <div style="display: flex; flex-direction: row" v-show="showDialog">
          <Button text @click="clearTree"><i class="fa-solid fa-chevron-left"/></Button>
          <h3>{{encounterText}}</h3>
        </div>
        <DataTable v-show="!showDialog" v-model:filters="filters" v-model:selection="selectedEncounter"paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['type', 'location', 'practitioner', 'date', 'code', 'orgName']" v-model:expandedRows="expandedEncounter" :value="encounters">
          <Column selectionMode="single"></Column>
          <Column field="type" sortable header="Encounter type" style="width:20%"></Column>
          <Column field="location" sortable header="Location" style="width:30%"></Column>
          <Column field="practitioner" sortable header="Practitioner" style="width:30%"></Column>
          <Column field="date" sortable header="Date of encounter" style="width:20%"></Column>
          <Column expander />
          <template #expansion="slotProps">
            <div style="display:flex">
              <div style="display:inline; padding-right:10%">
                <p>Source of data: <b>{{slotProps.data.orgName}}</b></p>
              </div>
              <div style="display:inline; padding-right:10%">
                <p>Code: <b>{{slotProps.data.code}}</b></p>
              </div>
            </div>

            <div v-if="slotProps.data.additionals">
              <ul v-if="slotProps.data.additionals[0].id" style="list-style-type: none; padding:0; margin: 0;">
                <li v-for="(item, index) in slotProps.data.additionals" style="margin-bottom: 14px" :key="index">
                  {{ item.concept }}: <b>{{ item.textValue }}</b>
                </li>
              </ul>
            </div>
          </template>
        </DataTable>
        <div v-show="showDialog">
          <EncounterTree :encounter="encounterForTree" @encounterText="handleEncounterText" @episodeText="handleEpisodeText"/>
        </div>
      </template>
      <template #footer>
        <b v-show="showDialog">{{episodeText}}</b>
      </template>
    </Card>
  </div>
</template>

<script>
import {FilterMatchMode} from "primevue/api";
import ApiPopulation from "@/API/ApiPopulation";
import PageHeader from "@/components/PageHeader.vue";
import EncounterTimeline from "@/components/EncounterTimeline.vue";
import DiagnosticReportChart from "@/components/DiagnosticReportChart.vue";
import EncounterTree from "@/components/EncounterTree.vue";

export default {
  name: "EncounterView",
  components: {EncounterTree, EncounterTimeline, PageHeader},
  props: {
    selectedPatient: null
  },
  methods: {
    FilterMatchMode() {
      return FilterMatchMode
    },
    clearTree(){
      this.showDialog = false;
      this.encounterForTree = {};
      this.selectedEncounter = null;
    },
    handleEpisodeText(episodeText){
      this.episodeText = episodeText
    },
    handleEncounterText(encounterText){
      this.encounterText = encounterText
    },
    async populateData(){
      if(this.selectedPatient!=null){
        this.searchingForData=true;
        this.encounters = await ApiPopulation.populateEncounters(this.selectedPatient.personId, "0");
        this.searchingForData=false;
      }
      else this.encounters = null;
    },
    async rowExpanded(){
      if(this.expandedEncounter[this.expandedEncounter.length-1]) {
        this.searchingForAdditionals = true;
        this.expandedEncounter[this.expandedEncounter.length - 1].additionals =
            await ApiPopulation.populateAdditionals("encounter", this.expandedEncounter[this.expandedEncounter.length - 1].id)
        for (let i = 0; i < this.encounters.length; i++) {
          if (this.expandedEncounter[this.expandedEncounter.length - 1].id == this.encounters[i].id) {
            this.encounters[i] = this.expandedEncounter[this.expandedEncounter.length - 1];
            break;
          }
        }
        this.searchingForAdditionals = false;
      }
    }
  },
  watch: {
    selectedPatient: async function(){
      await this.populateData()
    },
    expandedEncounter: async function(){
      if (this.expandedEncounter) {
        await this.rowExpanded();
      }
    },
    selectedEncounter: function(){
      if(this.selectedEncounter){
        this.showDialog = true;
        this.encounterForTree = this.selectedEncounter; // Ensure reactivity
      } else {
        this.showDialog = false;
        this.encounterForTree = {}; // Ensure reactivity
      }
    },
  },
  data() {
    return {
      isMounted: false,
      encounters: null,
      expandedEncounter: null,
      episodeText: "",
      encounterText: "",
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedEncounter: null,
      encounterForTree: {},
      searchingForData: false,
      showDialog: false,
      totalExpanded: 0
    }
  },
  mounted(){
    this.isMounted=true;
    this.populateData();
  },
  unmounted(){
    this.isMounted=false;
  }
}
</script>

<style scoped>

</style>