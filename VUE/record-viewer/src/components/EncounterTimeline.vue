<template>
  <Button type="button" @click="$event.stopPropagation(); addEncountersToTimeline(); toggleOverlay=true;" enter :style="buttonStyle">Show Timeline</Button>

  <Dialog v-model:visible="toggleOverlay" style="width: 70%; height: 70vh;" :closable="!toggleTree" modal>
    <template #header>
      <div style="display: flex; flex-direction: row" v-show="toggleTree">
        <Button text @click="clearTree"><i class="fa-solid fa-chevron-left"/></Button>
        <h3>{{ encounterText }}</h3>
      </div>
      <h3 v-show="!toggleTree">Encounter timeline</h3>
    </template>
    <div style="min-height:44vh">
      <div v-show="!toggleTree">
      <div v-show="showInput" style="display: flex; flex-direction:column">
        <label for="encounter-text">Total Encounters</label>

        <div style="display:flex; flex-direction:row; margin-bottom: 6px">
          <InputText id="encounter-text" v-model="totalItems" placeholder="Total Encounters" @keydown.enter="$event.stopPropagation(); addEncountersToTimeline()"/>
          <Button type="button" @click="$event.stopPropagation(); addEncountersToTimeline()" enter style="margin-left:4px">Update</Button>
        </div>
      </div>
      <div
          ref="timelineContainer"
          class="timeline-container"
          style="display: flex">
        <Timeline v-if="items.length >= 1" style="width:100%"
            :groups="groups"
            :items="items"
            :viewportMin="totalViewportMin"
            :viewportMax="totalViewportMax"
            :initialViewportStart="minDate"
            @click="updateTree"
            :initialViewportEnd="maxDate"
            :renderTimestampLabel="(timestamp) => formatTimestamp(timestamp)"
        >
          <template #item="{ item }">
            <div v-tooltip.top="item.tooltip" style="padding-bottom:32px;">
              {{item.name}}
            </div>
          </template>
        </Timeline>
        </div>
      </div>
      <div v-show="toggleTree">
        <EncounterTree @episodeText="handleEpisodeText" @encounterText="handleEncounterText" :encounter="expandedEncounter"/>
      </div>
    </div>
    <template #footer>
      <b v-show="toggleTree">{{episodeText}}</b>
    </template>
  </Dialog>
  <Toast/>
</template>

<script>
import {Timeline} from 'vue-timeline-chart';
import 'vue-timeline-chart/style.css';
import EncounterTree from "@/components/EncounterTree.vue";
import ApiPopulation from "@/API/ApiPopulation";
export default {
  name: "EncounterTimeline",

  props: {
    buttonStyle: String,
    encounters: Array,
    showInput: Boolean
  },
  components: {Timeline, EncounterTree},
  data(){
    return {
      colors: [
        '#ff3333', '#33ff33', '#3333ff', '#ff33ff', '#33ffff',
        '#ffff33', '#ff8833', '#8833ff', '#3388ff', '#88ff33'
      ],
      totalItems: 5,
      viewportStart: 0,
      viewportEnd: 10000000000,
      minDate: 1,
      totalViewportMin: 1,
      totalViewportMax: 2,
      maxDate: 2,
      episodeText: "",
      encounterText: "",
      isDragging: false,
      dragStartX: null,
      dragViewportMin: null,
      dragViewportMax: null,
      showArea: false,
      calendarTooltip: "Format: DD/MM/YYYY (for example 24/01/2019)",
      toggleOverlay: false,
      expandedEncounter: null,
      toggleTree: false,
      items: [],
      groups: [
      ]
    }
  },
  methods: {
    updateTree(item){
      if(item.item) {
        this.toggleTree = true;
        this.expandedEncounter = this.encounters.find(obj => obj.id === item.item.id)
      }
    },
    clearTree(){
      this.toggleTree = false;
      this.expandedEncounter = {};
    },
    handleEpisodeText(episodeText){
      this.episodeText = episodeText
    },
    handleEncounterText(encounterText){
      this.encounterText = encounterText
    },
    formatTimestamp(timestamp) {
      const date = new Date(timestamp); // Create a Date object from the timestamp
      let options = {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      };

      if (this.maxDate - this.minDate < 7 * 24 * 60 * 60 * 1000) {
        options = {
          hour: '2-digit',
          minute: '2-digit',
          day: '2-digit',
          month: '2-digit',
          year: 'numeric'
        };
      }

      // Format the date using toLocaleString
      return date.toLocaleString('en-GB', options).replace(',', ''); // "en-GB" for day/month/year format
    },
    addEncountersToTimeline() {
      this.items = [];
      this.minDate = 1;
      this.maxDate = 2;
      this.encounters.sort((a, b) => {
        return new Date(b.date) - new Date(a.date);
      });
      let timelineSize = this.totalItems;
      if (this.encounters.length < this.totalItems)
        timelineSize = this.encounters.length;
      for (let i = 0; i < timelineSize; i++) {
        let e = this.encounters[i];
        let min = new Date(e.date)
        let max = new Date(e.end_date)

        if (e.end_date == null || max.getTime() <= min.getTime())
          max = new Date(min.getTime() + 24 * 60 * 60 * 1000);

        if (!e.typeDesc)
          e.typeDesc = "No Type Specified"

        if (!this.groups.some(group => group.id === e.typeCode)) {
          let x = this.colors[this.groups.length]
          this.groups.push({id: e.typeCode, label: e.typeDesc, color: x});
        }
        let toolTip = e.location
        if(e.practitioner){
          toolTip += " | " + e.practitioner
        }
        if(e.date){
          toolTip += " | " + e.date
        }
        this.items.push({
          id: e.id,
          cssVariables: {'--item-background': this.groups.find(group => group.id === e.typeCode).color},
          tooltip: toolTip,
          group: e.typeCode,
          type: 'range',
          start: min,
          end: max
        })
      }
      this.minDate = this.items[this.items.length - 1].end;
      this.maxDate = this.items[0].start;
      this.minDate = new Date(new Date(this.minDate).getFullYear(), new Date(this.minDate).getMonth(), 1).getTime()
      this.maxDate = new Date(new Date(this.maxDate).getFullYear(), new Date(this.maxDate).getMonth() + 1, 1).getTime();
      this.totalViewportMin = this.minDate;
      this.totalViewportMax = this.maxDate;
    },
  },
  watch: {
    toggleSecondOverlay: function(){
      if(!this.toggleSecondOverlay){
        this.expandedEncounter = null;
      }
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