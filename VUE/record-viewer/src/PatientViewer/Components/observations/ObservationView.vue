<template>

  <Card style="margin-top:10px">
    <template #header>
      <PageHeader :searching="searchingForData" icon="fa-solid fa-heart-pulse" title="Observations"
                  subtitle="Measurements and simple assertions made about the selected patient."/>
      <div style="text-align:left; align-items:start; justify-content:left">
        <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
      </div>

      <DataTable v-model:filters="filters" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['name', 'category', 'date', 'orgName', 'code', 'practitioner']" v-model:expandedRows="expandedObservation" :value="observations">
        <Column field="name" sortable header="Observation" style="width:40%"></Column>
        <Column field="category" sortable header="Categories" style="width:40%"></Column>
        <Column field="date" sortable header="Effective date" style="width:20%"></Column>
        <Column expander />
        <template #expansion="slotProps">
          <div style="display:flex">
            <div style="display:inline; padding-right:10%">
              <p>Source of data: <b>{{slotProps.data.orgName}}</b></p>
            </div>
            <div style="display:inline; padding-right:10%">
              <p>Code: <b>{{slotProps.data.code}}</b></p>
            </div>

            <div style="display:inline; padding-right:10%">
              <p>Recorded by: <b>{{slotProps.data.practitioner}}</b></p>
            </div>
          </div>
          <div v-if="slotProps.data.additionals">
            <ul v-if="slotProps.data.additionals[0].id" style="list-style-type: none; padding:0; margin: 0;">
              <li v-for="(item, index) in slotProps.data.additionals" style="margin-bottom: 14px" :key="index">
                {{ item.concept }}: <b v-if="item.textValue">{{ item.textValue }}</b>
                <b v-if="item.valueId">{{ item.valueId }}</b>
                <b v-if="item.jsonValue"> Between {{ item.jsonValue[0].low.value }}{{item.jsonValue[0].low.unit}} and {{item.jsonValue[0].high.value}}{{item.jsonValue[0].high.unit}}</b>
              </li>
            </ul>
          </div>
        </template>
      </DataTable>
    </template>
  </Card>

</template>

<script>
import {FilterMatchMode} from "primevue/api";
import ApiPopulation from "@/API/ApiPopulation";
import PageHeader from "@/components/PageHeader.vue";

export default {
  name: "ObservationView",
  components: {PageHeader},
  props: {
    selectedPatient: null
  },
  methods: {
    FilterMatchMode() {
      return FilterMatchMode
    },
    async populateData(){
      if(this.selectedPatient!=null){
        this.searchingForData=true;
        this.observations = await ApiPopulation.populateAllObservations(this.selectedPatient.personId);
        this.searchingForData=false;
      }
      else this.observations = null;
    },
    async rowExpanded(){
      this.searchingForAdditionals = true;
      this.expandedObservation[this.expandedObservation.length-1].additionals =
          await ApiPopulation.populateAdditionals("observation", this.expandedObservation[this.expandedObservation.length-1].id)
      for(let j = 0; j < this.expandedObservation[this.expandedObservation.length-1].additionals.length; j++) {
        if (this.expandedObservation[this.expandedObservation.length - 1].additionals[j].valueId === "230496") {
          this.expandedObservation[this.expandedObservation.length - 1].additionals[j].valueId = "Significant (qualifier value)";
        } else
        if (this.expandedObservation[this.expandedObservation.length - 1].additionals[j].valueId === "226141") {
          this.expandedObservation[this.expandedObservation.length - 1].additionals[j].valueId = "Not significant (qualifier value)";
        }
      }
      for(let i = 0; i < this.observations.length; i++){
        if(this.expandedObservation[this.expandedObservation.length-1].id == this.observations[i].id){
          this.observations[i] = this.expandedObservation[this.expandedObservation.length-1];
          for(let j = 0; j < this.expandedObservation[this.expandedObservation.length-1].additionals.length; j++) {
            if (this.observations[i].additionals[j].jsonValue) {
              this.observations[i].additionals[j].jsonValue = JSON.parse(this.observations[i].additionals[j].jsonValue);
            }
          }
          break;
        }
      }
      this.searchingForAdditionals = false;

    }
  },
  watch: {
    selectedPatient: async function(){
      await this.populateData()
    },
    expandedObservation: async function(){
      let tempTotal = this.totalExpanded;
      this.totalExpanded = this.expandedObservation.length;
      if (this.expandedObservation.length > tempTotal) {
        await this.rowExpanded();
      }
    }
  },
  data() {
    return {
      isMounted: false,
      observations: null,
      expandedObservation: null,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedObservation: null,
      searchingForData: false,
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