<template>

  <Card style="margin-top:10px">
    <template #header>
      <PageHeader :searching="searchingForData" icon="fa-solid fa-microscope" title="Diagnostic orders"
                  subtitle="A record of a request for a diagnostic investigation service to be performed."/>
      <div style="text-align:left; align-items:start; justify-content:left">
        <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
      </div>

      <DataTable v-model:filters="filters" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['name', 'date', 'orgName', 'code', 'practitioner']" v-model:expandedRows="expandedDiagnostic" :value="diagnostics">
        <Column field="name" sortable header="Diagnostic order" style="width:80%"></Column>
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
  name: "DiagnosticOrderView",
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
        this.diagnostics = await ApiPopulation.populateDiagnosticOrders(this.selectedPatient.personId);
        this.searchingForData=false;
      }
      else this.diagnostics = null;
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
      diagnostics: null,
      expandedDiagnostic: null,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedDiagnostic: null,
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