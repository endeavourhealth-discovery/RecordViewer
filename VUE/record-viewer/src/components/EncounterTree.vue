<template>
  <div style="display:flex; flex-direction:column; justify-content:space-between">
    <Tree v-model:expandedKeys="expandedKeys" :value="nodes"></Tree>
    <ProgressSpinner v-if="!nodes.length" style="width:50px; height:50px; margin-top: 20px" strokeWidth="5"/>
  </div>
</template>

<script>
import ApiPopulation from '@/API/ApiPopulation'
export default {
  name: "EncounterTree",

  props: {
    encounter: Object
  },
  data(){
    return {
      nodes: [],
      episodeText: "",
      encounterText: "",
      expandedKeys: null
    }
  },
  methods: {
    expandAll() {
      const keys = {};
      const collectKeys = (nodes) => {
        nodes.forEach((node) => {
          keys[node.key] = true;
          if (node.children) {
            collectKeys(node.children);
          }
        });
      };
      collectKeys(this.nodes);
      this.expandedKeys = keys;
    },
    async buildTree(){
      const treeHead = await ApiPopulation.populateEncounterTree(this.encounter.id);
      const medication = await ApiPopulation.populateEncounterTreeMedication(this.encounter.id);
      const observations = await ApiPopulation.populateEncounterTreeObservation(this.encounter.id);



      let observationChildren = [];
      if(observations[0].id){
        for(let i = 0; i < observations.length; i++){

          observationChildren.push({
            key: observations[i].id,
            label: observations[i].name + ' (' + observations[i].code +')',
            data: observations[i].id + ' Folder',
            children: []
          })

          if(observations[i].resultValue) observationChildren[i].children.push({
            key: observations[i].resultValue,
            label: 'Result: ' + observations[i].resultValue,
          })
          if(observations[i].resultText) observationChildren[i].children.push({
            key: observations[i].resultText,
            label: 'Result Text: ' + observations[i].resultText,
          })
          if(observations[i].resultDate) observationChildren[i].children.push({
            key: observations[i].resultDate,
            label: 'Result Date: ' + observations[i].resultDate,
          })
        }
      }

      let medicationChildren = [];
      if(medication[0].id){
        for(let i = 0; i < medication.length; i++){
          medicationChildren.push({
            key: medication[i].id,
            label: 'Prescribed: ' + medication[i].name,
            data: medication[i].id + ' Folder',
            children: []
          })
          if(medication[i].dose) {
            medicationChildren[i].children.push(
                {
                  key: medication[i].id + '.dose',
                  label: 'Dose: ' + medication[i].dose,
                  data: medication[i].id + '.dose' + ' Folder'
                }
            )
          }
          if(medication[i].quantity) {
            medicationChildren[i].children.push(
                {
                  key: medication[i].id + '.quantity',
                  label: 'Quantity: ' + medication[i].quantity,
                  data: medication[i].id + '.quantity' + ' Folder'
                }
            )
          }if(medication[i].type) {
            medicationChildren[i].children.push(
                {
                  key: medication[i].id + '.type',
                  label: 'Prescription type: ' + medication[i].type,
                  data: medication[i].id + '.type' + ' Folder'
                }
            )
          }

        }
      }
      const start = treeHead[0].episodeStartDate ? treeHead[0].episodeStartDate : 'N/A'
      const end = treeHead[0].episodeEndDate ? treeHead[0].episodeEndDate : 'Present'
      const org = treeHead[0].orgName ? treeHead[0].orgName : 'No linked organisation'
      this.episodeText = 'Care Episode: ' + org + ' (' + start + ' - ' + end + ')'
      this.encounterText = 'Encounter: ' + treeHead[0].encounterDate
      this.$emit("episodeText", this.episodeText);
      this.$emit("encounterText", this.encounterText);


      if(treeHead[0].practitioner){
        this.nodes.push(
            {
              key: treeHead[0].encounterId + " practitioner",
              label: 'Practitioner: ' + treeHead[0].practitioner,
              data: treeHead[0].encounterId + ' Practitioner'
            }
        )
      }


      if(treeHead[0].conceptName){
        this.nodes.push({
          key: treeHead[0].encounterId + ' conceptName',
              label: treeHead[0].conceptName,
            children: [{key: treeHead[0].encounterId + ' conceptID', label: 'Concept ID: ' + treeHead[0].conceptId, data: treeHead[0].encounterId + ' conceptIdData'}]
        })
      }

      for(let i = 0; i < medicationChildren.length; i++) {
        this.nodes.push(medicationChildren[i]);
      }
      for(let i = 0; i < observationChildren.length; i++) {
        this.nodes.push(observationChildren[i]);
      }
      this.expandAll();
    }
  },
  watch: {
    'encounter.id': function() {
      if(this.encounter.id)
        this.buildTree();
      else this.nodes = [];
    }
  }
}

</script>
<style scoped>
.timeline-container {
  overflow: hidden; /* Prevents overflow when dragging */
  cursor: pointer; /* Indicates that the area is draggable */
}
</style>