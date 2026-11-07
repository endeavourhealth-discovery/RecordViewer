<template>
  <div class="header" style="background-color: var(--surface-0); padding-right: 8px; overflow: visible;">
    <Button text aria-label="toggle sidebar" class="toggle-sidebar" @click="$emit('toggleSidebar')">
      ☰
    </Button>
    <div class="header-right">
      <slot/>
      <div class="profile-container">
        <Button text aria-label="profile" @click="toggleProfile($event)">
          <i style="padding:8px 0px 6px 0px;font-size: 18px" class="fa-solid fa-user"></i>
        </Button>
        <div v-if="showProfilePanel" class="custom-profile-panel">
          <div style="margin-bottom: 12px;">
            <label style="display:block; font-size: 12px; color: #666; margin-bottom: 4px;">Theme</label>
            <SelectButton v-model="viewMode" :options="viewModeOptions"></SelectButton>
          </div>
          <div>
            <label style="display:block; font-size: 12px; color: #666; margin-bottom: 4px;">Clinician View</label>
            <InputSwitch v-model="clinicianViewMode" @update:model-value="$emit('clinicianViewModeChanged', $event ? 'Emergency' : 'gp')" style="margin-top: 4px;"/>
          </div>
        </div>
      </div>
      <Button text v-for="item in headerItems" :key="item.label" @click="item.command">
        <i style="padding:8px 0px 6px 0px; font-size: 18px" :class="item.icon"></i>
      </Button>
    </div>
  </div>
</template>

<script>
export default {
  name: "NavBar",
  props: {
    headerItems: {
      type: Array,
      default: () => [
        { name: "Help", icon: "fa-regular fa-circle-question" },
        { name: "Logout", icon: "fa-solid fa-right-from-bracket" }
      ]
    },
    helpUrl: {
      type: String,
      default: "https://example.com/help"
    }
  },
  data() {
    return {
      viewMode: "Light",
      viewModeOptions: ["Light", "Dark"],
      clinicianViewMode: false,
      showProfilePanel: false,
    };
  },
  methods: {
    toggleProfile(event) {
      event.stopPropagation();
      this.showProfilePanel = !this.showProfilePanel;
    },
    closeProfile() {
      this.showProfilePanel = false;
    },
    handleClickOutside(event) {
      if (this.showProfilePanel) {
        const profileContainer = document.querySelector('.profile-container');
        if (profileContainer && !profileContainer.contains(event.target)) {
          this.closeProfile();
        }
      }
    }
  },
  mounted() {
    document.addEventListener('click', this.handleClickOutside);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside);
  },
  watch: {
    viewMode(val) {
      this.$emit("viewModeChanged", val);
    },
    clinicianViewMode(val) {
      this.$emit("clinicianViewModeChanged", val ? "Emergency" : "gp");
    }
  }
};
</script>

<style scoped>
.header {
  width:101%;
  min-width:100%;
  margin: -64px 0 0 -8px;

  overflow: hidden;
  color: var(--text-color);
  outline: var(--surface-border) double 1px;
}

.logo{
  display:inline;
  height: 58px;
  padding-top: 6px;
  padding-left: 8px;
  float: left;
}

/* Float the link section to the right */
.header-right {
  float: right;
}
.header-logo{
  display:inline;
  height: 58px;
  padding-top: 6px;
  padding-left: 8px;
  float: left;
}

.profile-container {
  position: relative;
  display: inline-block;
}

.header-right Button {
  margin-left: 4px;
}

.custom-profile-panel {
  position: absolute;
  top: 45px;
  right: 0;
  background: var(--surface-0);
  border: 1px solid var(--surface-300);
  border-radius: 6px;
  padding: 12px;
  z-index: 9999 !important;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.p-overlaypanel {
  min-width: 180px;
  z-index: 9999 !important;
}

.toggle-sidebar {
  float:left;
  display: none;
}

@media (max-width: 768px) {
  .toggle-sidebar {
    font-size:1.4rem;
    display: block;
  }
}
</style>
