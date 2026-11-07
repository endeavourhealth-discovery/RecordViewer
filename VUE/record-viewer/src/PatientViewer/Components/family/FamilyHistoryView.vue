<template>

  <Card style="margin-top:10px">
    <template #header>
      <PageHeader :searching="searchingForData" icon="fa-solid fa-user-friends" title="Family history"
                  subtitle="Significant health conditions for a person related to the patient relevant in the context of care for the selected patient."/>
      <div style="text-align:left; align-items:start; justify-content:left">
        <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
      </div>

      <DataTable v-model:filters="filters" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['name', 'date', 'orgName', 'code', 'practitioner']" v-model:expandedRows="expandedHistory" :value="history">
        <Column field="name" sortable header="Family history" style="width:80%"></Column>
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
  name: "FamilyHistoryView",
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
        this.history = await ApiPopulation.populateFamilyHistory(this.selectedPatient.personId);
        this.searchingForData=false;
      }
      else this.history = null;
    }
  },
  watch: {
    selectedPatient: async function(){
      await this.populateData()
    }
  },
  data() {
    return {
      isMounted: false,
      history: null,
      expandedHistory: null,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedHistory: null,
      searchingForData: false
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