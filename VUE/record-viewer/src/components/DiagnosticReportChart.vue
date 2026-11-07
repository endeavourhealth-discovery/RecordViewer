<template>

  <Button type="button" v-tooltip="tooltipText" @click="$event.stopPropagation(); populateCharts()" :severity="dataSelected" enter :style="buttonStyle">Show chart</Button>

  <Dialog v-model:visible="toggleOverlay" @update:visible="resetDialog" header="Data chart view" modal>
    <template #header>
      <div>
        <Calendar v-tooltip="calendarTooltip" dateFormat="dd/mm/yy" style="margin-right: 16px" v-model="minDate" @dateSelect="updateMinDate" showIcon iconDisplay="input" placeholder="Start date"/>
        <Calendar v-tooltip="calendarTooltip" dateFormat="dd/mm/yy" style="margin-right: 16px" v-model="maxDate" showIcon iconDisplay="input" @dateSelect="updateMaxDate" placeholder="End date"/>
        <Checkbox v-model="showGridLines" style="margin: 8px 4px 8px 0" @change="updateGridLines" :binary="true"></Checkbox>
        <label style="margin: 0 16px 0 0">Grid lines</label>
        <Checkbox v-model="showArea" style="margin: 8px 4px 8px 0" @change="updateFillMode" :binary="true"></Checkbox>
        <label style="margin: 0 16px 0 0">Fill mode</label>
        <Checkbox v-show="showArea" v-model="useGradient" style="margin: 8px 4px 8px 0" @change="updateGradientMode" :binary="true"></Checkbox>
        <label v-show="showArea" style="margin: 0 16px 0 0">Gradient</label>
      </div>
    </template>
    <div style="display: flex">
      <Chart ref="primeChart" type="scatter" :data="chartData" :options="chartOptions" :width="chartWidth" height="1000"/>
    </div>

  </Dialog>
  <Toast/>
</template>

<script>
import 'chartjs-adapter-date-fns';
import {ref} from "vue";
export default {
  name: "DiagnosticReportChart",
  props: {
    selected: Array,
    values: Array,
    buttonStyle: String
  },
  data(){
    return {
      primeChart: ref(),
      minDate: null,
      maxDate: null,
      showGridLines: true,
      useGradient: false,
      showArea: false,
      calendarTooltip: "Format: DD/MM/YYYY (for example 24/01/2019)",
      toggleOverlay: false,
      chartData: [],
      chartOptions: {
        responsive: true,
        scales: {
          x: {
            type: 'time',
            ticks: {
              maxTicksLimit: 20
            },
            time: {
              unit: 'day',
              displayFormats: {
                day: 'yyyy-MM-dd'
              }
            }
          },
        },
        showLine:true
      }
    }
  },
  methods: {
    sortedArray(arr){
      return arr.sort((a,b) => {
        let fa = a.x.toLowerCase(), fb = b.x.toLowerCase();
        if (fa < fb) {
          return -1
        }
        if (fa > fb) {
          return 1
        }
        return 0
      })
    },
    populateCharts() {
      if(this.dataSelected === "success"){
        this.toggleOverlay=true;
        this.chartData.datasets = [];

        let addedTerms = new Set();

        for(let i = 0; i < this.selected.length; i++) {
          if (addedTerms.has(this.selected[i].term)) {
            continue;
          }
          let diagnosticData = []
          let totalResultsFound = 0;
          for (let j = 0; j < this.values.length; j++) {
            if (this.values[j].term === this.selected[i].term) {
              diagnosticData[totalResultsFound] = {
                x: this.values[j].date,
                y: parseFloat(this.values[j].result.match(/\d+(\.\d+)?/)[0])
              };
              totalResultsFound++;
            }
          }
          diagnosticData = this.sortedArray(diagnosticData);
          this.chartData.datasets[i] = {
            labels: diagnosticData.x,
            label: this.selected[i].term,
            data: diagnosticData,
            fill: false,
            stack: 1,
            tension: 0.4
          }
          addedTerms.add(this.selected[i].term);
        }
      }
    },
    resetDialog(){
      this.showArea = false;
      this.useGradient = false;
      this.showGridLines = true;
      this.updateGridLines();
      this.minDate = null;
      this.maxDate = null;
      this.$refs.primeChart.refresh();
    },
    updateGridLines(){
      if(this.showGridLines) {
        this.chartOptions.scales.x.grid.display = true;
        this.chartOptions.scales.y.grid.display = true;
      }
      else {
        this.chartOptions.scales.x.grid.display = false;
        this.chartOptions.scales.y.grid.display = false;
      }
      this.$refs.primeChart.refresh();
    },
    updateFillMode(){
      let fillMode = false;
      if(this.showArea){
        fillMode = 'origin';
      }
      for(let i = 0; i < this.chartData.datasets.length; i++){
        this.chartData.datasets[i].fill = fillMode;
      }
      this.$refs.primeChart.refresh();
    },
    updateGradientMode() {
      let gradient = false;
      if(this.useGradient){
        gradient = true;
      }
      for(let i = 0; i < this.chartData.datasets.length; i++){
        if(gradient) {
          this.chartData.datasets[i].backgroundColor = (ctx) => {
            const canvas = ctx.chart.ctx;
            const gradient = canvas.createLinearGradient(0, 0, 0, window.innerWidth/2);

            gradient.addColorStop(0, this.chartData.datasets[i].borderColor.slice(0, 3) + "a" +
                this.chartData.datasets[i].borderColor.slice(3, this.chartData.datasets[i].borderColor.length - 1) + ", 1)");
            gradient.addColorStop(1, this.chartData.datasets[i].borderColor.slice(0, 3) + "a" +
                this.chartData.datasets[i].borderColor.slice(3, this.chartData.datasets[i].borderColor.length - 1) + ", 0.0)");
            return gradient;
          }
        } else {
          this.chartData.datasets[i].backgroundColor = this.chartData.datasets[i].borderColor.slice(0, 3) + "a" +
              this.chartData.datasets[i].borderColor.slice(3, this.chartData.datasets[i].borderColor.length - 1) + ", 0.6)";
        }
      }
      this.$refs.primeChart.refresh();
    },
    updateMinDate() {
      this.chartOptions.scales.x.min = this.minDate.toISOString().substring(0, 10);
      this.$refs.primeChart.refresh();
    },
    updateMaxDate() {
      this.chartOptions.scales.x.max = this.maxDate.toISOString().substring(0, 10);
      this.$refs.primeChart.refresh();
    },
    handleResize(){
      this.$refs.primeChart.refresh();
    }
  },
  mounted() {
    window.addEventListener("resize", this.handleResize);
  },
  unmounted() {
    window.removeEventListener("resize", this.handleResize);
  },
  computed: {
    dataSelected: function() {
      if(this.selected.length > 0) {
        return "success";
      }
      return "secondary";
    },
    tooltipText: function(){
      if(this.selected.length > 0){
        return "";
      }
      return 'To view a chart, select data from the table below.';
    },
    chartWidth: function(){
      return 2000;
    }
  }
}

</script>
<style scoped>
</style>