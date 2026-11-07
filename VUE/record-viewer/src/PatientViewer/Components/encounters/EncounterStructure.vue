<template>
  <div
    :class="open ? 'EncounterStructure Expanded' : 'EncounterStructure'"
    @click="$emit('toggleOpen', index)"
  >
    <div class="encounterDate">{{ encounter.encounterDate }}</div>
    <div class="encounterDetails">
      <div class="reason">Reason for encounter: {{ encounter.reason }}</div>
      <div class="result">Result of encounter: {{ encounter.result }}</div>
      <div class="treatment">Proposed Treatment: {{ encounter.treatment }}</div>
      <div class="notes">Clinical Notes: {{ encounter.notes }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: "EncounterStructure",
  props: ["encounter", "index", "open"],
};
</script>

<style scoped>
.EncounterStructure {
  display: block;
  width: 100%;
  max-width: 768px;
  margin: 15px auto;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.2);
  background-color: whitesmoke;
}
.EncounterStructure .encounterDate {
  position: relative;
  color: #3c3c3c;
  font-size: 20px;
  transition: all 0.4s linear;
}
.EncounterStructure .encounterDate::after {
  content: "";
  position: absolute;
  top: 50%;
  right: 0px;
  transform: translate(-50%);
  width: 30px;
  height: 30px;
  background-image: url("./down-arrow.svg");
  background-position: center;
  background-size: contain;
  background-repeat: no-repeat;
  transition: all 0.4s linear;
}
.EncounterStructure.Expanded .encounterDate {
  margin-bottom: 15px;
}
.EncounterStructure.Expanded .encounterDate::after {
  transform: translateY(-50%) rotate(90deg);
}

.EncounterStructure .encounterDetails {
  color: #2c3e50;
  font-size: 20px;
  opacity: 0;
  max-height: 0px;
  overflow-y: hidden;
  transition: all 0.4s ease-out;
}

.EncounterStructure.Expanded .encounterDetails {
  opacity: 1;
  max-height: 1000px;
}
</style>
