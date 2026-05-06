<template>
  <Card>
    <template #header>
      <PageHeader :searching="searchingForData" icon="fa-solid fa-notes-medical" title="Care record summary"
                  subtitle="A summary of the selected patient's key care record events"/>

      <div class="accordionDiv">
        <Accordion id="leftAccordion" expandIcon="fa-solid fa-plus" collapseIcon="fa-solid fa-minus" :multiple="true"
                   :activeIndex="leftActive" style="margin: 0 30px 0 30px; border: var(--surface-100) double 1px;">
          <AccordionTab :disabled="activeConditions.length === 0">
            <template #header>
              <div class="accordion-header-text">Active conditions/problems
                <div class="none-found"
                     v-if="searchingForData && !activeConditions.length">(searching)
                </div>
                <div class="none-found"
                     v-else-if="activeConditions.length === 0">(none found)
                </div>
              </div>
              <i class="fa-solid fa-diagnoses" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in activeConditions" :key="tab.code" :header="tab.name">
                <p>Status: <b>{{ tab.status }}</b></p>
                <p>Date: <b>{{ tab.date }}</b></p>
                <p>Source: <b>{{ tab.orgName }}</b></p>
                <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
                <p>Code: <b>{{ tab.code }}</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>

          <AccordionTab :disabled="allergies.length === 0">
            <template #header>
              <div class="accordion-header-text">Allergies/intolerances
                <div class="none-found"
                     v-if="searchingForData && !allergies.length">(searching)
                </div>
                <div v-else-if="allergies.length === 0" class="none-found">(none found)</div></div>
              <i class="fa-solid fa-allergies" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in allergies" :key="tab.name" :header="tab.name">
                <p>Code: <b>{{ tab.code }}</b></p>
                <p>Status: <b>{{ tab.status }}</b></p>
                <p>Organisation: <b>{{ tab.orgName }}</b></p>
                <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
                <p>Date: <b>{{ tab.date }}</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>

          <AccordionTab :disabled="appointments.length === 0">
            <template #header>
              <div class="accordion-header-text">Future appointments
                <div class="none-found"
                     v-if="searchingForData && !appointments.length">(searching)
                </div>
                <div v-else-if="appointments.length === 0" class="none-found">(none found)</div></div>
              <i class="fa-solid fa-calendar-alt" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in appointments" :key="tab.date"
                            :header="tab.location + '&emsp; &emsp;' + tab.date">
                <p>Type: <b>{{ tab.type }}</b></p>
                <p>Code: <b>{{ tab.code }}</b></p>
                <p>Status: <b>{{ tab.status }}</b></p>
                <p>Duration: <b>{{ tab.duration }} minutes</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>

          <AccordionTab :disabled="diagnostics.length === 0">

            <template #header>
              <div class="accordion-header-text" style="display: flex">
                <div style="padding: 2px">Recent results</div>
                <div class="none-found"
                     v-if="searchingForData && !diagnostics.length">(searching)
                </div>
                <div
                  v-else-if="diagnostics.length === 0" class="none-found">(none found)</div>
              <div v-if="diagnostics.length !== 0">  &nbsp;
                <DiagnosticReportChart :buttonStyle="chartButtonStyle" :selected="selectedDiagnostics"
                                     :values="diagnostics" style="margin:0px"></DiagnosticReportChart></div>
              </div>
              <i class="fa-solid fa-x-ray" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <DataTable v-model:selection="selectedDiagnostics" selectionMode="multiple" paginator :rows="10"
                       v-model:expandedRows="expandedDiagnostic" :value="diagnostics" tableStyle="min-width: 100%">
              <Column field="term" sortable header="Diagnostic report" style="width:40%"></Column>
              <Column field="result" sortable header="Result" style="width:40%"></Column>
              <Column field="date" sortable header="Effective date" style="width:10%"></Column>
              <Column expander/>
              <template #expansion="slotProps">
                <div style="display:inline; padding-right:10rem">
                  <p>Source of data: <b>{{ slotProps.data.orgName }}</b></p>
                </div>
                <div style="display:inline; padding-right:10rem">
                  <p>Practitioner: <b>{{ slotProps.data.practitioner }}</b></p>
                </div>
                <div style="display:inline; padding-right:10rem">
                  <p>Code: <b>{{ slotProps.data.code }}</b></p>
                </div>
              </template>
            </DataTable>
          </AccordionTab>

          <AccordionTab :disabled="procedures.length === 0">
            <template #header>
              <div class="accordion-header-text">Procedures
                <div class="none-found"
                     v-if="searchingForData && !procedures.length">(searching)
                </div>
                <div v-else-if="procedures.length === 0" class="none-found">(none found)</div></div>
              <i class="fa-solid fa-procedures" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in procedures" :key="tab.code" :header="tab.name + '&emsp; &emsp;' + tab.date">
                <p>Code: <b>{{ tab.code }}</b></p>
                <p>Source of data: <b>{{ tab.orgName }}</b></p>
                <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>
        </Accordion>

        <Accordion id="rightAccordion" expandIcon="fa-solid fa-plus" collapseIcon="fa-solid fa-minus" :multiple="true"
                   :activeIndex="rightActive" style="margin: 0px 30px 0 30px;  border: var(--surface-100) double 1px;">
          <AccordionTab :disabled="medication.length === 0">
            <template #header>
              <div class="accordion-header-text">Current medication
                <div class="none-found"
                     v-if="searchingForData && !medication.length">(searching)
                </div>
                <div v-else-if="medication.length === 0" class="none-found">(none found)</div></div>
              <i class="fa-solid fa-pills" style="font-size:1.6rem"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in medication" :key="tab.name" :header="tab.name">
                <p>Code: <b>{{ tab.code }}</b></p>
                <p>Dose: <b>{{ tab.dose }}</b></p>
                <p>Quantity: <b>{{ tab.quantity }}</b></p>
                <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
                <p>Status: <b>{{ tab.status }}</b></p>
                <p>Date: <b>{{ tab.date }}</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>

          <AccordionTab :disabled="warnings.length === 0">
            <template #header>
              <div class="accordion-header-text">Warnings/flags
                <div class="none-found"
                     v-if="searchingForData && !warnings.length">(searching)
                </div>
                <div v-else-if="warnings.length === 0" class="none-found">(none found)</div></div>
              <i class="fa-solid fa-exclamation-triangle"
                 style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in warnings" :key="tab.name" :header="tab.name">
                <p>Status: <b>{{ tab.status }}</b></p>
                <p>Onset date: <b>{{ tab.date }}</b></p>
                <p>Organisation: <b>{{ tab.orgName }}</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>

          <AccordionTab :disabled="encounters.length === 0">
            <template #header>
              <div class="accordion-header-text">Recent encounters
                <div class="none-found"
                     v-if="searchingForData && !encounters.length">(searching)
                </div>
                <div v-else-if="encounters.length === 0" class="none-found">(none found)</div>

                <div v-if="encounters.length !== 0">
                  <EncounterTimeline :encounters="encounters" buttonStyle="margin: -2px 0 0 8px; padding: 2px;"/>
                </div>
              </div>
              <i class="fa-solid fa-user-doctor" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in encounters" :key="tab.code" :header="tab.type + '&emsp; &emsp;' + tab.date">
                <p>Code: <b>{{ tab.code }}</b></p>
                <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
                <p>Source of data: <b>{{ tab.orgName }}</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>


          <AccordionTab :disabled="immunisations.length === 0">
            <template #header>
              <div class="accordion-header-text">Immunisations
                <div class="none-found"
                     v-if="searchingForData && !immunisations.length">(searching)
                </div>
                <div v-else-if="immunisations.length === 0" class="none-found">(none found)</div></div>
              <i class="fa-solid fa-syringe" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in immunisations" :key="tab.code" :header="tab.name">
                <p>Code: <b>{{ tab.code }}</b></p>
                <p>Source of data: <b>{{ tab.orgName }}</b></p>
                <p>Practitioner: <b>{{ tab.practitioner }}</b></p>
                <p>Date: <b>{{ tab.date }}</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>

          <!-- removed observations as not required at the moment, code kept
          incase this changes in the future-->
          <!--<AccordionTab :disabled="observations.length === 0">
            <template #header>
              <span style="display:flex">Observations <div v-if="observations.length === 0" class="none-found">(none found)</div></span>
              <i class="fa-solid fa-heart-pulse" style="font-size:1.6rem; margin-right:10px; display:inline"></i>
            </template>
            <Accordion class="sub-accordion" :multiple="true">
              <AccordionTab v-for="tab in observations" :key="tab.name" :header="tab.name">
                <p>Code: <b>{{ tab.code }}</b></p>
                <p>Status: <b>{{ tab.status }}</b></p>
                <p>Onset date: <b>{{tab.date}}</b></p>
              </AccordionTab>
            </Accordion>
          </AccordionTab>-->
        </Accordion>
      </div>
    </template>
  </Card>
</template>

<script>
import ApiPopulation from "@/API/ApiPopulation";
import DiagnosticReportChart from "@/components/DiagnosticReportChart.vue";
import PageHeader from "@/components/PageHeader.vue";
import EncounterTimeline from "@/components/EncounterTimeline.vue";

export default {
  name: "PatientSummaryScreen",
  params: {
    nhsNum: String
  },
  props: {
    selectedPatient: null
  },
  components: {
    EncounterTimeline,
    DiagnosticReportChart,
    PageHeader
  },
  data() {
    return {
      patient: {},
      patientSummary: {},
      medication: [],
      warnings: [],
      diagnostics: [],
      selectedDiagnostics: [],
      expandedDiagnostic: null,
      allergies: [],
      activeConditions: [],
      appointments: [],
      observations: [],
      encounters: [],
      procedures: [],
      immunisations: [],
      rightActive: [],
      leftActive: [],
      chartButtonStyle: "margin: 0px; padding: 2px;",
      searchingForData: false
    };
  },
  mounted() {
    this.populateFields();
  },
  watch: {
    selectedPatient: async function() {

      this.patient = {};
      this.patientSummary = {};
      this.medication = [];
      this.warnings = [];
      this.diagnostics = [];
      this.selectedDiagnostics = [];
      this.expandedDiagnostic = null;
      this.allergies = [];
      this.activeConditions = [];
      this.appointments = [];
      this.observations = [];
      this.encounters = [];
      this.procedures = [];
      this.immunisations = [];
      this.rightActive = [];
      this.leftActive = [];
      if (this.selectedPatient != null)
        await this.populateFields()
    }
  },
  unmounted(){
    this.nhsNumber = null;
  },
  methods: {
    async populateFields() {
      let num = this.$route.fullPath.split("#")[0];
      num = num.split("nhsNum=")[1];
      if (num === undefined || num === null || num === "") {
        if(this.selectedPatient != null) {
          this.nhsNumber = this.selectedPatient.nhsNumber;
        } else this.nhsNumber = null;
      } else {
        this.nhsNumber = num;
      }
      if(this.nhsNumber != null) {
        this.searchingForData = true;
        this.patient = await ApiPopulation.searchForPatient(this.nhsNumber);
        this.activeConditions = await ApiPopulation.populateActiveConditionsProblems(this.patient.personId);
        this.medication = await ApiPopulation.populateMedication(this.patient.personId, "1");
        this.allergies = await ApiPopulation.populateAllergies(this.patient.personId);
        this.warnings = await ApiPopulation.populateWarningsAndFlags(this.patient.personId, "1");
        this.appointments = await ApiPopulation.populateAppointments(this.patient.personId);
        this.appointments = this.appointments.slice(0, 5);
        this.encounters = await ApiPopulation.populateEncounters(this.patient.personId, "1");
        this.encounters = this.encounters.slice(0, 5);
        this.diagnostics = await ApiPopulation.populateDiagnostics(this.patient.personId, "0");
        this.immunisations = await ApiPopulation.populateImmunisations(this.patient.personId, "1");
        this.procedures = await ApiPopulation.populateProcedures(this.patient.personId, "1");
        this.procedures = this.procedures.slice(0, 5);

        if (this.activeConditions.length > 0) {
          this.leftActive.push(0);
        }
        if (this.allergies.length > 0) {
          this.leftActive.push(1);
        }
        if (this.appointments.length > 0) {
          this.leftActive.push(2);
        }
        if (this.diagnostics.length > 0) {
          this.leftActive.push(3);
        }
        if (this.procedures.length > 0) {
          this.leftActive.push(4);
        }

        if (this.medication.length > 0) {
          this.rightActive.push(0);
        }
        if (this.warnings.length > 0) {
          this.rightActive.push(1);
        }
        if (this.encounters.length > 0) {
          this.rightActive.push(2);
        }
        if (this.immunisations.length > 0) {
          this.rightActive.push(3);
        }

        this.searchingForData = false;
      } else {
        this.patient = [];
        this.activeConditions = [];
        this.medication = [];
        this.allergies = [];
        this.warnings = [];
        this.appointments = [];
        this.encounters = [];
        this.diagnostics = [];
        this.immunisations = [];
        this.procedures = [];
      }
      //this.observations = await ApiPopulation.populateActiveObservations(this.patient.personId);
      //this.procedures = await ApiPopulation.populateActiveProcedures(this.patient.personId);
    }
  }
};
</script>
<style scoped>

:deep(.p-accordion-header-link) {
  border-style: none none solid none;
  border-color: var(--surface-300);
  border-width: 1px;
  background-color: var(--surface-200);
}

:deep(.p-accordion-header:not(.p-highlight) .p-accordion-header-link) {
  background-color: var(--surface-300);
  border-color: var(--surface-400);
}

:deep(.sub-accordion .p-accordion-header:not(.p-highlight) .p-accordion-header-link) {
  background-color: var(--surface-0);
}

:deep(.sub-accordion .p-accordion-header-link) {
  background-color: var(--surface-0);
  border-style: none none none none;
  border-color: var(--surface-300);
  border-width: 0px;
}

:deep(.sub-accordion .p-accordion-header) {
  border-width: 0px;
}

.sub-accordion {
  text-align:left;
  margin-top: 3px;
  border-width: 0px;
}

.none-found{
  margin-left:24px;
}


#leftAccordion {
  width: 46%;
}

#rightAccordion {
  width: 46%;
}

.accordion-header-text{
  display:flex;
  width:80%;
  text-align:left;
}

.accordionDiv {
  display: flex; /* Components next to each other above 768px */
}

/* Media query for screens below a certain width */
@media (max-width: 1000px) {
  .accordionDiv {
    display: inline; /* Components stacked on top of each other below 768px */
  }

  #leftAccordion {
    width: 90%;
  }

  #rightAccordion {
    width: 90%;
  }
}
</style>
