<template>
  <tr v-on:click="viewPatient" class="patient-info">
    <th class="patient-name-and-address">
      <label class="patient-name">{{ patient["name"] }}</label>
      <br />
      <label class="patient-address">{{ patient["address"] }}</label>
    </th>
    <th class="patient-dob-and-gender">
      <label class="patient-dob">{{ patient["dob"] }} </label>
      <br />
      <label class="patient-gender">{{ patient["gender"] }}</label>
    </th>
    <th class="patient-nhs-number">{{ patient["nhsNumber"] }}</th>
  </tr>
</template>

<script>
export default {
  name: "PatientEntryStructure",
  props: ["patient", "index"],
  data() {
    return {
      nhsNumber: this.patient["nhsNumber"],
    };
  },
  methods: {
    viewPatient() {
      sessionStorage.setItem("patient", JSON.stringify(this.patient));
      this.$router.push({
        name: "PatientViewer",
        params: { nhsNum: this.nhsNumber },
      });
    },
  },
};
</script>

<style scoped>
.patient-name-and-address {
  border: solid 1px;
}
.patient-dob-and-gender {
  border: solid 1px;
}
.patient-nhs-number {
  border: solid 1px;
}
</style>
