<template>

  <Card style="margin-top:10px">
    <template #header>
      <PageHeader :searching="searchingForData" icon="fa-solid fa-hospital-user" title="Referral requests"
                  subtitle="A record of a request for a referral service or transfer of the selected patient to the care of another provider or provider organisation."/>
      <div style="text-align:left; align-items:start; justify-content:left">
        <InputText v-model="filters['global'].value" placeholder="Keyword search" style="margin: 0 10px 4px 30px"/>
      </div>

      <DataTable v-model:filters="filters" paginator :rows="5" :rowsPerPageOptions="[5, 10, 20, 50]" :global-filter-fields="['recipient', 'priority', 'type', 'code', 'mode', 'speciality', 'date', 'orgName']" v-model:expandedRows="expandedReferral" :value="referrals">
        <Column field="recipient" sortable header="Recipient" style="width:20%"></Column>
        <Column field="priority" sortable header="Priority" style="width:10%"></Column>
        <Column field="type" sortable header="Type" style="width:20%"></Column>
        <Column field="mode" sortable header="Mode" style="width:20%"></Column>
        <Column field="speciality" sortable header="Speciality" style="width:20%"></Column>
        <Column field="date" sortable header="Date of referral" style="width:10%"></Column>
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
  name: "ReferralRequestView",
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
        this.referrals = await ApiPopulation.populateReferralRequests(this.selectedPatient.personId);
        this.searchingForData=false;
      }
      else this.referrals = null;
    },
    async rowExpanded(){
      this.searchingForAdditionals = true;
      this.expandedReferral[this.expandedReferral.length-1].additionals =
          await ApiPopulation.populateAdditionals("referral_request", this.expandedReferral[this.expandedReferral.length-1].id)
      for(let i = 0; i < this.referrals.length; i++){
        if(this.expandedReferral[this.expandedReferral.length-1].id == this.referrals[i].id){
          this.referrals[i] = this.expandedReferral[this.expandedReferral.length-1];
          break;
        }
      }
      this.searchingForAdditionals = false;

    }
  },
  data() {
    return {
      isMounted: false,
      referrals: null,
      expandedReferral: null,
      filters:{
        global: {value: null, matchMode: FilterMatchMode.CONTAINS}
      },
      selectedReferral: null,
      searchingForData: false,
      totalExpanded: 0
    }
  },
  watch: {
    selectedPatient: async function(){
      await this.populateData()
    },
    expandedReferral: async function(){
      let tempTotal = this.totalExpanded;
      this.totalExpanded = this.expandedReferral.length;
      if (this.expandedReferral.length > tempTotal) {
        await this.rowExpanded();
      }
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