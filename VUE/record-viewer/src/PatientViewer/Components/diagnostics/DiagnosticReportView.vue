<template>

  <Card style="margin-top:10px;">
    <template #header>
      <PageHeader :searching="searchingForData" icon="fa-solid fa-x-ray" title="Diagnostic Report"
                  subtitle="The findings and interpretation of diagnostic tests performed on the selected patient."/>

      <div style="display:flex; text-align:left; align-items:start; justify-content:left" v-if="diagnostics !== null">
        <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
        <DiagnosticReportChart :selected="selectedValues" :values="diagnostics" buttonStyle="margin: 0px"/>
      </div>
      <DataTable v-model:filters="filters" v-model:selection="selectedValues" selectionMode="multiple" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['term', 'result', 'date', 'orgName', 'practitioner', 'code']" v-model:expandedRows="expandedDiagnostic" :value="diagnostics">
        <Column field="term" sortable header="Diagnostic report" style="width:40%"></Column>
        <Column field="result" sortable header="Result" style="width:40%"></Column>
        <Column field="date" sortable header="Effective date" style="width:10%"></Column>
        <Column expander />
        <template #expansion="slotProps">
          <div style="display:flex; background-color: var(--surface-100)">
            <div style="display:inline; padding-right:10%">
              <p>Source of data: <b>{{slotProps.data.orgName}}</b></p>
            </div>
            <div style="display:inline; padding-right:10%">
              <p>Practitioner: <b>{{slotProps.data.practitioner}}</b></p>
            </div>
            <div style="display:inline; padding-right:10%">
              <p>Code: <b>{{slotProps.data.code}}</b></p>
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
import DiagnosticReportChart from "@/components/DiagnosticReportChart.vue";
import PageHeader from "@/components/PageHeader.vue";

export default {
  name: "DiagnosticReportView",
  components: {
    DiagnosticReportChart,
    PageHeader
  },
  props: {
    selectedPatient: null
  },
  methods: {
    FilterMatchMode() {
      return FilterMatchMode
    },
    async populateData(){
      if(this.selectedPatient !== null) {
        this.searchingForData = true;
        this.diagnostics = await ApiPopulation.populateDiagnostics(this.selectedPatient.personId, '0');
        this.searchingForData = false;
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
      expandedDiagnostic: null,
      searchingForData: false,
      diagnostics: null,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedValues: [],
      toggleOverlay: false,
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
