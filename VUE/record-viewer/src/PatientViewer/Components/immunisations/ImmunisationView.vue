<template>

  <Card style="margin-top:10px">
    <template #header>
      <PageHeader :searching="searchingForData" icon="fa-solid fa-syringe" title="Immunisations"
                  subtitle="Current and historical administration of vaccines."/>
      <div style="text-align:left; align-items:start; justify-content:left">
        <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
      </div>

      <DataTable v-model:filters="filters" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['name', 'date', 'orgName', 'code', 'practitioner']" v-model:expandedRows="expandedImmunisation" :value="immunisations">
        <Column field="name" sortable header="Immunisation" style="width:80%"></Column>
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
  name: "ImmunisationView",
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
        this.immunisations = await ApiPopulation.populateImmunisations(this.selectedPatient.personId, "0");
        this.searchingForData=false;
      }
      else this.immunisations = null;
    }
  },
  data() {
    return {
      isMounted: false,
      immunisations: null,
      expandedImmunisation: null,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedImmunisation: null,
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