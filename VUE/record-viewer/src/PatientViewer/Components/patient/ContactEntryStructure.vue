<template>
  <div
    :class="open ? 'ContactEntryStructure open' : 'ContactEntryStructure'"
    @click="$emit('toggleOpen', index)"
  >
    <div class="contactName">
      {{ contact.name[0].firstName }} {{ contact.name[0]["middle-name"] }}
      {{ contact.name[0]["last-name"] }}
    </div>
    <div class="contactDetails">
      <div class="contactRelationship">
        Relationship: {{ contact.relationship }}
      </div>
      <ContactPointStructure
        v-for="(contactType, i) in contact.telecom"
        :contactType="contactType"
        :index="i"
        :key="i"
      />
      <div class="contactAddress">Address: {{ contact.address }}</div>
      <div class="contactGender">Gender: {{ contact.gender }}</div>
      <div class="contactOrganisation">
        Organisation: {{ contact.organization }}
      </div>
      <div class="contactPeriod">Period: {{ contact.period }}</div>
    </div>
  </div>
</template>

<script>
import ContactPointStructure from "@/PatientViewer/Components/patient/ContactPointStructure";

export default {
  name: "ContactEntryStructure",
  components: { ContactPointStructure },
  props: ["contact", "index", "open"],
  data() {
    return {
      contactName: "",
    };
  },
  mounted() {
    this.contactName =
      this.contact.name.firstName +
      " " +
      this.contact.name["middle-name"] +
      " " +
      this.contact.name["last-name"];
  },
};
</script>

<style scoped>
.ContactEntryStructure {
  display: block;
  width: 100%;
  max-width: 768px;
  margin: 15px auto;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.2);
  background-color: whitesmoke;
}

.ContactEntryStructure .contactName {
  position: relative;
  color: #3c3c3c;
  font-size: 20px;
  transition: all 0.4s linear;
}

.ContactEntryStructure .contactName::after {
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

.ContactEntryStructure.open .contactName {
  margin-bottom: 15px;
}

.ContactEntryStructure.open .contactName::after {
  transform: translateY(-50%) rotate(90deg);
}

.ContactEntryStructure .contactDetails {
  color: #2c3e50;
  font-size: 20px;
  opacity: 0;
  max-height: 0px;
  overflow-y: hidden;
  transition: all 0.4s ease-out;
}

.ContactEntryStructure.open .contactDetails {
  opacity: 1;
  max-height: 1000px;
}
</style>
