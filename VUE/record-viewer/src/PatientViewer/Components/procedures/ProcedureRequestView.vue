<template>

  <Card style="margin-top:10px">
    <template #header>
      <PageHeader :searching="searchingForData" icon="fa-solid fa-house-medical-flag" title="Procedure requests"
                  subtitle="A record of a request for a procedure to be performed."/>
      <div style="text-align:left; align-items:start; justify-content:left">
        <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
      </div>

      <DataTable v-model:filters="filters" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['name', 'date', 'orgName', 'code', 'practitioner']" v-model:expandedRows="expandedRequest" :value="procedures">
        <Column field="name" sortable header="Procedure request" style="width:60%"></Column>
        <Column field="status" sortable header="Status" style="width:20%"></Column>
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
  name: "ProcedureRequestView",
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
        this.procedures = await ApiPopulation.populateProcedureRequests(this.selectedPatient.personId);
        this.searchingForData=false;
      }
      else this.procedures = null;
    }
  },
  data() {
    return {
      isMounted: false,
      procedures: null,
      expandedRequest: null,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedRequest: null,
      searchingForData: false
    }
  },
  watch: {
    selectedPatient: async function(){
      await this.populateData()
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