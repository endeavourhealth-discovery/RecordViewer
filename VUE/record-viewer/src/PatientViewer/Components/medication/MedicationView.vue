<template>

    <Card style="margin-top: 10px">
      <template #header>

        <PageHeader :searching="searchingForData" icon="fa-solid fa-pills" title="Patient medication"
                    subtitle="A record of acute and repeat medication that has been prescribed for the selected patient, including past medication."/>

        <div style="text-align:left; align-items:start; justify-content:left">
          <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
        </div>

        <DataTable v-model:filters="filters" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['name', 'dose', 'status', 'date', 'type', 'last', 'orgName', 'quantity', 'practitioner', 'code']" v-model:expandedRows="expandedMedication" :value="summaryMedication">
          <Column field="name" sortable header="Current medication" style="width:40%"></Column>
          <Column field="dose" sortable header="Dose" style="width:40%"></Column>
          <Column field="status" sortable header="Status" style="width:10%"></Column>
          <Column field="date" sortable header="Date" style="width:10%"></Column>
          <Column expander />
          <template #expansion="slotProps">
            <div style="display:flex">
              <div style="display:inline; padding-right:10%">
                <p>Type: <b>{{slotProps.data.type}}</b></p>
                <p>Last prescribed: <b>{{slotProps.data.last}}</b></p>
              </div>
              <div style="display:inline; padding-right:10%">
                <p>Source: <b>{{slotProps.data.orgName}}</b></p>
                <p>Quantity: <b>{{slotProps.data.quantity}}</b></p>
              </div>
              <div style="display:inline; padding-right:10%">
                <p>Practitioner: <b>{{slotProps.data.practitioner}}</b></p>
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
import PageHeader from "@/components/PageHeader.vue";

export default {
  name: "MedicationView",
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
        this.summaryMedication = await ApiPopulation.populateMedication(this.selectedPatient.personId, "1");
        this.searchingForData=false;
      }
      else this.summaryMedication = null;
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
      searchingForData: false,
      summaryMedication: null,
      expandedMedication: null,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedMedication: null
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
