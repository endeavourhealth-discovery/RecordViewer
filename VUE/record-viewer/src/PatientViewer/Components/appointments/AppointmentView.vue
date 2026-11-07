<template>

    <Card style="margin-top:10px">
      <template #header>
        <PageHeader :searching="searchingForData" icon="fa-solid fa-calendar-alt" title="Appointments"
                    subtitle="A booking of a healthcare event among patient(s), practitioner(s), and related person(s)."/>
        <div style="text-align:left; align-items:start; justify-content:left">
          <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
        </div>

        <DataTable v-model:filters="filters" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['type', 'location', 'date', 'duration', 'delay', 'status']" v-model:expandedRows="expandedAppointment" :value="appointments" tableStyle="width:100%">
          <Column field="type" sortable header="Schedule type" style="width:30%"></Column>
          <Column field="date" sortable header="Date of appointment" style="width:30%"></Column>
          <Column field="duration" sortable header="Duration" style="width:20%"></Column>
          <Column field="status" sortable header="Status" style="width:20%"></Column>
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
                <p>Delay: <b>{{slotProps.data.delay}} minutes</b></p>
              </div>

            </div>
            <div v-if="slotProps.data.additionals">
              <ul v-if="slotProps.data.additionals[0].id" style="list-style-type: none; padding:0; margin: 0;">
                <li v-for="(item, index) in slotProps.data.additionals" style="margin-bottom: 14px" :key="index">
                  {{ item.concept }}: <b>{{ item.textValue }}</b>
                </li>
              </ul>
            </div>
            <div v-if="slotProps.data.scheduleAdditionals">
              <ul v-if="slotProps.data.scheduleAdditionals[0].id" style="list-style-type: none; padding:0; margin: 0;">
                <li v-for="(item, index) in slotProps.data.scheduleAdditionals" style="margin-bottom: 14px" :key="index">
                  {{ item.concept }}: <b>{{ item.textValue }}</b>
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
  name: "AppointmentView",
  components: {PageHeader},
  props: {
    selectedPatient: null
  },
  methods: {
    FilterMatchMode() {
      return FilterMatchMode
    },
    async rowExpanded(){
      if(!this.expandedAppointment[this.expandedAppointment.length-1].additionals)
      {
        this.searchingForAdditionals = true;
        this.expandedAppointment[this.expandedAppointment.length - 1].additionals =
            await ApiPopulation.populateAdditionals("appointment", this.expandedAppointment[this.expandedAppointment.length - 1].id)
        for (let i = 0; i < this.appointments.length; i++) {
          if (this.expandedAppointment[this.expandedAppointment.length - 1].id == this.appointments[i].id) {
            this.appointments[i] = this.expandedAppointment[this.expandedAppointment.length - 1];
            break;
          }
        }
        this.expandedAppointment[this.expandedAppointment.length - 1].scheduleAdditionals =
            await ApiPopulation.populateAdditionals("schedule", this.expandedAppointment[this.expandedAppointment.length - 1].scheduleId)
        for (let i = 0; i < this.appointments.length; i++) {
          if (this.expandedAppointment[this.expandedAppointment.length - 1].id == this.appointments[i].id) {
            this.appointments[i] = this.expandedAppointment[this.expandedAppointment.length - 1];
            break;
          }
        }
        this.searchingForAdditionals = false;
      }
    },
    async populateData(){
      if(this.selectedPatient!=null){
        this.searchingForData=true;
        this.appointments = await ApiPopulation.populateAppointments(this.selectedPatient.personId);
        this.searchingForData=false;
      }
      else this.appointments = null;
    },

  },
  watch: {
    selectedPatient: async function(){
      await this.populateData()
    },
    expandedAppointment: async function(){
      let tempTotal = this.totalExpanded;
      this.totalExpanded = this.expandedAppointment.length;
      if(this.expandedAppointment.length > tempTotal) {
        await this.rowExpanded();
      }
    }
  },
  data() {
    return {
      isMounted: false,
      appointments: null,
      expandedAppointment: null,
      searchingForAdditionals: false,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedAppointment: null,
      searchingForData: false,
      totalExpanded: 0,
      additionalsDialogContent: null,
      additionalsOverlay: false
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
